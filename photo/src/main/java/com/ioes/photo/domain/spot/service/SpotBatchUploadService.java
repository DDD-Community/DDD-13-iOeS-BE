package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.spot.dto.SpotBatchUploadResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.global.common.util.FileUtils;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.AccessType;
import com.ioes.photo.global.storage.HeicImageResizer;
import com.ioes.photo.global.storage.ImageResizer;
import com.ioes.photo.global.storage.S3StorageService;
import com.ioes.photo.global.storage.StoragePathUtils;
import com.ioes.photo.global.storage.StorageUploadRollbackEvent;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 스팟 배치 업로드 서비스.
 *
 * Excel(스팟 정보)과 ZIP(이미지)을 함께 받아 스팟과 대표 이미지를 일괄 등록한다.
 * 트랜잭션 실패 시 이미 업로드된 S3 파일은 StorageUploadRollbackEvent로 자동 보상 삭제된다.
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotBatchUploadService {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(\\d+)_");

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final S3StorageService s3StorageService;
    private final ImageResizer imageResizer;
    private final HeicImageResizer heicImageResizer;
    private final ImageProperties imageProperties;
    private final StorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final CrowdAreaMapper crowdAreaMapper;

    @Transactional
    public SpotBatchUploadResponse batchUpload(MultipartFile excelFile, MultipartFile imagesFile) {
        List<SpotExcelRow> rows = parseExcel(excelFile);
        Map<String, ImageEntry> imageMap = extractImages(imagesFile);

        List<SpotBatchUploadResponse.SpotResult> results = rows.stream()
            .map(row -> processSpot(row, imageMap))
            .toList();

        log.info("스팟 배치 업로드 완료: total={}", results.size());
        return SpotBatchUploadResponse.of(results);
    }

    private String resolveCrowdAreaName(SpotExcelRow row) {
        if (NullUtils.isNotBlank(row.crowdAreaName())) {
            return row.crowdAreaName();
        }
        return crowdAreaMapper.findNearestAreaName(row.latitude(), row.longitude()).orElse(null);
    }

    private SpotBatchUploadResponse.SpotResult processSpot(SpotExcelRow row, Map<String, ImageEntry> imageMap) {
        GridPoint grid = LccGridConverter.toGrid(row.latitude(), row.longitude());
        Spot spot = spotRepository.save(Spot.builder()
            .name(row.name())
            .comment(row.comment())
            .theme(row.theme())
            .latitude(row.latitude())
            .longitude(row.longitude())
            .address(row.address())
            .status(SpotStatus.PUBLISHED)
            .gridNx(grid.nx())
            .gridNy(grid.ny())
            .crowdAreaName(resolveCrowdAreaName(row))
            .build());

        ImageEntry entry = imageMap.get(row.idPrefix());
        if (entry == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE,
                "ZIP에서 이미지를 찾을 수 없습니다: " + row.name() + " (prefix=" + row.idPrefix() + ")");
        }

        String ext = FileUtils.getExtension(entry.filename());
        String imageKey = StoragePathUtils.generateWithExt(
            storageProperties.env(), AccessType.PUBLIC, "spots", spot.getId(), "original", ext);
        s3StorageService.uploadBytes(entry.data(), imageKey, entry.contentType());
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(imageKey));

        String thumbnailKey = uploadThumbnail(entry, spot.getId());

        SpotImage spotImage = SpotImage.create(spot.getId(), imageKey, entry.filename(), entry.contentType());
        spotImage.updateThumbnailKey(thumbnailKey);
        spotImage.updateRecordedDate(row.recordedDate());
        spotImageRepository.save(spotImage);

        log.info("스팟 등록 완료: id={}, name={}", spot.getId(), row.name());
        return SpotBatchUploadResponse.SpotResult.of(
            spot.getId(), row.name(),
            s3StorageService.getUrl(imageKey),
            s3StorageService.getUrl(thumbnailKey)
        );
    }

    private String uploadThumbnail(ImageEntry entry, Long spotId) {
        int width = imageProperties.thumbnail().width();
        int height = imageProperties.thumbnail().height();
        byte[] thumbnail = heicImageResizer.supports(entry.contentType())
            ? heicImageResizer.resize(entry.data(), width, height)
            : imageResizer.resize(entry.data(), width, height);

        String thumbnailKey = StoragePathUtils.generateWithExt(
            storageProperties.env(), AccessType.PUBLIC, "spots", spotId, "thumbnail", "jpg");
        s3StorageService.uploadBytes(thumbnail, thumbnailKey, imageResizer.outputContentType());
        eventPublisher.publishEvent(new StorageUploadRollbackEvent(thumbnailKey));
        return thumbnailKey;
    }

    private List<SpotExcelRow> parseExcel(MultipartFile excelFile) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(excelFile.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            List<SpotExcelRow> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String id = getStringValue(row.getCell(0));
                if (id == null || id.isBlank()) continue;

                String idPrefix = id.replaceFirst("^spot_", "");

                rows.add(new SpotExcelRow(
                    idPrefix,
                    getStringValue(row.getCell(1)),
                    resolveTheme(getStringValue(row.getCell(2))),
                    getStringValue(row.getCell(3)),
                    getStringValue(row.getCell(4)),
                    row.getCell(5).getNumericCellValue(),
                    row.getCell(6).getNumericCellValue(),
                    getDateValue(row.getCell(7)),
                    getStringValue(row.getCell(8))
                ));
            }
            return rows;
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "Excel 파일 읽기 실패: " + e.getMessage());
        }
    }

    private Map<String, ImageEntry> extractImages(MultipartFile imagesFile) {
        Map<String, ImageEntry> imageMap = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(imagesFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                String fullName = entry.getName();
                int slashIdx = fullName.lastIndexOf('/');
                String filename = slashIdx >= 0 ? fullName.substring(slashIdx + 1) : fullName;

                Matcher m = PREFIX_PATTERN.matcher(filename);
                if (!m.find()) {
                    zis.closeEntry();
                    continue;
                }
                String prefix = m.group(1);
                byte[] data = zis.readAllBytes();
                imageMap.put(prefix, new ImageEntry(data, filename, detectContentType(filename)));
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "ZIP 파일 읽기 실패: " + e.getMessage());
        }
        return imageMap;
    }

    private SpotTheme resolveTheme(String themeKr) {
        if (themeKr == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "테마 값이 없습니다.");
        }
        return switch (themeKr.trim()) {
            case "노을" -> SpotTheme.SUNSET;
            case "윤슬" -> SpotTheme.YUNSEUL;
            default -> throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "알 수 없는 테마: " + themeKr);
        };
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() != CellType.NUMERIC) return null;
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return LocalDate.of((int) cell.getNumericCellValue(), 1, 1);
    }

    private String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".heic") || lower.endsWith(".heif")) return "image/heic";
        return "application/octet-stream";
    }

    private record SpotExcelRow(
        String idPrefix,
        String name,
        SpotTheme theme,
        String comment,
        String address,
        double latitude,
        double longitude,
        LocalDate recordedDate,
        String crowdAreaName
    ) {}

    private record ImageEntry(
        byte[] data,
        String filename,
        String contentType
    ) {}
}

package com.ioes.photo.domain.spot.service;

import com.ioes.photo.domain.crowdarea.service.CrowdAreaMapper;
import com.ioes.photo.domain.spot.dto.SpotBatchUploadResponse;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spot.repository.SpotImageRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import com.ioes.photo.global.config.image.ImageProperties;
import com.ioes.photo.global.config.image.ImageProperties.ThumbnailProperties;
import com.ioes.photo.global.config.s3.properties.StorageProperties;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.HeicImageResizer;
import com.ioes.photo.global.storage.ImageResizer;
import com.ioes.photo.global.storage.S3StorageService;
import com.ioes.photo.global.storage.UploadResult;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * {@link SpotBatchUploadService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpotBatchUploadService 단위 테스트")
class SpotBatchUploadServiceTest {

    @Mock SpotRepository spotRepository;
    @Mock SpotImageRepository spotImageRepository;
    @Mock S3StorageService s3StorageService;
    @Mock ImageResizer imageResizer;
    @Mock HeicImageResizer heicImageResizer;
    @Mock ImageProperties imageProperties;
    @Mock StorageProperties storageProperties;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CrowdAreaMapper crowdAreaMapper;

    @InjectMocks SpotBatchUploadService service;

    @BeforeEach
    void setUpCommonMocks() {
        given(imageProperties.thumbnail()).willReturn(new ThumbnailProperties(300, 300));
        given(storageProperties.env()).willReturn("dev");
        given(imageResizer.outputContentType()).willReturn("image/jpeg");
        given(imageResizer.resize(any(), any(int.class), any(int.class))).willReturn(new byte[]{1, 2, 3});
        given(heicImageResizer.supports(anyString())).willReturn(false);
        given(s3StorageService.uploadBytes(any(), anyString(), anyString())).willReturn(
            new UploadResult("key", null, 0, "image/jpeg"));
        given(s3StorageService.getUrl(anyString())).willAnswer(inv -> "http://minio/" + inv.getArgument(0));
    }

    // ── batchUpload ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("batchUpload()")
    class BatchUpload {

        @Test
        @DisplayName("Excel과 ZIP을 파싱해 스팟과 이미지를 일괄 등록한다")
        void shouldSaveAllSpotsWithImages() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "서울상암하늘공원", "노을", "설명A", "주소A", 37.567, 126.886, null),
                new SpotRow("spot_002", "서울숲(1)", "윤슬", "설명B", "주소B", 37.545, 127.040, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_서울상암하늘공원.jpg", "002_서울숲.jpg"));

            Spot spot1 = savedSpot(1L, "서울상암하늘공원", SpotTheme.SUNSET);
            Spot spot2 = savedSpot(2L, "서울숲(1)", SpotTheme.YUNSEUL);
            given(spotRepository.save(any())).willReturn(spot1, spot2);

            SpotBatchUploadResponse response = service.batchUpload(excelFile, zipFile);

            assertThat(response.total()).isEqualTo(2);
            assertThat(response.success()).isEqualTo(2);
            assertThat(response.failed()).isEqualTo(0);
            assertThat(response.results()).hasSize(2);
            assertThat(response.results()).extracting("spotId").containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("등록된 스팟은 PUBLISHED 상태여야 한다")
        void shouldSaveSpotAsPublished() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "양재천", "윤슬", null, null, 37.469, 127.030, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_양재천.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "양재천", SpotTheme.YUNSEUL));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SpotStatus.PUBLISHED);
        }

        @Test
        @DisplayName("recorded_date가 SpotImage에 저장된다")
        void shouldSaveRecordedDate() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "한강", "노을", null, null, 37.5, 126.9, LocalDate.of(2023, 10, 1))
            ));
            MultipartFile zipFile = buildZip(List.of("001_한강.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "한강", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<SpotImage> captor = ArgumentCaptor.forClass(SpotImage.class);
            then(spotImageRepository).should().save(captor.capture());
            assertThat(captor.getValue().getRecordedDate()).isEqualTo(LocalDate.of(2023, 10, 1));
        }

        @Test
        @DisplayName("ZIP에서 이미지를 찾지 못하면 BusinessException을 던진다")
        void shouldThrowWhenImageNotFound() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "양재천", "윤슬", null, null, 37.469, 127.030, null)
            ));
            // ZIP에 001_ 파일이 없음
            MultipartFile zipFile = buildZip(List.of("999_없는파일.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "양재천", SpotTheme.YUNSEUL));

            assertThatThrownBy(() -> service.batchUpload(excelFile, zipFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("001");
        }

        @Test
        @DisplayName("S3에 이미지와 썸네일 각각 1회씩 업로드한다")
        void shouldUploadImageAndThumbnailToS3() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "서울숲", "노을", null, null, 37.545, 127.040, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_서울숲.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "서울숲", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            then(s3StorageService).should(times(2)).uploadBytes(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("ZIP 폴더 내 이미지도 숫자 prefix로 매칭된다")
        void shouldMatchImagesInsideZipFolder() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_007", "양재천", "윤슬", null, null, 37.469, 127.030, null)
            ));
            MultipartFile zipFile = buildZipWithFolder("이미지/007_양재천.jpg");
            given(spotRepository.save(any())).willReturn(savedSpot(7L, "양재천", SpotTheme.YUNSEUL));

            SpotBatchUploadResponse response = service.batchUpload(excelFile, zipFile);

            assertThat(response.results().get(0).spotId()).isEqualTo(7L);
        }
    }

    // ── theme mapping ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("테마 매핑")
    class ThemeMapping {

        @Test
        @DisplayName("노을은 SUNSET으로 매핑된다")
        void shouldMapSunset() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "스팟", "노을", null, null, 37.5, 127.0, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_스팟.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "스팟", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTheme()).isEqualTo(SpotTheme.SUNSET);
        }

        @Test
        @DisplayName("윤슬은 YUNSEUL로 매핑된다")
        void shouldMapYunseul() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "스팟", "윤슬", null, null, 37.5, 127.0, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_스팟.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "스팟", SpotTheme.YUNSEUL));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTheme()).isEqualTo(SpotTheme.YUNSEUL);
        }

        @Test
        @DisplayName("알 수 없는 테마는 BusinessException을 던진다")
        void shouldThrowOnUnknownTheme() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "스팟", "황혼", null, null, 37.5, 127.0, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_스팟.jpg"));

            assertThatThrownBy(() -> service.batchUpload(excelFile, zipFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("황혼");
        }
    }

    // ── 외부 API 연동 필드 ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("외부 API 연동 필드")
    class ExternalApiFields {

        @Test
        @DisplayName("위경도로부터 기상청 격자(grid_nx/ny)를 계산해 저장한다")
        void shouldComputeWeatherGrid() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "서울시청", "노을", null, null, 37.5665, 126.9780, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_서울시청.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "서울시청", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            GridPoint expected = LccGridConverter.toGrid(37.5665, 126.9780);
            assertThat(captor.getValue().getGridNx()).isEqualTo(expected.nx());
            assertThat(captor.getValue().getGridNy()).isEqualTo(expected.ny());
        }

        @Test
        @DisplayName("혼잡도 지역명(9번째 컬럼)을 읽어 저장한다")
        void shouldSaveCrowdAreaName() throws IOException {
            MultipartFile excelFile = buildExcelWithCrowdArea(
                new SpotRow("spot_001", "여의도한강공원", "노을", null, null, 37.528, 126.933, null),
                "여의도");
            MultipartFile zipFile = buildZip(List.of("001_여의도한강공원.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "여의도한강공원", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getCrowdAreaName()).isEqualTo("여의도");
        }

        @Test
        @DisplayName("혼잡도 지역명 컬럼이 없으면 crowdAreaName은 null이다")
        void shouldLeaveCrowdAreaNameNullWhenAbsent() throws IOException {
            MultipartFile excelFile = buildExcel(List.of(
                new SpotRow("spot_001", "한강", "노을", null, null, 37.5, 126.9, null)
            ));
            MultipartFile zipFile = buildZip(List.of("001_한강.jpg"));
            given(spotRepository.save(any())).willReturn(savedSpot(1L, "한강", SpotTheme.SUNSET));

            service.batchUpload(excelFile, zipFile);

            ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
            then(spotRepository).should().save(captor.capture());
            assertThat(captor.getValue().getCrowdAreaName()).isNull();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private record SpotRow(
        String id, String name, String theme, String comment, String address,
        double lat, double lng, LocalDate recordedDate
    ) {}

    private MultipartFile buildExcel(List<SpotRow> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Sheet1");

            // header
            var headerRow = sheet.createRow(0);
            String[] headers = {"id", "name", "theme", "comment", "address", "위도(Lat)", "경도(Lng)", "recorded_time"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CreationHelper creationHelper = wb.getCreationHelper();
            CellStyle dateCellStyle = wb.createCellStyle();
            dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            for (int i = 0; i < rows.size(); i++) {
                SpotRow row = rows.get(i);
                var excelRow = sheet.createRow(i + 1);
                excelRow.createCell(0).setCellValue(row.id());
                excelRow.createCell(1).setCellValue(row.name());
                excelRow.createCell(2).setCellValue(row.theme());
                if (row.comment() != null) excelRow.createCell(3).setCellValue(row.comment());
                if (row.address() != null) excelRow.createCell(4).setCellValue(row.address());
                excelRow.createCell(5).setCellValue(row.lat());
                excelRow.createCell(6).setCellValue(row.lng());
                if (row.recordedDate() != null) {
                    var dateCell = excelRow.createCell(7);
                    dateCell.setCellStyle(dateCellStyle);
                    dateCell.setCellValue(row.recordedDate());
                }
            }

            wb.write(out);
            return new MockMultipartFile("excel", "spots.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private MultipartFile buildExcelWithCrowdArea(SpotRow row, String crowdAreaName) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            sheet.createRow(0); // header

            var excelRow = sheet.createRow(1);
            excelRow.createCell(0).setCellValue(row.id());
            excelRow.createCell(1).setCellValue(row.name());
            excelRow.createCell(2).setCellValue(row.theme());
            if (row.comment() != null) excelRow.createCell(3).setCellValue(row.comment());
            if (row.address() != null) excelRow.createCell(4).setCellValue(row.address());
            excelRow.createCell(5).setCellValue(row.lat());
            excelRow.createCell(6).setCellValue(row.lng());
            if (crowdAreaName != null) excelRow.createCell(8).setCellValue(crowdAreaName);

            wb.write(out);
            return new MockMultipartFile("excel", "spots.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private MultipartFile buildZip(List<String> filenames) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (String name : filenames) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}); // minimal JPEG header
                zos.closeEntry();
            }
        }
        return new MockMultipartFile("images", "images.zip", "application/zip", out.toByteArray());
    }

    private MultipartFile buildZipWithFolder(String fullPath) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            // folder entry
            zos.putNextEntry(new ZipEntry(fullPath.substring(0, fullPath.lastIndexOf('/') + 1)));
            zos.closeEntry();
            // file entry
            zos.putNextEntry(new ZipEntry(fullPath));
            zos.write(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            zos.closeEntry();
        }
        return new MockMultipartFile("images", "images.zip", "application/zip", out.toByteArray());
    }

    private Spot savedSpot(Long id, String name, SpotTheme theme) {
        Spot spot = Spot.builder()
            .name(name)
            .theme(theme)
            .latitude(37.5)
            .longitude(127.0)
            .status(SpotStatus.PUBLISHED)
            .build();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }
}

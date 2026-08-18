package com.ioes.photo.domain.spot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ioes.photo.domain.spot.entity.Spot;
import com.ioes.photo.domain.spot.entity.SpotImage;
import com.ioes.photo.domain.spot.entity.SpotReview;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.spotinfo.entity.SpotInfo;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import com.ioes.photo.global.common.util.NullUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 스팟 상세 조회 응답.
 *
 * @author 김성민
 */
@Schema(description = "스팟 상세 조회 응답")
public record SpotDetailResponse(
    @Schema(description = "스팟 ID") Long spotId,
    @Schema(description = "스팟 이름") String name,
    @Schema(description = "한 줄 코멘트") String comment,
    @Schema(description = "테마 (SUNSET=노을, YUNSEUL=윤슬, SUNLIGHT=햇살, NIGHT_VIEW=야경)") SpotTheme theme,
    @Schema(description = "위도") Double latitude,
    @Schema(description = "경도") Double longitude,
    @Schema(description = "간략 주소 (시·구 단위)") String address,
    @Schema(description = "도로명 주소") String addressRoad,
    @Schema(description = "지번 주소") String addressJibun,
    @Schema(description = "대표 이미지 URL") String imageUrl,
    @Schema(description = "촬영 일자", example = "2024-05-01") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate recordedDate,
    @Schema(description = "촬영 시각", example = "18:30") @JsonFormat(pattern = "HH:mm") LocalTime recordedTime,
    @Schema(description = "하늘 상태") SkyStatus weatherSky,
    @Schema(description = "강수 형태") PrecipitationType precipitation,
    @Schema(description = "강수 확률(%)") Integer precipitationProbability,
    @Schema(description = "혼잡도 수준") CongestionLevel congestionLevel,
    @Schema(description = "일몰 시각", example = "19:12") @JsonFormat(pattern = "HH:mm") LocalTime sunsetTime,
    @Schema(description = "천문 정보 기준일", example = "2024-05-01") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate astronomyDate,
    @Schema(description = "날씨 갱신 시각") LocalDateTime weatherUpdatedAt,
    @Schema(description = "혼잡도 갱신 시각") LocalDateTime congestionUpdatedAt,
    @Schema(description = "주차 정보") String parkingInfo,
    @Schema(description = "북마크 수") long bookmarkCount,
    @Schema(description = "북마크 여부 (비로그인 시 false)") boolean isBookmarked,
    @Schema(description = "내 스팟 여부 (비로그인 시 false)") boolean isMySpot,
    @Schema(description = "스팟 공개 상태 (DRAFT/PENDING/RE_REVIEW_PENDING/PUBLISHED/REJECTED)") String status,
    @Schema(description = "관리자 큐레이션 스팟 여부 (사용자 등록 스팟이면 false)") boolean isCurated,
    @Schema(description = "좋아요(추천) 수") long likeCount,
    @Schema(description = "좋아요 여부 (비로그인 시 false)") boolean isLiked,
    @Schema(description = "좋아요 가능 여부 (비공개 상태의 유저 스팟이면 false)") boolean isLikeable,
    @Schema(description = "반려 정보. 반려된 내 스팟일 때만 채워지며 그 외에는 null") RejectionInfo rejection
) {

    private static final String PARKING_INFO_DEFAULT = "-";

    @Schema(description = "스팟 반려 정보")
    public record RejectionInfo(
        @Schema(description = "반려 사유 코드", example = "LOW_QUALITY") String reason,
        @Schema(description = "반려 사유명", example = "사진 상태 불량") String reasonLabel,
        @Schema(description = "사용자 안내 문구") String guideMessage,
        @Schema(description = "운영자가 입력한 상세 사유 (기타 사유일 때 사용)") String detail,
        @Schema(description = "반려 처리 시각") LocalDateTime rejectedAt
    ) {

        public static RejectionInfo from(SpotReview review) {
            RejectionReason reason = review.getReason();
            if (reason == null) {
                return null;
            }
            return new RejectionInfo(
                reason.name(),
                reason.getLabel(),
                NullUtils.orDefault(reason.getGuideMessage(), review.getDetail()),
                review.getDetail(),
                review.getCreatedAt()
            );
        }
    }

    /**
     * 응답에 실을 사용자별 판단 결과 묶음.
     * 파라미터가 길어져 호출부에서 순서를 헷갈리기 쉬워 별도 타입으로 묶는다.
     */
    public record SpotDetailFlags(
        boolean bookmarked,
        boolean mySpot,
        boolean liked,
        RejectionInfo rejection
    ) {}

    public static SpotDetailResponse of(Spot spot, SpotImage spotImage, SpotInfo spotInfo,
                                        String imageUrl, SpotDetailFlags flags) {
        return new SpotDetailResponse(
            spot.getId(),
            spot.getName(),
            spot.getComment(),
            spot.getTheme(),
            spot.getLatitude(),
            spot.getLongitude(),
            spot.getAddress(),
            spot.getAddressRoad(),
            spot.getAddressJibun(),
            imageUrl,
            Optional.ofNullable(spotImage).map(SpotImage::getRecordedDate).orElse(null),
            Optional.ofNullable(spotImage).map(SpotImage::getRecordedTime).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherSky).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherPrecipitation).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getPrecipitationProbability).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getCongestionLevel).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getSunsetTime).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getAstronomyDate).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getWeatherUpdatedAt).orElse(null),
            Optional.ofNullable(spotInfo).map(SpotInfo::getCongestionUpdatedAt).orElse(null),
            NullUtils.orDefault(spot.getParkingInfo(), PARKING_INFO_DEFAULT),
            spot.getBookmarkCount(),
            flags.bookmarked(),
            flags.mySpot(),
            spot.getStatus().name(),
            spot.isCurated(),
            spot.getLikeCount(),
            flags.liked(),
            spot.isLikeable(),
            flags.rejection()
        );
    }
}

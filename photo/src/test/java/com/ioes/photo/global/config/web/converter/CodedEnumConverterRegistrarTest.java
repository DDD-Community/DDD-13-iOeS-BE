package com.ioes.photo.global.config.web.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.spot.enums.ImageSourceType;
import com.ioes.photo.domain.spot.enums.RejectionReason;
import com.ioes.photo.domain.spot.enums.ReviewDecision;
import com.ioes.photo.domain.spot.enums.SortType;
import com.ioes.photo.domain.spot.enums.SpotOpenRequestStatus;
import com.ioes.photo.domain.spot.enums.SpotReportStatus;
import com.ioes.photo.domain.spot.enums.SpotStatus;
import com.ioes.photo.domain.spot.enums.SpotTheme;
import com.ioes.photo.domain.user.enums.UserRole;
import com.ioes.photo.domain.user.enums.WithdrawalReasonType;
import com.ioes.photo.external.crowd.enums.CongestionLevel;
import com.ioes.photo.external.weather.enums.PrecipitationType;
import com.ioes.photo.external.weather.enums.SkyStatus;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

/**
 * {@link CodedEnumConverterRegistrar} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("CodedEnumConverterRegistrar 테스트")
class CodedEnumConverterRegistrarTest {

    private static final String BASE_PACKAGE = "com.ioes.photo";

    static List<Class<?>> knownCodedEnums() {
        return List.of(
            SpotTheme.class, SpotStatus.class, SpotOpenRequestStatus.class, ReviewDecision.class,
            RejectionReason.class, ImageSourceType.class, SortType.class, SpotReportStatus.class,
            UserRole.class, WithdrawalReasonType.class, OAuthProvider.class,
            SkyStatus.class, PrecipitationType.class, CongestionLevel.class
        );
    }

    @Nested
    @DisplayName("스캔 및 등록")
    class RegisterAll {

        @ParameterizedTest(name = "{0} 이 스캔되어 등록된다")
        @MethodSource("com.ioes.photo.global.config.web.converter.CodedEnumConverterRegistrarTest#knownCodedEnums")
        @DisplayName("프로젝트 내 CodedEnum 구현 enum이 전부 스캔 대상에 포함된다")
        void registersEveryKnownCodedEnum(Class<?> enumClass) {
            FormattingConversionService conversionService = new FormattingConversionService();
            new CodedEnumConverterRegistrar().registerAll(conversionService, BASE_PACKAGE);

            assertThat(conversionService.canConvert(String.class, enumClass)).isTrue();
        }
    }

    @Nested
    @DisplayName("우선순위")
    class Priority {

        @Test
        @DisplayName("Spring 기본 StringToEnumConverterFactory가 먼저 등록되어 있어도 code 매칭이 성공한다")
        void codeMatchingWinsOverDefaultConverter() {
            // DefaultFormattingConversionService는 생성 시점에 기본 컨버터(StringToEnumConverterFactory 포함)를 등록한다.
            DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

            new CodedEnumConverterRegistrar().registerAll(conversionService, BASE_PACKAGE);

            // "SS"는 SpotTheme의 enum 이름이 아니라 code이므로, 기본 컨버터만으로는 절대 변환될 수 없다.
            SpotTheme result = conversionService.convert("SS", SpotTheme.class);

            assertThat(result).isEqualTo(SpotTheme.SUNSET);
        }
    }
}

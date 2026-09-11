package com.ioes.photo.global.config.web.converter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import fixture.codedenum.AmbiguousEnum;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CodedEnum 모호성(name/code 충돌) 정책에 대한 fail-fast 검증 테스트.
 *
 * <p>{@link StringToCodedEnumConverter}는 code를 우선 매칭하므로, 한 상수의 name이 다른 상수의
 * code와 겹치면 그 name으로는 영원히 도달할 수 없다. 이 모호성은 컨버터 생성 시점에 즉시 예외로 드러나야 한다.</p>
 *
 * @author 황제연
 */
@DisplayName("CodedEnum 모호성 정책 테스트")
class CodedEnumAmbiguityTest {

    static List<Class<?>> knownCodedEnums() {
        return List.of(
            SpotTheme.class, SpotStatus.class, SpotOpenRequestStatus.class, ReviewDecision.class,
            RejectionReason.class, ImageSourceType.class, SortType.class, SpotReportStatus.class,
            UserRole.class, WithdrawalReasonType.class, OAuthProvider.class,
            SkyStatus.class, PrecipitationType.class, CongestionLevel.class
        );
    }

    @ParameterizedTest(name = "{0} 은 모호성이 없어 정상 생성된다")
    @MethodSource("knownCodedEnums")
    @DisplayName("프로젝트 내 실제 CodedEnum 구현체는 모호성이 없다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void realEnumsHaveNoAmbiguity(Class<?> enumClass) {
        assertThatCode(() -> new StringToCodedEnumConverter(enumClass)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한 상수의 name이 다른 상수의 code와 겹치면 IllegalStateException")
    void throwsWhenNameCollidesWithAnotherConstantsCode() {
        assertThatThrownBy(() -> new StringToCodedEnumConverter<>(AmbiguousEnum.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BAR");
    }
}

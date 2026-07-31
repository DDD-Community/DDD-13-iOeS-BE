package com.ioes.photo.domain.user.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link UserRoleConverter} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("UserRoleConverter 단위 테스트")
class UserRoleConverterTest {

    private final UserRoleConverter converter = new UserRoleConverter();

    @ParameterizedTest
    @EnumSource(UserRole.class)
    @DisplayName("enum과 code를 왕복 변환한다")
    void shouldRoundTrip(UserRole role) {
        String code = converter.convertToDatabaseColumn(role);

        assertThat(code).isEqualTo(role.getCode());
        assertThat(converter.convertToEntityAttribute(code)).isEqualTo(role);
    }

    @Test
    @DisplayName("USER_ADMIN은 'A', USER_CUSTOMER는 'C'로 저장된다")
    void shouldMapToExpectedCodes() {
        assertThat(converter.convertToDatabaseColumn(UserRole.USER_ADMIN)).isEqualTo("A");
        assertThat(converter.convertToDatabaseColumn(UserRole.USER_CUSTOMER)).isEqualTo("C");
    }

    @Test
    @DisplayName("null은 null로 변환한다")
    void shouldHandleNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("알 수 없는 코드는 예외를 던진다")
    void shouldThrowOnUnknownCode() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("X"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.ioes.photo.global.common.util;

import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link NullUtils} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("NullUtils 테스트")
class NullUtilsTest {

    @Nested
    @DisplayName("requireNonNull")
    class RequireNonNull {

        @Test
        @DisplayName("null이 아닌 값은 그대로 반환")
        void returnsValue_whenNotNull() {
            assertThat(NullUtils.requireNonNull("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("null이면 NullPointerException 발생 (단일 인자)")
        void throwsNullPointerException_whenNull() {
            assertThatThrownBy(() -> NullUtils.requireNonNull(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null이면 Supplier가 제공하는 예외 발생")
        void throwsSupplierException_whenNull() {
            assertThatThrownBy(() -> NullUtils.requireNonNull(null,
                    () -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("null이면 Supplier에서 커스텀 메시지 포함 예외 발생")
        void throwsSupplierException_withCustomMessage() {
            assertThatThrownBy(() -> NullUtils.requireNonNull(null,
                    () -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "유저 없음")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유저 없음");
        }
    }

    @Nested
    @DisplayName("orElseThrow (Optional)")
    class OrElseThrow {

        @Test
        @DisplayName("Optional에 값이 있으면 반환")
        void returnsValue_whenPresent() {
            assertThat(NullUtils.orElseThrow(Optional.of("data"))).isEqualTo("data");
        }

        @Test
        @DisplayName("empty Optional이면 NoSuchElementException 발생 (단일 인자)")
        void throwsNoSuchElement_whenEmpty() {
            assertThatThrownBy(() -> NullUtils.orElseThrow(Optional.empty()))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("empty Optional이면 Supplier가 제공하는 예외 발생")
        void throwsSupplierException_whenEmpty() {
            assertThatThrownBy(() ->
                    NullUtils.orElseThrow(Optional.empty(),
                            () -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("Supplier로 임의 RuntimeException 사용 가능")
        void throwsArbitraryRuntimeException() {
            assertThatThrownBy(() ->
                    NullUtils.orElseThrow(Optional.empty(), () -> new IllegalStateException("커스텀 예외")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("커스텀 예외");
        }
    }

    @Nested
    @DisplayName("orDefault")
    class OrDefault {

        @Test
        @DisplayName("null이 아니면 원래 값 반환")
        void returnsOriginal_whenNotNull() {
            assertThat(NullUtils.orDefault("value", "default")).isEqualTo("value");
        }

        @Test
        @DisplayName("null이면 기본값 반환")
        void returnsDefault_whenNull() {
            assertThat(NullUtils.orDefault(null, "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("null이면 Supplier 기본값 반환")
        void returnsSupplierDefault_whenNull() {
            assertThat(NullUtils.orDefault(null, () -> "supplier-default")).isEqualTo("supplier-default");
        }

        @Test
        @DisplayName("null이 아니면 Supplier 호출 안 함")
        void doesNotCallSupplier_whenNotNull() {
            assertThat(NullUtils.orDefault("exist", () -> "supplier")).isEqualTo("exist");
        }
    }

    @Nested
    @DisplayName("String 검사")
    class StringChecks {

        @Test
        @DisplayName("null이면 isBlank true")
        void isBlank_null() {
            assertThat(NullUtils.isBlank(null)).isTrue();
        }

        @Test
        @DisplayName("빈 문자열이면 isBlank true")
        void isBlank_empty() {
            assertThat(NullUtils.isBlank("")).isTrue();
        }

        @Test
        @DisplayName("공백 문자열이면 isBlank true")
        void isBlank_whitespace() {
            assertThat(NullUtils.isBlank("   ")).isTrue();
        }

        @Test
        @DisplayName("내용이 있으면 isBlank false")
        void isBlank_withContent() {
            assertThat(NullUtils.isBlank("hello")).isFalse();
        }

        @Test
        @DisplayName("isNotBlank는 isBlank 반대")
        void isNotBlank() {
            assertThat(NullUtils.isNotBlank("hello")).isTrue();
            assertThat(NullUtils.isNotBlank(null)).isFalse();
            assertThat(NullUtils.isNotBlank("  ")).isFalse();
        }

        @Test
        @DisplayName("blank이면 orDefaultIfBlank가 기본값 반환")
        void orDefaultIfBlank_blank() {
            assertThat(NullUtils.orDefaultIfBlank(null, "기본")).isEqualTo("기본");
            assertThat(NullUtils.orDefaultIfBlank("  ", "기본")).isEqualTo("기본");
        }

        @Test
        @DisplayName("blank가 아니면 orDefaultIfBlank가 원래 값 반환")
        void orDefaultIfBlank_notBlank() {
            assertThat(NullUtils.orDefaultIfBlank("값", "기본")).isEqualTo("값");
        }
    }

    @Nested
    @DisplayName("Collection 검사")
    class CollectionChecks {

        @Test
        @DisplayName("null 컬렉션이면 isEmpty true")
        void isEmpty_null() {
            assertThat(NullUtils.isEmpty(null)).isTrue();
        }

        @Test
        @DisplayName("빈 컬렉션이면 isEmpty true")
        void isEmpty_empty() {
            assertThat(NullUtils.isEmpty(Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("내용 있으면 isEmpty false")
        void isEmpty_withContent() {
            assertThat(NullUtils.isEmpty(List.of("a"))).isFalse();
        }

        @Test
        @DisplayName("isNotEmpty는 isEmpty 반대")
        void isNotEmpty() {
            assertThat(NullUtils.isNotEmpty(List.of("a"))).isTrue();
            assertThat(NullUtils.isNotEmpty(Collections.emptyList())).isFalse();
            assertThat(NullUtils.isNotEmpty(null)).isFalse();
        }
    }
}
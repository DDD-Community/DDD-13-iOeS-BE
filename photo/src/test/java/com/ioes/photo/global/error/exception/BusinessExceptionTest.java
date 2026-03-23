package com.ioes.photo.global.error.exception;

import com.ioes.photo.global.error.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BusinessException} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {

    @Test
    @DisplayName("ErrorCode만으로 생성 시 기본 메시지 사용")
    void constructor_withErrorCode() {
        BusinessException ex = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("ErrorCode + 커스텀 메시지로 생성")
    void constructor_withCustomMessage() {
        String customMessage = "사용자 ID 1번을 찾을 수 없습니다.";
        BusinessException ex = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, customMessage);

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("RuntimeException을 상속함")
    void isRuntimeException() {
        BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("모든 CommonErrorCode로 생성 가능")
    void canBeCreatedWithAllCommonErrorCodes() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            BusinessException ex = new BusinessException(errorCode);
            assertThat(ex.getErrorCode()).isEqualTo(errorCode);
            assertThat(ex.getMessage()).isEqualTo(errorCode.getMessage());
        }
    }
}
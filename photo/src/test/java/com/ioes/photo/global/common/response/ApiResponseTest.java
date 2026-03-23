package com.ioes.photo.global.common.response;

import com.ioes.photo.global.error.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiResponse} 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("ApiResponse 테스트")
class ApiResponseTest {

    @Test
    @DisplayName("데이터 있는 성공 응답 생성")
    void success_withData() {
        String data = "테스트 데이터";
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("S000");
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("데이터 없는 성공 응답 생성")
    void success_withoutData() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("S000");
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("ErrorCode 기본 메시지 에러 응답 생성")
    void error_withErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("C006");
        assertThat(response.getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("커스텀 메시지 에러 응답 생성")
    void error_withCustomMessage() {
        String customMessage = "사용자를 찾을 수 없습니다.";
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.RESOURCE_NOT_FOUND, customMessage);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("C006");
        assertThat(response.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("모든 CommonErrorCode에 대해 에러 응답이 올바르게 생성됨")
    void error_allCommonErrorCodes() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
            assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
        }
    }
}
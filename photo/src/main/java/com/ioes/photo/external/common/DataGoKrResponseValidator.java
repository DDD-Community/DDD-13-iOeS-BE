package com.ioes.photo.external.common;

import com.ioes.photo.external.error.ExternalApiErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 공공데이터포털 응답 공통 검증 유틸리티.
 *
 * <p>기상청, 천문연구원 등 공공데이터포털 API 응답의 resultCode를 검사하여
 * 적절한 {@link BusinessException}으로 변환합니다.</p>
 *
 * @author 김성민
 */
@Slf4j
public final class DataGoKrResponseValidator {

    private DataGoKrResponseValidator() {
    }

    /**
     * 공공데이터포털 응답의 resultCode를 검증합니다.
     *
     * @param resultCode 응답 헤더의 결과 코드
     * @param resultMsg  응답 헤더의 결과 메시지
     * @param apiName    API 식별 이름 (로그/에러 메시지에 사용)
     * @throws BusinessException 성공이 아닌 경우
     */
    public static void validate(String resultCode, String resultMsg, String apiName) {
        DataGoKrResultCode code = DataGoKrResultCode.fromCode(resultCode);
        if (code.isSuccess()) {
            return;
        }

        log.warn("{} API 오류 응답: code={}, msg={}", apiName, resultCode, resultMsg);

        ExternalApiErrorCode errorCode = code.getMappedErrorCode();
        if (errorCode == ExternalApiErrorCode.API_RATE_LIMIT_EXCEEDED
            || errorCode == ExternalApiErrorCode.API_SERVICE_KEY_INVALID) {
            throw new BusinessException(errorCode);
        }

        throw new BusinessException(errorCode, apiName + " 오류: " + resultMsg);
    }
}

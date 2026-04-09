package com.ioes.photo.external.common;

import com.ioes.photo.external.error.ExternalApiErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 공공데이터포털 공통 응답 결과 코드.
 *
 * <p>공공데이터포털(data.go.kr)의 모든 OpenAPI는 HTTP 200으로 응답하면서
 * body의 resultCode로 성공/실패를 표현합니다. 여러 API(기상청, 천문연구원 등)에서
 * 동일한 코드 체계를 공유하므로 공통 enum으로 분리합니다.</p>
 *
 * @author 김성민
 */
@Getter
@RequiredArgsConstructor
public enum DataGoKrResultCode {

    NORMAL_SERVICE("00", null),
    APPLICATION_ERROR("01", ExternalApiErrorCode.API_CALL_FAILED),
    DB_ERROR("02", ExternalApiErrorCode.API_CALL_FAILED),
    NODATA_ERROR("03", ExternalApiErrorCode.API_CALL_FAILED),
    HTTP_ERROR("04", ExternalApiErrorCode.API_CALL_FAILED),
    SERVICE_TIMEOUT_ERROR("05", ExternalApiErrorCode.API_TIMEOUT),
    INVALID_REQUEST_PARAMETER_ERROR("10", ExternalApiErrorCode.API_CALL_FAILED),
    NO_MANDATORY_REQUEST_PARAMETERS_ERROR("11", ExternalApiErrorCode.API_CALL_FAILED),
    NO_OPENAPI_SERVICE_ERROR("12", ExternalApiErrorCode.API_CALL_FAILED),
    SERVICE_ACCESS_DENIED_ERROR("20", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    TEMPORARILY_DISABLE_THE_SERVICE_KEY_ERROR("21", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR("22", ExternalApiErrorCode.API_RATE_LIMIT_EXCEEDED),
    SERVICE_KEY_IS_NOT_REGISTERED_ERROR("30", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    DEADLINE_HAS_EXPIRED_ERROR("31", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    UNREGISTERED_IP_ERROR("32", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    UNSIGNED_CALL_ERROR("33", ExternalApiErrorCode.API_SERVICE_KEY_INVALID),
    UNKNOWN_ERROR("99", ExternalApiErrorCode.API_CALL_FAILED);

    private final String code;
    private final ExternalApiErrorCode mappedErrorCode;

    private static final Map<String, DataGoKrResultCode> CODE_MAP =
        Stream.of(values()).collect(Collectors.toMap(DataGoKrResultCode::getCode, Function.identity()));

    public static DataGoKrResultCode fromCode(String code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN_ERROR);
    }

    public boolean isSuccess() {
        return this == NORMAL_SERVICE;
    }
}

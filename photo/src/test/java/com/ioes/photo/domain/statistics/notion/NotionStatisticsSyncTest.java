package com.ioes.photo.domain.statistics.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioes.photo.domain.statistics.dto.StatisticsSnapshot;
import com.ioes.photo.global.common.util.HttpClientUtils;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link NotionStatisticsSync} 단위 테스트 — 노션 호출 없이 upsert 분기/페이로드 검증.
 *
 * @author 김성민
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotionStatisticsSync 단위 테스트")
class NotionStatisticsSyncTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClientUtils httpClientUtils = org.mockito.Mockito.mock(HttpClientUtils.class);
    private final NotionStatisticsSync sync =
        new NotionStatisticsSync(httpClientUtils, new NotionProperties("token", "db-id"));

    private final StatisticsSnapshot snapshot = new StatisticsSnapshot(
        LocalDate.of(2026, 8, 12), 5, 3, 2, 100, 40, 80, 0.5, "한강(3), 남산(1)");

    @Test
    @DisplayName("해당 일자 페이지가 없으면 페이지를 생성(POST)한다")
    void createsPageWhenAbsent() throws Exception {
        given(httpClientUtils.post(contains("/query"), any(), any(), eq(JsonNode.class)))
            .willReturn(objectMapper.readTree("{\"results\":[]}"));

        sync.upsert(snapshot);

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(httpClientUtils).post(contains("/pages"), body.capture(), any(), eq(JsonNode.class));
        verify(httpClientUtils, never()).patch(any(), any(), any(), eq(JsonNode.class));

        assertThat(properties(body.getValue())).containsKey("일자");
        assertThat(number(body.getValue(), "신규가입_카카오").longValue()).isEqualTo(3L);
        assertThat(number(body.getValue(), "저장사용유저비율").doubleValue()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("해당 일자 페이지가 있으면 갱신(PATCH)한다")
    void updatesPageWhenPresent() throws Exception {
        given(httpClientUtils.post(contains("/query"), any(), any(), eq(JsonNode.class)))
            .willReturn(objectMapper.readTree("{\"results\":[{\"id\":\"page-123\"}]}"));

        sync.upsert(snapshot);

        verify(httpClientUtils).patch(contains("/pages/page-123"), any(), any(), eq(JsonNode.class));
        verify(httpClientUtils, never()).post(contains("/pages"), any(), any(), eq(JsonNode.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(Object createBody) {
        return (Map<String, Object>) ((Map<String, Object>) createBody).get("properties");
    }

    @SuppressWarnings("unchecked")
    private Number number(Object createBody, String key) {
        Map<String, Object> prop = (Map<String, Object>) properties(createBody).get(key);
        return (Number) prop.get("number");
    }
}

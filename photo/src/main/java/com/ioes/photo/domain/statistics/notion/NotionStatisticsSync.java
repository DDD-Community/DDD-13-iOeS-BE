package com.ioes.photo.domain.statistics.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.ioes.photo.domain.statistics.dto.StatisticsSnapshot;
import com.ioes.photo.global.common.util.HttpClientUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * 운영 지표 스냅샷을 노션 DB에 upsert 한다 (일자 Title 기준).
 *
 * 노션 DB 속성(이름/타입 고정): 일자(Title), 신규가입/신규가입_카카오/신규가입_애플/누적가입/저장활성유저(Number),
 * 저장사용유저비율(Number-Percent, 0~1), 저장TOP(Text).
 *
 * @author 김성민
 */
@Component
@RequiredArgsConstructor
public class NotionStatisticsSync {

    private static final String BASE_URL = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";
    private static final String TITLE_PROPERTY = "일자";

    private final HttpClientUtils httpClientUtils;
    private final NotionProperties properties;

    public void upsert(StatisticsSnapshot snapshot) {
        String date = snapshot.date().toString();
        Map<String, Object> props = buildProperties(snapshot, date);

        String pageId = findPageIdByDate(date);
        if (pageId != null) {
            httpClientUtils.patch(BASE_URL + "/pages/" + pageId,
                Map.of("properties", props), headers(), JsonNode.class);
        } else {
            httpClientUtils.post(BASE_URL + "/pages",
                Map.of("parent", Map.of("database_id", properties.databaseId()), "properties", props),
                headers(), JsonNode.class);
        }
    }

    private String findPageIdByDate(String date) {
        JsonNode response = httpClientUtils.post(
            BASE_URL + "/databases/" + properties.databaseId() + "/query",
            Map.of("filter", Map.of("property", TITLE_PROPERTY, "title", Map.of("equals", date)),
                "page_size", 1),
            headers(), JsonNode.class);

        JsonNode results = response.path("results");
        return results.isArray() && !results.isEmpty() ? results.get(0).path("id").asText(null) : null;
    }

    private Map<String, Object> buildProperties(StatisticsSnapshot s, String date) {
        return Map.of(
            TITLE_PROPERTY, title(date),
            "신규가입", number(s.newSignups()),
            "신규가입_카카오", number(s.newSignupsKakao()),
            "신규가입_애플", number(s.newSignupsApple()),
            "누적가입", number(s.cumulativeSignups()),
            "저장활성유저", number(s.activeSavers()),
            "저장사용유저비율", number(s.saveUsageRatio()),
            "저장TOP", richText(s.topSpots())
        );
    }

    private Consumer<HttpHeaders> headers() {
        return h -> {
            h.setBearerAuth(properties.token());
            h.set("Notion-Version", NOTION_VERSION);
        };
    }

    private Map<String, Object> number(Number value) {
        return Map.of("number", value);
    }

    private Map<String, Object> title(String content) {
        return Map.of("title", List.of(Map.of("text", Map.of("content", content))));
    }

    private Map<String, Object> richText(String content) {
        return Map.of("rich_text", List.of(Map.of("text", Map.of("content", content == null ? "" : content))));
    }
}

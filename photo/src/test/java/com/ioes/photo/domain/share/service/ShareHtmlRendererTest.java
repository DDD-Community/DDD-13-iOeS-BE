package com.ioes.photo.domain.share.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ioes.photo.domain.appconfig.config.AppConfigProperties;
import com.ioes.photo.domain.appconfig.config.AppConfigProperties.PlatformConfig;
import com.ioes.photo.domain.share.config.ShareProperties;
import com.ioes.photo.domain.share.dto.ShareView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ShareHtmlRenderer 테스트")
class ShareHtmlRendererTest {

    private static final String STORE_URL = "https://apps.apple.com/app/id6761319884";
    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.ioes.pickflow";
    private static final String BASE_URL = "https://pickflow-api.us";

    private final ShareHtmlRenderer renderer = renderer(BASE_URL);

    private static ShareHtmlRenderer renderer(String baseUrl) {
        return new ShareHtmlRenderer(
            new AppConfigProperties(
                new PlatformConfig("1.0.0", "1.0.0", false, STORE_URL, "help@pickflow", List.of()),
                new PlatformConfig("1.0.0", "1.0.0", false, ANDROID_STORE_URL, "help@pickflow", List.of())
            ),
            new ShareProperties(baseUrl)
        );
    }

    @Test
    @DisplayName("스팟 정보를 OG 메타태그로 렌더링한다")
    void rendersSpotOgTags() {
        ShareView view = new ShareView("남산 야경", "서울 야경 명소", "https://cdn/spot.jpg");

        String html = renderer.render("k-k", view);

        assertThat(html)
            .contains("<meta property=\"og:title\" content=\"남산 야경\">")
            .contains("<meta property=\"og:description\" content=\"서울 야경 명소\">")
            .contains("<meta property=\"og:image\" content=\"https://cdn/spot.jpg\">")
            .contains("<meta property=\"og:url\" content=\"https://pickflow-api.us/k-k\">")
            .contains(STORE_URL);
    }

    @Test
    @DisplayName("코멘트가 없으면 기본 설명 문구를 사용한다")
    void usesDefaultDescriptionWhenCommentBlank() {
        ShareView view = new ShareView("남산 야경", " ", null);

        String html = renderer.render("k-k", view);

        assertThat(html)
            .contains("<meta property=\"og:description\" content=\"Pickflow에서 이 스팟을 확인해보세요\">")
            .doesNotContain("og:image");
    }

    @Test
    @DisplayName("스팟 정보가 없으면 기본 폴백 페이지를 렌더링한다")
    void rendersFallbackWhenViewNull() {
        String html = renderer.render("k-invalid", null);

        assertThat(html)
            .contains("<meta property=\"og:title\" content=\"Pickflow\">")
            .contains("<meta property=\"og:description\" content=\"Pickflow에서 이 스팟을 확인해보세요\">")
            .doesNotContain("og:image")
            .contains(STORE_URL);
    }

    @Test
    @DisplayName("플랫폼별 스토어 URL을 모두 스크립트에 포함한다")
    void includesBothStoreUrls() {
        String html = renderer.render("k-k", null);

        assertThat(html)
            .contains("var IOS_STORE_URL = \"" + STORE_URL + "\";")
            .contains("var ANDROID_STORE_URL = \"" + ANDROID_STORE_URL + "\";")
            .contains("var isAndroid = /Android/i.test(ua);");
    }

    @Test
    @DisplayName("baseUrl 후행 슬래시가 있어도 og:url에 중복 슬래시가 생기지 않는다")
    void normalizesTrailingSlashInBaseUrl() {
        String html = renderer(BASE_URL + "/").render("k-k", null);

        assertThat(html).contains("<meta property=\"og:url\" content=\"https://pickflow-api.us/k-k\">");
    }

    @Test
    @DisplayName("스팟 이름·코멘트의 HTML 특수문자를 이스케이프한다")
    void escapesHtmlInSpotFields() {
        ShareView view = new ShareView("<script>alert(1)</script>", "\"onload\" & <b>", null);

        String html = renderer.render("k-k", view);

        assertThat(html)
            .doesNotContain("<script>alert(1)</script>")
            .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
            .contains("&quot;onload&quot; &amp; &lt;b&gt;");
    }
}

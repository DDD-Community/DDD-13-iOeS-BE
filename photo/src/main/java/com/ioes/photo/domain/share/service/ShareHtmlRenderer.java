package com.ioes.photo.domain.share.service;

import com.ioes.photo.domain.appconfig.config.AppConfigProperties;
import com.ioes.photo.domain.share.config.ShareProperties;
import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.global.common.util.NullUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공유 링크 OG 미리보기 페이지 HTML 렌더러.
 *
 * OG 메타태그와 함께 카카오톡 인앱 브라우저 분기·앱스토어 유도 스크립트를 포함한다.
 * 스팟 이름·코멘트·이미지 URL은 신뢰할 수 없는 입력이므로 모두 이스케이프한다.
 *
 * @author 김성민
 */
@Component
@RequiredArgsConstructor
public class ShareHtmlRenderer {

    private static final String DEFAULT_TITLE = "Pickflow";
    private static final String DEFAULT_DESCRIPTION = "Pickflow에서 이 스팟을 확인해보세요";

    private final AppConfigProperties appConfigProperties;
    private final ShareProperties shareProperties;

    public String render(String token, ShareView view) {
        String title = view != null ? NullUtils.orDefaultIfBlank(view.name(), DEFAULT_TITLE) : DEFAULT_TITLE;
        String description = view != null
            ? NullUtils.orDefaultIfBlank(view.comment(), DEFAULT_DESCRIPTION)
            : DEFAULT_DESCRIPTION;
        String imageUrl = view != null ? view.imageUrl() : null;
        String pageUrl = baseUrl() + "/" + token;
        return buildHtml(title, description, imageUrl, pageUrl);
    }

    private String baseUrl() {
        return NullUtils.orDefault(shareProperties.baseUrl(), "").replaceAll("/+$", "");
    }

    private String buildHtml(String title, String description, String imageUrl, String pageUrl) {
        StringBuilder html = new StringBuilder()
            .append("<!DOCTYPE html>\n")
            .append("<html lang=\"ko\">\n")
            .append("<head>\n")
            .append("  <meta charset=\"utf-8\">\n")
            .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            .append("  <title>").append(escapeHtml(title)).append("</title>\n")
            .append("  <meta property=\"og:type\" content=\"website\">\n")
            .append(ogTag("og:title", title))
            .append(ogTag("og:description", description))
            .append(ogTag("og:url", pageUrl));
        if (imageUrl != null && !imageUrl.isBlank()) {
            html.append(ogTag("og:image", imageUrl));
        }
        html.append("</head>\n")
            .append("<body>\n")
            .append("  <p>Pickflow 앱으로 이동 중...</p>\n")
            .append("  <p><a id=\"store-link\" href=\"").append(escapeHtml(appConfigProperties.ios().storeUrl()))
            .append("\">앱이 열리지 않으면 여기를 눌러 설치하세요</a></p>\n")
            .append("  <script>\n")
            .append(redirectScript())
            .append("  </script>\n")
            .append("</body>\n")
            .append("</html>\n");
        return html.toString();
    }

    private String redirectScript() {
        return """
              (function () {
                var IOS_STORE_URL = "%s";
                var ANDROID_STORE_URL = "%s";
                var ua = navigator.userAgent || "";
                var isIOS = /iPhone|iPad|iPod/i.test(ua);
                var isAndroid = /Android/i.test(ua);
                var isKakao = /KAKAOTALK/i.test(ua);
                var storeUrl = isAndroid ? ANDROID_STORE_URL : IOS_STORE_URL;
                var link = document.getElementById("store-link");
                if (link) {
                  link.href = storeUrl;
                }
                if (isKakao && (isIOS || isAndroid)) {
                  location.href = "kakaotalk://web/openExternal?url=" + encodeURIComponent(location.href);
                  return;
                }
                location.replace(storeUrl);
              })();
            """.formatted(
            escapeJs(appConfigProperties.ios().storeUrl()),
            escapeJs(appConfigProperties.android().storeUrl())
        );
    }

    private String ogTag(String property, String content) {
        return "  <meta property=\"" + property + "\" content=\"" + escapeHtml(content) + "\">\n";
    }

    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String escapeJs(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}

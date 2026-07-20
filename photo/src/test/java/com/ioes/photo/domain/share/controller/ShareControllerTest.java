package com.ioes.photo.domain.share.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.domain.share.service.ShareHtmlRenderer;
import com.ioes.photo.domain.share.service.SpotShareService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareController 테스트")
class ShareControllerTest {

    @Mock
    private SpotShareService spotShareService;

    @Mock
    private ShareHtmlRenderer shareHtmlRenderer;

    @InjectMocks
    private ShareController shareController;

    @Test
    @DisplayName("HTML 응답에 UTF-8 charset을 명시한다")
    void respondsWithUtf8Html() {
        given(spotShareService.findShareView("k-k")).willReturn(Optional.empty());
        given(shareHtmlRenderer.render("k-k", null)).willReturn("<html>남산</html>");

        ResponseEntity<String> response = shareController.getSharePage("k-k");

        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
    }

    @Test
    @DisplayName("스팟 조회 중 예외가 발생해도 폴백 페이지를 응답한다")
    void respondsWithFallbackWhenLookupFails() {
        willThrow(new RuntimeException("DB 장애")).given(spotShareService).findShareView("k-k");
        given(shareHtmlRenderer.render("k-k", null)).willReturn("<html>fallback</html>");

        ResponseEntity<String> response = shareController.getSharePage("k-k");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("<html>fallback</html>");
    }

    @Test
    @DisplayName("조회된 스팟 정보를 렌더러에 전달한다")
    void passesShareViewToRenderer() {
        ShareView view = new ShareView("남산 야경", "서울 야경 명소", "https://cdn/spot.jpg");
        given(spotShareService.findShareView("k-k")).willReturn(Optional.of(view));
        given(shareHtmlRenderer.render("k-k", view)).willReturn("<html>남산 야경</html>");

        ResponseEntity<String> response = shareController.getSharePage("k-k");

        assertThat(response.getBody()).isEqualTo("<html>남산 야경</html>");
    }
}

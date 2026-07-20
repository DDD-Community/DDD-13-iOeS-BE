package com.ioes.photo.domain.share.controller;

import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.domain.share.service.ShareHtmlRenderer;
import com.ioes.photo.domain.share.service.SpotShareService;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공유 링크 OG 미리보기 페이지 컨트롤러.
 *
 * nginx가 루트 경로({@code /{token}})를 이 엔드포인트로 프록시한다.
 * 링크 크롤러와 사용자 모두 HTML 응답을 기대하므로, 스팟 조회가 실패하더라도
 * 오류 응답 대신 기본 폴백 페이지를 반환한다.
 *
 * @author 김성민
 */
@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
public class ShareController {

    private static final MediaType TEXT_HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final SpotShareService spotShareService;
    private final ShareHtmlRenderer shareHtmlRenderer;

    @GetMapping(value = "/v1/share/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getSharePage(@PathVariable String token) {
        return ResponseEntity.ok()
            .contentType(TEXT_HTML_UTF8)
            .cacheControl(CacheControl.noStore())
            .body(renderPage(token));
    }

    private String renderPage(String token) {
        ShareView view = resolveView(token);
        return shareHtmlRenderer.render(token, view);
    }

    private ShareView resolveView(String token) {
        try {
            return spotShareService.findShareView(token).orElse(null);
        } catch (Exception e) {
            log.warn("공유 스팟 조회 실패로 폴백 페이지를 응답합니다: token={}", token, e);
            return null;
        }
    }
}

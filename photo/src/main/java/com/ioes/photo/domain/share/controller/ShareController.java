package com.ioes.photo.domain.share.controller;

import com.ioes.photo.domain.share.dto.ShareView;
import com.ioes.photo.domain.share.service.ShareHtmlRenderer;
import com.ioes.photo.domain.share.service.SpotShareService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
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
 *
 * @author 김성민
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class ShareController {

    private final SpotShareService spotShareService;
    private final ShareHtmlRenderer shareHtmlRenderer;

    @GetMapping(value = "/v1/share/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getSharePage(@PathVariable String token) {
        ShareView view = spotShareService.findShareView(token).orElse(null);
        String html = shareHtmlRenderer.render(token, view);
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .cacheControl(CacheControl.noStore())
            .body(html);
    }
}

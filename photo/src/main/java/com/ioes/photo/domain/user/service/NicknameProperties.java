package com.ioes.photo.domain.user.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 닉네임 자동 생성에 사용되는 설정값.
 *
 * application.yaml의 app.nickname 하위 값을 바인딩합니다.
 *
 * @author 황제연
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.nickname")
public class NicknameProperties {

    private List<String> adjectives = new ArrayList<>();
    private List<String> nouns = new ArrayList<>();
    private Hashtag hashtag = new Hashtag();

    @Getter
    @Setter
    public static class Hashtag {
        private long min;
        private long max;
        private int maxAttempts;
    }
}
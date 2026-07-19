package com.ioes.photo.domain.share.util;

import java.util.Optional;

/**
 * 공유 링크 토큰 디코더.
 *
 * iOS {@code SpotIDCoder}와 1:1로 일치하는 커스텀 base-62 규칙을 사용한다.
 * 토큰은 {@code {type}-{id}} 형식이며, 알파벳 문자열의 인덱스가 곧 자릿수 값이다.
 * spot 토큰의 type은 항상 1({@code k})이다.
 *
 * @author 김성민
 */
public final class SpotIdCoder {

    private static final String ALPHABET = "5kRmHvNpLqT8sYuWdXjZcFbGiOeA9n2BgVrMo3CQfthE0SaKwIPDy61lJU74xz";
    private static final int BASE = 62;
    private static final long SPOT_TYPE = 1L;

    private SpotIdCoder() {
    }

    public static Optional<Long> decodeSpotId(String token) {
        if (token == null) {
            return Optional.empty();
        }
        int hyphen = token.indexOf('-');
        if (hyphen < 0) {
            return Optional.empty();
        }
        Long type = decode(token.substring(0, hyphen));
        if (type == null || type != SPOT_TYPE) {
            return Optional.empty();
        }
        return Optional.ofNullable(decode(token.substring(hyphen + 1)));
    }

    private static Long decode(String value) {
        long result = 0;
        for (int i = 0; i < value.length(); i++) {
            int index = ALPHABET.indexOf(value.charAt(i));
            if (index < 0 || result > (Long.MAX_VALUE - index) / BASE) {
                return null;
            }
            result = result * BASE + index;
        }
        return result;
    }
}

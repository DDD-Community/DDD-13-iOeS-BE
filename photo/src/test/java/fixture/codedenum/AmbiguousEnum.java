package fixture.codedenum;

import com.ioes.photo.global.persistence.enumeration.CodedEnum;

/**
 * {@link com.ioes.photo.global.config.web.converter.StringToCodedEnumConverter} 모호성 검증 테스트 전용 fixture.
 *
 * <p>{@code BAR} 상수의 name이 {@code FOO} 상수의 code와 겹치도록 의도적으로 설계했다.
 * {@code com.ioes.photo} 패키지 밖에 두어, 애플리케이션 컨텍스트 기동 시 수행되는
 * {@code com.ioes.photo} 전수 스캔에 이 fixture가 걸리지 않도록 한다.</p>
 *
 * @author 황제연
 */
public enum AmbiguousEnum implements CodedEnum {
    FOO("BAR"),
    BAR("F");

    private final String code;

    AmbiguousEnum(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}

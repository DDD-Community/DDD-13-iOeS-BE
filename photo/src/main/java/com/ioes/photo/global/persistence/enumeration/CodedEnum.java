package com.ioes.photo.global.persistence.enumeration;

/**
 * DB 저장용 짧은 코드를 가지는 enum 계약.
 *
 * <p>{@code @Enumerated(STRING)}은 enum 이름 전체를 저장해 공간을 낭비하고,
 * {@code @Enumerated(ORDINAL)}은 선언 순서 변경에 취약하다.
 * 이 인터페이스를 구현하는 enum은 {@link com.ioes.photo.global.persistence.enumeration.CodedEnumConverter}
 * 를 통해 짧고 안정적인 코드(예: "P", "K")로 저장된다.</p>
 *
 * <h3>구현 규칙</h3>
 * <ul>
 *   <li>각 enum 상수는 고유한 {@code code} 를 가져야 한다.</li>
 *   <li>{@code code} 는 가급적 1~3자의 ASCII 문자열을 사용한다.</li>
 *   <li>한 번 운영에 반영된 코드는 변경하지 않는다 (DB 데이터 호환성).</li>
 * </ul>
 *
 * @author 김성민
 */
public interface CodedEnum {

    String getCode();
}

package com.ioes.photo.global.common.annotation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Double 값을 지정한 소수점 자리수로 절삭(truncation)하여 직렬화하는 애노테이션.
 *
 * 기본값은 소수점 1자리 절삭이며, scale 속성으로 자릿수를 조정할 수 있다.
 * 반올림이 아닌 절삭(버림)을 적용한다. (예: 1.29 → 1.2)
 *
 * 응답 전용:@JsonSerialize만 사용하므로 직렬화(응답) 시에만 동작하며,
 * 역직렬화(요청 파싱)에는 영향을 주지 않는다.
 *
 * @author 황제연
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = TruncateDecimalSerializer.class)
public @interface TruncateDecimal {

    int scale() default 1;
}

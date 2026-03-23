package com.ioes.photo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션 컨텍스트 통합 테스트.
 * 모든 Bean이 정상적으로 생성되고 컨텍스트가 로드되는지 확인합니다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("PhotoApplication 통합 테스트")
class PhotoApplicationTests {

    @Test
    @DisplayName("Spring 애플리케이션 컨텍스트 정상 로드")
    void contextLoads() {
        // Spring 컨텍스트 로드 성공 여부만 확인
    }
}
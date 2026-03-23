package com.ioes.photo.global.config.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Auditing 및 트랜잭션 관리 설정 클래스.
 * Spring Data JPA 감사 애노테이션이 자동으로 동작하도록 합니다.
 * EnableTransactionManagement의 order를 LOWEST_PRECEDENCE - 2로 설정합니다.
 * 이렇게 하면 Spring의 TransactionInterceptor가 TransactionLoggingAspect보다
 * 바깥쪽에서 실행되어, Aspect 내부에서 TransactionSynchronizationManager로
 * 실제 트랜잭션 활성화 여부를 정확히 확인할 수 있습니다.
 *
 *
 * TransactionInterceptor  (order: LOWEST_PRECEDENCE - 2)  ← 트랜잭션 시작/종료
 *   └─ TransactionLoggingAspect (order: LOWEST_PRECEDENCE - 1)
 *        └─ isActualTransactionActive() 확인 후 txId 설정
 *             └─ 실제 메서드 실행
 *
 * @see com.ioes.photo.global.entity.BaseEntity
 * @see com.ioes.photo.global.logging.TransactionLoggingAspect
 * @author 황제연
 */
@Configuration
@EnableJpaAuditing
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 2)
public class JpaAuditingConfig {
}
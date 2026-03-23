package com.ioes.photo.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * @Transactional 메서드 실행 시 MDC에 트랜잭션 ID를 설정하는 AOP Aspect.
 *
 * JpaAuditingConfig에서 @EnableTransactionManagement(order = LOWEST_PRECEDENCE - 2)로 설정하여
 * TransactionInterceptor가 Aspect보다 바깥쪽(높은 우선순위)에서 실행됩니다
 * 따라서 이 Aspect 내부에서 TransactionSynchronizationManager_isActualTransactionActive()로 실제 트랜잭션 여부를 정확히 확인할 수 있습니다.
 *
 * 실행 순서:
 * TransactionInterceptor  (order: LOWEST_PRECEDENCE - 2)  ← 트랜잭션 시작
 *   └─ TransactionLoggingAspect (order: LOWEST_PRECEDENCE - 1)
 *        └─ isActualTransactionActive() 확인 → true 이면 txId 설정
 *             └─ 실제 @Transactional 메서드 실행
 *
 * propagation = NOT_SUPPORTED처럼 실제 트랜잭션이 없는 경우에는 txId를 설정하지 않습니다.
 *
 * @see MdcFilter
 * @see com.ioes.photo.global.config.jpa.JpaAuditingConfig
 * @author 황제연
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class TransactionLoggingAspect {

    private static final String TX_ID_KEY = "txId";

    /**
     * 실제 트랜잭션이 활성화된 경우에만 MDC에 트랜잭션 ID를 설정합니다.
     *
     * @param pjp 실행 중인 조인포인트
     * @return 원본 메서드 반환값
     * @throws Throwable 원본 메서드에서 발생한 예외를 그대로 전파
     */
    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional)" +
        " || @within(org.springframework.transaction.annotation.Transactional)"
    )
    public Object setTransactionId(ProceedingJoinPoint pjp) throws Throwable {
        // TransactionInterceptor가 먼저 실행된 후 여기에 도달하므로
        // isActualTransactionActive()가 실제 트랜잭션 여부를 정확히 반영함
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return pjp.proceed();
        }

        boolean isOutermost = MDC.get(TX_ID_KEY) == null;
        if (isOutermost) {
            MDC.put(TX_ID_KEY, UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            log.debug("transaction begin");
        }
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            if (isOutermost) {
                log.debug("transaction rollback cause={}", ex.getMessage());
            }
            throw ex;
        } finally {
            if (isOutermost) {
                log.debug("transaction end");
                MDC.remove(TX_ID_KEY);
            }
        }
    }
}
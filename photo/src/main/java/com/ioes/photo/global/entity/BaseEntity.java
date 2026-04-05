package com.ioes.photo.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA 감사(Auditing) 기능을 제공하는 기본 엔티티 추상 클래스.
 *
 * 모든 도메인 엔티티가 상속받아야 하는 공통 필드를 제공합니다.
 *
 * @see com.ioes.photo.global.config.jpa.JpaAuditingConfig
 * @author 황제연
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /*
     * 내부 PK, FK 참조용
     * Long타입 identity (DB의 Auto_increment 위임)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /*
     * 엔티티 최초 생성 일시
     */
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /*
     * 엔티티 최종 수정 일시
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
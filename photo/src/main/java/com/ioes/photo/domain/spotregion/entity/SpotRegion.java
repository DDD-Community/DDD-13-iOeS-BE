package com.ioes.photo.domain.spotregion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 지역 필터용 참조 데이터.
 *
 * 이후 지역 추가, 조회 API 성능, 어드민 페이지 활용을 고려해 enum 대신 테이블로 관리한다.
 * region_id 는 {@code spots.region_id} 에서 참조한다
 * spots 는 주소(address) 접두어를 region_name 과 매칭해 지역을 배정한다
 * ({@link com.ioes.photo.domain.spotregion.service.SpotRegionResolver} 참고).
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(name = "spot_regions")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id", updatable = false, nullable = false)
    private Long regionId;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "new_date", updatable = false, nullable = false)
    private LocalDateTime newDate;

    @LastModifiedDate
    @Column(name = "edt_date", nullable = false)
    private LocalDateTime edtDate;
}

package com.ioes.photo.domain.crowdarea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서울시 실시간 도시데이터 장소(핫스팟) 참조 데이터.
 *
 * 스팟 좌표를 가장 가까운 장소에 매핑할 때 기준 좌표로 사용한다.
 * 위경도는 각 장소 경계 폴리곤의 중심점이며, 시드(V8)로만 적재되는 읽기 전용 데이터다.
 *
 * @author 김성민
 */
@Getter
@Entity
@Table(name = "crowd_areas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrowdArea {

    @Id
    @Column(name = "area_code", length = 20)
    private String areaCode;

    @Column(name = "area_name", nullable = false, length = 50)
    private String areaName;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;
}

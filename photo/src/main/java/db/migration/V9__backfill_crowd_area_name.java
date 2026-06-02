package db.migration;

import com.ioes.photo.global.common.util.GeoUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 기존 PUBLISHED 스팟의 혼잡도 장소명(crowd_area_name) backfill.
 *
 * crowd_area_name 이 비어 있던 스팟은 CrowdCollector 대상에서 제외되어 혼잡도가 수집되지 않았다.
 * V8 로 적재된 장소(crowd_areas) 중 가장 가까운 장소를 {@link GeoUtils} 로 찾아 채운다.
 * 런타임 자동 매핑(CrowdAreaMapper)과 동일한 Haversine 거리식을 사용하며,
 * 임계값은 마이그레이션 작성 시점의 기본값(3km)으로 고정한다.
 *
 * @author 김성민
 */
public class V9__backfill_crowd_area_name extends BaseJavaMigration {

    private static final double MAX_DISTANCE_METERS = 3000.0;

    private static final String SELECT_AREAS =
        "SELECT area_name, latitude, longitude FROM crowd_areas";

    private static final String SELECT_TARGET_SPOTS =
        "SELECT id, latitude, longitude FROM spots "
            + "WHERE status = 'B' AND crowd_area_name IS NULL "
            + "AND latitude IS NOT NULL AND longitude IS NOT NULL";

    private static final String UPDATE_AREA_NAME =
        "UPDATE spots SET crowd_area_name = ? WHERE id = ?";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<Area> areas = loadAreas(connection);
        if (areas.isEmpty()) {
            return;
        }

        try (Statement select = connection.createStatement();
             ResultSet rs = select.executeQuery(SELECT_TARGET_SPOTS);
             PreparedStatement update = connection.prepareStatement(UPDATE_AREA_NAME)) {
            while (rs.next()) {
                String areaName = findNearestAreaName(rs.getDouble("latitude"), rs.getDouble("longitude"), areas);
                if (areaName == null) {
                    continue;
                }
                update.setString(1, areaName);
                update.setLong(2, rs.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private List<Area> loadAreas(Connection connection) throws Exception {
        List<Area> areas = new ArrayList<>();
        try (Statement select = connection.createStatement();
             ResultSet rs = select.executeQuery(SELECT_AREAS)) {
            while (rs.next()) {
                areas.add(new Area(
                    rs.getString("area_name"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")));
            }
        }
        return areas;
    }

    private String findNearestAreaName(double latitude, double longitude, List<Area> areas) {
        String nearest = null;
        double nearestMeters = Double.MAX_VALUE;
        for (Area area : areas) {
            double meters = GeoUtils.distanceMeters(latitude, longitude, area.latitude(), area.longitude());
            if (meters < nearestMeters) {
                nearestMeters = meters;
                nearest = area.name();
            }
        }
        return nearestMeters > MAX_DISTANCE_METERS ? null : nearest;
    }

    private record Area(String name, double latitude, double longitude) {
    }
}

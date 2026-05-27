package db.migration;

import com.ioes.photo.external.weather.util.LccGridConverter;
import com.ioes.photo.external.weather.util.LccGridConverter.GridPoint;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 기존 스팟 기상청 격자(grid_nx/grid_ny) backfill.
 *
 * 배치 업로드로 등록된 스팟은 격자가 NULL이라 WeatherCollector 대상에서 제외되었다.
 * 위경도가 있으나 격자가 비어 있는 스팟을 {@link LccGridConverter}로 계산해 채운다.
 * 배치 업로드 경로와 동일한 변환식을 재사용한다.
 *
 * @author 김성민
 */
public class V6__backfill_spot_grid extends BaseJavaMigration {

    private static final String SELECT_MISSING_GRID =
        "SELECT id, latitude, longitude FROM spots "
            + "WHERE (grid_nx IS NULL OR grid_ny IS NULL) "
            + "AND latitude IS NOT NULL AND longitude IS NOT NULL";

    private static final String UPDATE_GRID =
        "UPDATE spots SET grid_nx = ?, grid_ny = ? WHERE id = ?";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement select = connection.createStatement();
             ResultSet rs = select.executeQuery(SELECT_MISSING_GRID);
             PreparedStatement update = connection.prepareStatement(UPDATE_GRID)) {
            while (rs.next()) {
                GridPoint grid = LccGridConverter.toGrid(rs.getDouble("latitude"), rs.getDouble("longitude"));
                update.setInt(1, grid.nx());
                update.setInt(2, grid.ny());
                update.setLong(3, rs.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}

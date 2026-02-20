-- Remove the restrictive unique constraint that prevents historical rivalries
-- In H2, we use a script to find and drop the unique constraint on (wrestler1_id, wrestler2_id)
-- because auto-generated names (like CONSTRAINT_7) are unreliable.

CREATE ALIAS IF NOT EXISTS DROP_RIVALRY_UNIQUE_CONSTRAINT AS $$
import java.sql.*;
import java.util.*;
@CODE
void dropRivalryUnique(Connection conn) throws SQLException {
    DatabaseMetaData meta = conn.getMetaData();
    String tableName = "RIVALRY";
    
    // Map to store unique index names and their columns
    Map<String, List<String>> indexColumns = new HashMap<>();
    
    // Get unique indexes for the table
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, true, false)) {
        while (rs.next()) {
            String indexName = rs.getString("INDEX_NAME");
            String columnName = rs.getString("COLUMN_NAME");
            if (indexName != null && columnName != null) {
                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName.toUpperCase());
            }
        }
    }
    
    // Look for the index that covers exactly (WRESTLER1_ID, WRESTLER2_ID)
    for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
        List<String> columns = entry.getValue();
        if (columns.size() == 2 && columns.contains("WRESTLER1_ID") && columns.contains("WRESTLER2_ID")) {
            String name = entry.getKey();
            try (Statement stmt = conn.createStatement()) {
                // In H2, unique constraints often manifest as both a constraint and an index.
                // We attempt to drop it as a constraint first.
                try {
                    stmt.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + name);
                } catch (SQLException e) {
                    // Fallback to dropping as an index if constraint drop fails
                    stmt.execute("DROP INDEX IF EXISTS " + name);
                }
                return;
            }
        }
    }
}
$$;

CALL DROP_RIVALRY_UNIQUE_CONSTRAINT();
DROP ALIAS DROP_RIVALRY_UNIQUE_CONSTRAINT;

-- Add a non-unique index for performance
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);

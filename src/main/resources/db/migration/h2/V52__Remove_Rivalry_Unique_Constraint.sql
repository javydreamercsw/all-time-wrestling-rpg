-- Remove the restrictive unique constraint that prevents historical rivalries
-- In H2, we use a script to find and drop the unique constraint on (wrestler1_id, wrestler2_id)
-- because auto-generated names (like CONSTRAINT_7) are unreliable.

CREATE ALIAS IF NOT EXISTS DROP_RIVALRY_UNIQUE_CONSTRAINT AS $$
import java.sql.*;
@CODE
void dropRivalryUnique(Connection conn) throws SQLException {
    // Direct query for H2 2.x to find the unique constraint covering both columns
    String query = "SELECT tc.CONSTRAINT_NAME " +
                   "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
                   "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu " +
                   "  ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                   "  AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA " +
                   "WHERE tc.TABLE_NAME = 'RIVALRY' " +
                   "  AND tc.CONSTRAINT_TYPE = 'UNIQUE' " +
                   "  AND kcu.COLUMN_NAME IN ('WRESTLER1_ID', 'WRESTLER2_ID') " +
                   "GROUP BY tc.CONSTRAINT_NAME " +
                   "HAVING COUNT(*) = 2";
    
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        while (rs.next()) {
            String constraintName = rs.getString("CONSTRAINT_NAME");
            if (constraintName != null && !constraintName.isEmpty()) {
                System.out.println("Dropping rivalry unique constraint: " + constraintName);
                try (Statement dropStmt = conn.createStatement()) {
                    dropStmt.execute("ALTER TABLE RIVALRY DROP CONSTRAINT " + constraintName);
                }
            }
        }
    } catch (SQLException e) {
        // Log but don't fail, as the index might have been already dropped or doesn't exist
        System.err.println("Warning: Could not drop rivalry unique constraint: " + e.getMessage());
    }
}
$$;

CALL DROP_RIVALRY_UNIQUE_CONSTRAINT();
DROP ALIAS DROP_RIVALRY_UNIQUE_CONSTRAINT;

-- Add a non-unique index for performance
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);

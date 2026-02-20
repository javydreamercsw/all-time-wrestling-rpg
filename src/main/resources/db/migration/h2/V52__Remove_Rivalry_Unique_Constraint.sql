-- Remove the restrictive unique constraint that prevents historical rivalries
-- In H2, we use a script to find and drop the unique constraint on (wrestler1_id, wrestler2_id)
-- because auto-generated names (like CONSTRAINT_7) are unreliable.

CREATE ALIAS IF NOT EXISTS DROP_RIVALRY_UNIQUE_CONSTRAINT AS $$
import java.sql.*;
@CODE
void dropRivalryUnique(Connection conn) throws SQLException {
    // Find the unique constraint name for the rivalry table on columns (wrestler1_id, wrestler2_id)
    // H2 2.x information_schema.constraints is the most reliable way.
    String query = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS " +
                   "WHERE TABLE_NAME='RIVALRY' " +
                   "AND COLUMN_LIST='WRESTLER1_ID,WRESTLER2_ID' " +
                   "AND CONSTRAINT_TYPE='UNIQUE'";
    
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        if (rs.next()) {
            String constraintName = rs.getString("CONSTRAINT_NAME");
            // Use CASCADE to handle any internal dependencies (like auto-generated indexes)
            stmt.execute("ALTER TABLE RIVALRY DROP CONSTRAINT " + constraintName);
        }
    } catch (SQLException e) {
        // If it fails, log it but don't stop the migration as the JPA entity will handle new schemas
        System.err.println("Warning: Could not drop rivalry unique constraint: " + e.getMessage());
    }
}
$$;

CALL DROP_RIVALRY_UNIQUE_CONSTRAINT();
DROP ALIAS DROP_RIVALRY_UNIQUE_CONSTRAINT;

-- Add a non-unique index for performance
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);

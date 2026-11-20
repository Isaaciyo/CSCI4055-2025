package src.query;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import src.database.DatabaseManager;

/**
 * All SQL for the project.
 *
 * Expected schema (COMPANY database):
 *   EMPLOYEE(Fname, Minit, Lname, Ssn, ..., Dno)
 *   DEPARTMENT(Dname, Dnumber, ...)
 *   PROJECT(Pname, Pnumber, ...)
 *   WORKS_ON(Essn, Pno, Hours)
 */
public class EmployeeSearchQuery {

	private static final String[] REQUIRED_TABLES = { "employee", "department", "project", "works_on" };

	/**
	 * Checks that the open database really has the COMPANY tables.
	 * Throws a readable SQLException if any are missing.
	 */
	public void checkSchema() throws SQLException {
		Connection conn = DatabaseManager.getConnection();
		List<String> found = new ArrayList<>();
		DatabaseMetaData md = conn.getMetaData();
		try (ResultSet rs = md.getTables(null, null, "%", new String[] { "TABLE" })) {
			while (rs.next()) found.add(rs.getString("TABLE_NAME").toLowerCase());
		}
		List<String> missing = new ArrayList<>();
		for (String t : REQUIRED_TABLES) {
			if (!found.contains(t)) missing.add(t.toUpperCase());
		}
		if (!missing.isEmpty()) {
			throw new SQLException("This database does not use the COMPANY schema (missing: "
					+ String.join(", ", missing) + ").");
		}
	}

	public List<String> loadDepartments() throws SQLException {
		return readNames("SELECT Dname FROM DEPARTMENT ORDER BY Dname");
	}

	public List<String> loadProjects() throws SQLException {
		return readNames("SELECT Pname FROM PROJECT ORDER BY Pname");
	}

	private List<String> readNames(String sql) throws SQLException {
		List<String> out = new ArrayList<>();
		Connection conn = DatabaseManager.getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) out.add(rs.getString(1));
		}
		return out;
	}

	// TODO (search): add searchEmployees(depts, notDept, projs, notProj) here.
}

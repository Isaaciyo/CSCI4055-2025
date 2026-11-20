import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Read-only access to the standard COMPANY tables in an existing SQLite file. */
public final class CompanyDatabase {
    private final String url;

    /** Keep the database ID with each displayed name, including duplicate names. */
    public static final class Choice {
        public final int id;
        public final String name;

        public Choice(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public static final class Catalog {
        public final List<Choice> departments;
        public final List<Choice> projects;

        private Catalog(List<Choice> departments, List<Choice> projects) {
            this.departments = Collections.unmodifiableList(departments);
            this.projects = Collections.unmodifiableList(projects);
        }
    }

    public CompanyDatabase(String filename) throws SQLException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new SQLException("Enter the path to an existing SQLite database file.");
        }
        try {
            Path file = Paths.get(filename.trim());
            // Preserve main's short-name convention (company -> resources/company.db).
            // Explicit paths always refer to exactly the path the user entered.
            if (!Files.exists(file) && !file.isAbsolute() && file.getNameCount() == 1) {
                String name = file.toString();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".db")) {
                    name += ".db";
                }
                file = Paths.get("resources", name);
            }
            file = file.toRealPath();
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                throw new SQLException("The database must be a readable file.");
            }
            // URI encoding handles spaces, '?' and '#' in filenames correctly.
            url = "jdbc:sqlite:" + file.toUri().toASCIIString() + "?mode=ro";
        } catch (IOException | InvalidPathException exception) {
            throw new SQLException("Database file does not exist or cannot be read: " + filename, exception);
        }
    }

    private Connection connect() throws SQLException {
        Properties options = new Properties();
        // SQLITE_OPEN_READONLY also prevents creation if the file is removed later.
        options.setProperty("open_mode", "1");
        try {
            return DriverManager.getConnection(url, options);
        } catch (SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("No suitable driver")) {
                throw new SQLException("SQLite JDBC driver is missing. Add the SQLite JDBC JAR to the runtime classpath.", exception);
            }
            throw exception;
        }
    }

    public Catalog loadCatalog() throws SQLException {
        try (Connection connection = connect()) {
            List<Choice> departments = readChoices(connection,
                    "SELECT Dnumber, Dname FROM DEPARTMENT ORDER BY Dname, Dnumber");
            List<Choice> projects = readChoices(connection,
                    "SELECT Pnumber, Pname FROM PROJECT ORDER BY Pname, Pnumber");
            // Fail during Fill if the remaining required tables/columns are absent.
            try (Statement statement = connection.createStatement()) {
                try (ResultSet ignored = statement.executeQuery(
                        "SELECT Ssn, Fname, Minit, Lname, Dno FROM EMPLOYEE WHERE 1 = 0")) {
                    // Schema validation only; no employee data is loaded here.
                }
                try (ResultSet ignored = statement.executeQuery(
                        "SELECT Essn, Pno FROM WORKS_ON WHERE 1 = 0")) {
                    // Schema validation only.
                }
            }
            return new Catalog(departments, projects);
        }
    }

    private static List<Choice> readChoices(Connection connection, String sql) throws SQLException {
        List<Choice> choices = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                choices.add(new Choice(rows.getInt(1), rows.getString(2)));
            }
        }
        return choices;
    }

    /**
     * Match any selection within a list, and require both active list filters.
     * An empty selection imposes no condition, even when its Not box is checked.
     */
    public List<String> search(List<Choice> departments, boolean notDepartment,
            List<Choice> projects, boolean notProject) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT e.Fname, e.Minit, e.Lname FROM EMPLOYEE e WHERE 1 = 1");
        List<Integer> parameters = new ArrayList<>();

        if (!departments.isEmpty()) {
            if (notDepartment) {
                sql.append(" AND (e.Dno IS NULL OR e.Dno NOT IN (");
            } else {
                sql.append(" AND e.Dno IN (");
            }
            appendIds(sql, parameters, departments);
            sql.append(notDepartment ? "))" : ")");
        }
        if (!projects.isEmpty()) {
            sql.append(notProject ? " AND NOT EXISTS (" : " AND EXISTS (");
            sql.append("SELECT 1 FROM WORKS_ON w WHERE w.Essn = e.Ssn AND w.Pno IN (");
            appendIds(sql, parameters, projects);
            sql.append("))");
        }
        sql.append(" ORDER BY e.Lname, e.Fname, e.Ssn");

        List<String> employees = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setInt(i + 1, parameters.get(i));
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    List<String> parts = new ArrayList<>();
                    for (int i = 1; i <= 3; i++) {
                        String part = rows.getString(i);
                        if (part != null && !part.trim().isEmpty()) {
                            parts.add(part.trim());
                        }
                    }
                    employees.add(String.join(" ", parts));
                }
            }
        }
        return employees;
    }

    private static void appendIds(StringBuilder sql, List<Integer> parameters, List<Choice> choices) {
        for (int i = 0; i < choices.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('?');
            parameters.add(choices.get(i).id);
        }
    }
}

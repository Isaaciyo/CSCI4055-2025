import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Integration tests against temporary SQLite files; no JUnit dependency. */
public final class CompanyDatabaseTest {
    private static int checks;
    private static final List<CompanyDatabase.Choice> NONE = Collections.emptyList();

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("company-test-");
        // Exercise SQLite URI escaping as well as the usual database path.
        Path file = directory.resolve("Company's data #1?.db");
        Path missing = directory.resolve("missing.db");
        Path invalid = directory.resolve("invalid.db");
        Path empty = directory.resolve("empty.db");
        Path incomplete = directory.resolve("incomplete.db");
        Path removed = directory.resolve("removed.db");
        try {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement statement = connection.createStatement()) {
                createSchema(statement);
                statement.executeUpdate("INSERT INTO DEPARTMENT VALUES (1, 'Headquarters'),"
                        + " (4, 'Administration'), (5, 'Research')");
                statement.executeUpdate("INSERT INTO PROJECT VALUES (10, 'Product X'),"
                        + " (20, 'Product X'), (30, 'O''Brien Project')");
                statement.executeUpdate("INSERT INTO EMPLOYEE VALUES"
                        + " ('1', 'Alice', 'Q', 'Able', 1),"
                        + " ('2', 'Bob', NULL, 'Baker', 4),"
                        + " ('3', 'Cara', '', 'Cole', 5),"
                        + " ('4', 'Dan', NULL, 'Dunn', 5),"
                        + " ('5', 'Eve', NULL, 'Evans', NULL),"
                        + " ('6', 'Same', NULL, 'Name', 1),"
                        + " ('7', 'Same', NULL, 'Name', 4)");
                statement.executeUpdate("INSERT INTO WORKS_ON VALUES"
                        + " ('1', 10), ('1', 20), ('2', 20), ('3', 30), ('5', 10)");
            }
            byte[] original = Files.readAllBytes(file);
            CompanyDatabase database = new CompanyDatabase(file.toString());
            CompanyDatabase.Catalog catalog = database.loadCatalog();
            equal("department order", ids(catalog.departments), Arrays.asList(4, 1, 5));
            equal("project order and duplicate names", ids(catalog.projects), Arrays.asList(30, 10, 20));
            equal("quoted project name", catalog.projects.get(0).name, "O'Brien Project");

            String[] all = {"Alice Q Able", "Bob Baker", "Cara Cole", "Dan Dunn", "Eve Evans", "Same Name", "Same Name"};
            expect(database, "no filters", NONE, false, NONE, false, all);
            expect(database, "Not with no selections", NONE, true, NONE, true, all);
            expect(database, "single department", choices(5), false, NONE, false, "Cara Cole", "Dan Dunn");
            expect(database, "multiple departments", choices(1, 4), false, NONE, false,
                    "Alice Q Able", "Bob Baker", "Same Name", "Same Name");
            expect(database, "department Not includes unassigned employees", choices(1, 4), true, NONE, false,
                    "Cara Cole", "Dan Dunn", "Eve Evans");
            expect(database, "single project", NONE, false, choices(20), false, "Alice Q Able", "Bob Baker");
            expect(database, "project IDs distinguish duplicate names", NONE, false, choices(10), false,
                    "Alice Q Able", "Eve Evans");
            expect(database, "multiple projects do not duplicate employees", NONE, false, choices(10, 20), false,
                    "Alice Q Able", "Bob Baker", "Eve Evans");
            expect(database, "project Not excludes employees who also work on another project",
                    NONE, false, choices(10), true, "Bob Baker", "Cara Cole", "Dan Dunn", "Same Name", "Same Name");
            expect(database, "multiple project Not includes employees with no projects",
                    NONE, false, choices(10, 20), true, "Cara Cole", "Dan Dunn", "Same Name", "Same Name");
            expect(database, "department AND project", choices(1, 4), false, choices(10), false, "Alice Q Able");
            expect(database, "department AND Not project", choices(1, 4), false, choices(10), true,
                    "Bob Baker", "Same Name", "Same Name");
            expect(database, "Not department AND project", choices(1, 4), true, choices(10), false, "Eve Evans");
            expect(database, "Not department AND Not project", choices(1, 4), true, choices(10), true,
                    "Cara Cole", "Dan Dunn");
            expect(database, "no matches", choices(5), false, choices(10, 20), false);
            equal("searches leave the file unchanged", Arrays.equals(original, Files.readAllBytes(file)), true);

            // A second Fill must reread data instead of appending or caching old entries.
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO DEPARTMENT VALUES (7, 'New department')");
            }
            equal("catalog refresh", ids(database.loadCatalog().departments), Arrays.asList(4, 1, 7, 5));
            equal("repeated catalog load does not accumulate entries", database.loadCatalog().departments.size(), 4);

            CompanyDatabase padded = new CompanyDatabase("  " + file + "  ");
            equal("filename with surrounding spaces", ids(padded.loadCatalog().departments), Arrays.asList(4, 1, 7, 5));

            expectSqlFailure("blank filename", () -> new CompanyDatabase(" "));
            expectSqlFailure("missing file", () -> new CompanyDatabase(missing.toString()));
            equal("missing database was not created", Files.exists(missing), false);
            expectSqlFailure("directory is not a database", () -> new CompanyDatabase(directory.toString()));
            Files.write(invalid, "This is not a database".getBytes(StandardCharsets.UTF_8));
            expectSqlFailure("invalid database", () -> new CompanyDatabase(invalid.toString()).loadCatalog());
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + incomplete);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE DEPARTMENT (Dnumber INTEGER, Dname TEXT)");
                statement.executeUpdate("CREATE TABLE PROJECT (Pnumber INTEGER, Pname TEXT)");
            }
            expectSqlFailure("incomplete COMPANY schema", () -> new CompanyDatabase(incomplete.toString()).loadCatalog());
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + empty);
                    Statement statement = connection.createStatement()) {
                createSchema(statement);
            }
            CompanyDatabase emptyDatabase = new CompanyDatabase(empty.toString());
            equal("empty departments", emptyDatabase.loadCatalog().departments.size(), 0);
            equal("empty projects", emptyDatabase.loadCatalog().projects.size(), 0);
            expect(emptyDatabase, "empty employees", NONE, false, NONE, false);

            Files.copy(empty, removed);
            CompanyDatabase removedDatabase = new CompanyDatabase(removed.toString());
            Files.delete(removed);
            expectSqlFailure("deleted database fails when reopening", removedDatabase::loadCatalog);
            equal("deleted database was not recreated", Files.exists(removed), false);
            System.out.println("Passed " + checks + " database integration checks.");
        } finally {
            for (Path path : Arrays.asList(file, missing, invalid, empty, incomplete, removed)) {
                Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        }
    }

    private static void createSchema(Statement statement) throws SQLException {
        // Only columns used by the application are needed by this test fixture.
        statement.executeUpdate("CREATE TABLE DEPARTMENT (Dnumber INTEGER PRIMARY KEY, Dname TEXT)");
        statement.executeUpdate("CREATE TABLE PROJECT (Pnumber INTEGER PRIMARY KEY, Pname TEXT)");
        statement.executeUpdate("CREATE TABLE EMPLOYEE (Ssn TEXT PRIMARY KEY, Fname TEXT, Minit TEXT, Lname TEXT, Dno INTEGER)");
        statement.executeUpdate("CREATE TABLE WORKS_ON (Essn TEXT, Pno INTEGER, PRIMARY KEY (Essn, Pno))");
    }

    private static List<CompanyDatabase.Choice> choices(int... ids) {
        List<CompanyDatabase.Choice> choices = new ArrayList<>();
        for (int id : ids) {
            choices.add(new CompanyDatabase.Choice(id, "Test selection"));
        }
        return choices;
    }

    private static List<Integer> ids(List<CompanyDatabase.Choice> choices) {
        List<Integer> ids = new ArrayList<>();
        for (CompanyDatabase.Choice choice : choices) {
            ids.add(choice.id);
        }
        return ids;
    }

    private static void expect(CompanyDatabase database, String label,
            List<CompanyDatabase.Choice> departments, boolean notDepartment,
            List<CompanyDatabase.Choice> projects, boolean notProject, String... names) throws SQLException {
        equal(label, database.search(departments, notDepartment, projects, notProject), Arrays.asList(names));
    }

    private static void equal(String label, Object actual, Object expected) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private interface SqlAction {
        void run() throws SQLException;
    }

    private static void expectSqlFailure(String label, SqlAction action) throws SQLException {
        try {
            action.run();
        } catch (SQLException expected) {
            checks++;
            return;
        }
        throw new AssertionError(label + ": expected SQLException");
    }
}

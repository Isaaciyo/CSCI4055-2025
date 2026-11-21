# COMPANY Employee Search

Java Swing implementation of Dr. Smith's database project, extended from the provided `EmployeeSearchFrame.java` template. The application opens an existing SQLite COMPANY database and searches employees by department and project.

Group: **Ashutosh Thapa, Isaaciyo, Shirshak18**. Original template: https://github.com/proflon/CSCI4055-2025.

## Project structure

| Path | Purpose |
| --- | --- |
| `EmployeeSearchFrame.java` | Swing interface and event handlers |
| `CompanyDatabase.java` | Read-only connections, catalog loading, and employee queries |
| `CompanyDatabaseTest.java` | Standalone SQLite integration checks |
| `resources/company.db` | Included COMPANY database |
| `resources/buildCOMPANYdb.sql` | Original COMPANY schema and sample data script |
| `src/database/DatabaseManager.java` | Finds the database file and opens read-only SQLite connections |
| `src/query/EmployeeSearchQuery.java` | Earlier catalog helper retained from main |

The GUI uses `CompanyDatabase` for queries, which uses `DatabaseManager` to find and open the database file. Every database operation closes its connection with try-with-resources, so there is no persistent connection to close when the window exits. Pressing Enter in the Database box runs Fill.

## Setup and run

Install a JDK (Java 8 or newer). The SQLite JDBC driver (`lib/sqlite-jdbc-3.46.1.3.jar`) and the sample database (`resources/company.db`) are already in the repository, so nothing needs to be downloaded. Run the commands from the project directory.

macOS / Linux:

```sh
javac -d out EmployeeSearchFrame.java CompanyDatabase.java CompanyDatabaseTest.java src/database/DatabaseManager.java
java -cp 'out:lib/sqlite-jdbc-3.46.1.3.jar' EmployeeSearchFrame
```

Windows:

```bat
javac -d out EmployeeSearchFrame.java CompanyDatabase.java CompanyDatabaseTest.java src\database\DatabaseManager.java
java -cp "out;lib/sqlite-jdbc-3.46.1.3.jar" EmployeeSearchFrame
```

In Eclipse, add these Java files to a Java project's source folder, then select **Project → Properties → Java Build Path → Libraries → Add JARs** and add `lib/sqlite-jdbc-3.46.1.3.jar`. Run `EmployeeSearchFrame` as a Java application.

On JDK 24 or newer, add `--enable-native-access=ALL-UNNAMED` immediately after `java` in the run commands to permit the SQLite driver's native library loading without a warning.

Enter **`company`**, **`company.db`**, or **`resources/company.db`**, then click **Fill** to load the included sample database. Other existing SQLite files can be opened by relative or absolute path. Bare filenames are checked in the working directory first, then in `resources/` (adding `.db` when needed). The database is opened read-only; an incorrect filename does not create a new database.

The original `resources/buildCOMPANYdb.sql` script is retained. Its final eight `ALTER TABLE ... ADD CONSTRAINT` statements are not supported by SQLite; use the included `resources/company.db` to run the application without rebuilding it.

## Search behavior

- **Fill** replaces the department and project lists with current database data, clears employee results and selections, and turns both Not checkboxes off. Missing files, invalid files, and incompatible schemas produce an alert and leave the old data cleared.
- Select multiple departments or projects with Ctrl-click (Command-click on macOS) or Shift-click. All three data areas scroll, and the window can be resized.
- Within each list, any selected item matches. For example, selecting departments 1 and 5 includes employees from either department.
- Department and project conditions combine with **AND**. Selecting department 5 and projects 1 and 2 means employees in department 5 who work on project 1 or 2. Project membership comes from `WORKS_ON`, not the department that controls the project.
- A **Not** checkbox excludes employees matching any selected item in its list. Project Not uses `NOT EXISTS`, so an employee working on both a selected project and another project is excluded. Employees with no projects can match a Not-project query. Employees with no department can match a Not-department query.
- An empty selection leaves that category unrestricted, even if its Not checkbox is checked. With neither list selected, Search returns all employees.
- **Search** replaces the employee results, sorted by last name, first name, and SSN. Multiple matching projects do not repeat an employee; separate employees with identical names remain separate rows. A status message reports zero matches.
- **Clear** empties employee results, clears selections in both lists, and unchecks both Not boxes. It keeps the loaded departments, projects, and database filename.
- Keyboard shortcuts: **Alt+F** runs Fill, **Alt+S** runs Search, and **Alt+C** runs Clear (on macOS, use Control+Option with the letter). After a successful Fill, pressing Enter outside the Database box runs Search; Enter in the Database box still runs Fill.
- After editing the database filename, click Fill before searching. Database operations run in a background worker; controls are temporarily disabled while a request completes.

Names are displayed with department/project numbers to distinguish duplicate names. Queries bind those numeric IDs through prepared statements.

## Expected COMPANY schema

The database must contain these standard tables and columns (additional columns and tables are allowed):

| Table | Required columns |
| --- | --- |
| `DEPARTMENT` | `Dnumber`, `Dname` |
| `PROJECT` | `Pnumber`, `Pname` |
| `EMPLOYEE` | `Ssn`, `Fname`, `Minit`, `Lname`, `Dno` |
| `WORKS_ON` | `Essn`, `Pno` |

`EMPLOYEE.Dno` references `DEPARTMENT.Dnumber`; `WORKS_ON.Essn` references `EMPLOYEE.Ssn`; `WORKS_ON.Pno` references `PROJECT.Pnumber`.

## Verification

After compiling, run the integration checks (no JUnit required):

```sh
java -cp 'out:lib/sqlite-jdbc-3.46.1.3.jar' CompanyDatabaseTest
```

On Windows, use `"out;lib/sqlite-jdbc-3.46.1.3.jar"` for the classpath. The tests create and delete temporary databases. They verify multiple selection, filenames with surrounding spaces, all four combinations of the two Not boxes, combined filters, employees without assignments, duplicate project and employee names, result deduplication, catalog refresh, unchanged database contents, empty data, invalid files, missing schema, filenames with special characters, and prevention of accidental database creation.

For a manual GUI check, Fill your class database twice and confirm no duplicated entries. Select multiple items in each list, try both Not boxes, and check the results. Click Clear and confirm that employee results, selections, and checkboxes reset while department/project entries stay loaded. Resize the window and check scrolling with enough rows. Finally, Fill a nonexistent filename and confirm the alert and cleared lists.

## Tools used

The supplied template was created using **Eclipse WindowBuilder**, as described in the assignment. This implementation uses **hand-written Java Swing with GridBagLayout**, edited with **OpenAI Codex**. No additional GUI designer was used. JDBC provides database access through the Xerial SQLite driver. `javac` compiles the application, and the standalone Java integration test exercises real SQLite databases.

## Requirements checklist

| Requirement | Implementation |
| --- | --- |
| Group of three | Ashutosh Thapa, Isaaciyo, Shirshak18 |
| Document GUI tools | See Tools used above |
| Multiple departments and projects | Multiple-interval selection in both lists |
| Scrollable department, project, and employee areas | Each area is inside a JScrollPane |
| Fill resets data and alerts on failure | Current catalogs replace old entries; failures show a dialog |
| Clear resets results, selections, and Not boxes | Clear action resets all search state |
| Not filters | Department exclusion and project NOT EXISTS queries |

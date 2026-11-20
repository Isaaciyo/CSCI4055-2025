# COMPANY Employee Search

Java Swing implementation of Dr. Smith's database project, extended from the provided `EmployeeSearchFrame.java` template. The application opens an existing SQLite COMPANY database and searches employees by department and project.

## Setup and run

Install a JDK (Java 8 or newer) and download the [Xerial SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc). The commands below use version 3.54.1.0 and run from the project directory. The driver has already been downloaded into `lib/` in this workspace; JARs and database files are excluded from Git.

macOS / Linux:

```sh
mkdir -p lib
curl -fL https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar -o lib/sqlite-jdbc-3.54.1.0.jar
mkdir -p out
javac -d out EmployeeSearchFrame.java CompanyDatabase.java CompanyDatabaseTest.java
java -cp 'out:lib/*' EmployeeSearchFrame
```

Windows (after downloading the JAR into `lib`):

```bat
mkdir out
javac -d out EmployeeSearchFrame.java CompanyDatabase.java CompanyDatabaseTest.java
java -cp "out;lib/*" EmployeeSearchFrame
```

In Eclipse, add these Java files to a Java project's source folder, then select **Project → Properties → Java Build Path → Libraries → Add External JARs** and add the SQLite JDBC JAR to the classpath. Run `EmployeeSearchFrame` as a Java application.

On JDK 24 or newer, add `--enable-native-access=ALL-UNNAMED` immediately after `java` in the run commands to permit the SQLite driver's native library loading without a warning.

Enter the path to your existing SQLite database (for example, `company.db` or an absolute path), then click **Fill**. Relative paths start at the program's working directory. The database is opened read-only; an incorrect filename does not create a new database. A database file is not included in this repository.

## Search behavior

- **Fill** replaces the department and project lists with current database data, clears employee results and selections, and turns both Not checkboxes off. Missing files, invalid files, and incompatible schemas produce an alert and leave the old data cleared.
- Select multiple departments or projects with Ctrl-click (Command-click on macOS) or Shift-click. All three data areas scroll, and the window can be resized.
- Within each list, any selected item matches. For example, selecting departments 1 and 5 includes employees from either department.
- Department and project conditions combine with **AND**. Selecting department 5 and projects 1 and 2 means employees in department 5 who work on project 1 or 2. Project membership comes from `WORKS_ON`, not the department that controls the project.
- A **Not** checkbox excludes employees matching any selected item in its list. Project Not uses `NOT EXISTS`, so an employee working on both a selected project and another project is excluded. Employees with no projects can match a Not-project query. Employees with no department can match a Not-department query.
- An empty selection leaves that category unrestricted, even if its Not checkbox is checked. With neither list selected, Search returns all employees.
- **Search** replaces the employee results, sorted by last name, first name, and SSN. Multiple matching projects do not repeat an employee; separate employees with identical names remain separate rows. A status message reports zero matches.
- **Clear** empties employee results, clears selections in both lists, and unchecks both Not boxes. It keeps the loaded departments, projects, and database filename.
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
java -cp 'out:lib/*' CompanyDatabaseTest
```

On Windows, use `"out;lib/*"` for the classpath. The tests create and delete temporary databases. They verify multiple selection, all four combinations of the two Not boxes, combined filters, employees without assignments, duplicate project and employee names, result deduplication, catalog refresh, unchanged database contents, empty data, invalid files, missing schema, filenames with special characters, and prevention of accidental database creation.

For a manual GUI check, Fill your class database twice and confirm no duplicated entries. Select multiple items in each list, try both Not boxes, and check the results. Click Clear and confirm that employee results, selections, and checkboxes reset while department/project entries stay loaded. Resize the window and check scrolling with enough rows. Finally, Fill a nonexistent filename and confirm the alert and cleared lists.

## Tools used

The supplied template was created using **Eclipse WindowBuilder**, as described in the assignment. This implementation uses **hand-written Java Swing with GridBagLayout**, edited with **OpenAI Codex**. No additional GUI designer was used. JDBC provides database access through the Xerial SQLite driver. `javac` compiles the application, and the standalone Java integration test exercises real SQLite databases.

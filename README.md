# Employee Search: CSCI 4055 Database Project

A Java Swing program that loads the departments and projects from a database that uses the
COMPANY schema, then lists employees who are in (or **not** in) the selected departments
and/or working (or **not** working) on the selected projects.

Built from Dr. Smith's template: https://github.com/proflon/CSCI4055-2025

## Project structure
```
lib/sqlite-jdbc-3.46.1.3.jar      SQLite JDBC driver
resources/buildCOMPANYdb.sql      COMPANY schema + data
resources/company.db              Generated SQLite database
src/database/DatabaseManager.java Opens/closes the SQLite connection
src/query/EmployeeSearchQuery.java All SQL (load lists, search)
EmployeeSearchFrame.java          The GUI window (template from Dr. Smith)
```

## How to run
Requires Java 17+. Run from the project folder.

**macOS / Linux**
```
javac -cp "lib/*" -d bin EmployeeSearchFrame.java src/database/DatabaseManager.java src/query/EmployeeSearchQuery.java
java -cp "bin:lib/*" EmployeeSearchFrame
```

**Windows**
```
javac -cp "lib/*" -d bin EmployeeSearchFrame.java src\database\DatabaseManager.java src\query\EmployeeSearchQuery.java
java -cp "bin;lib/*" EmployeeSearchFrame
```

Type `company` in the Database box and press **Fill**.

To rebuild the database: delete `resources/company.db`, then run
`sqlite3 resources/company.db < resources/buildCOMPANYdb.sql`.
The last 8 `ALTER TABLE ... ADD CONSTRAINT` lines error in SQLite. That is expected; all data still loads.

## Requirements checklist
| # | Requirement | Status |
|---|---|---|
| 1 | Group of three | Ashutosh Thapa, Isaaciyo, Shirshak18 |
| 2 | Document GUI tools used | TODO |
| 3 | Select multiple departments and projects | TODO |
| 4 | Department, Project, Employee lists scrollable | TODO |
| 5 | Fill resets lists from the DB; alert if it can't be opened | Done |
| 6 | Clear empties employees, selections, and Not boxes | TODO |
| 7 | Not boxes list employees NOT in dept / NOT on project | TODO |

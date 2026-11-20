package src.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

/**
 * Finds and opens the SQLite database file.
 *
 * The user types a database name such as "company" and this class finds
 * resources/company.db. A name ending in ".db", or a relative/absolute path
 * to an existing file, is also accepted. A missing file is never created.
 */
public class DatabaseManager {

	private static final String FOLDER = "resources";
	private static Connection connection = null;
	private static String currentName = null;

	/**
	 * Turns what the user typed into the real path of an existing, readable database file.
	 * Bare names are checked in the working directory first, then in resources/ (adding .db).
	 */
	public static Path resolve(String dbName) throws SQLException {
		if (dbName == null || dbName.trim().isEmpty()) {
			throw new SQLException("Please type a database name first.");
		}
		try {
			Path file = Paths.get(dbName.trim());
			if (!Files.exists(file) && !file.isAbsolute() && file.getNameCount() == 1) {
				String name = file.toString();
				if (!name.toLowerCase(Locale.ROOT).endsWith(".db")) {
					name += ".db";
				}
				file = Paths.get(FOLDER, name);
			}
			file = file.toRealPath();
			if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
				throw new SQLException("The database must be a readable file.");
			}
			return file;
		} catch (IOException | InvalidPathException e) {
			throw new SQLException("Database file does not exist or cannot be read: " + dbName, e);
		}
	}

	/** Opens a new read-only connection to a file returned by resolve(). The caller closes it. */
	public static Connection connectReadOnly(Path file) throws SQLException {
		// URI encoding handles spaces, '?' and '#' in filenames.
		String url = "jdbc:sqlite:" + file.toUri().toASCIIString() + "?mode=ro";
		Properties options = new Properties();
		// SQLITE_OPEN_READONLY: also prevents creating the file if it was removed.
		options.setProperty("open_mode", "1");
		try {
			return DriverManager.getConnection(url, options);
		} catch (SQLException e) {
			if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
				throw new SQLException("SQLite JDBC driver is missing. Add the SQLite JDBC JAR to the runtime classpath.", e);
			}
			throw e;
		}
	}

	/** Opens one shared read-only connection (used by EmployeeSearchQuery). */
	public static Connection open(String dbName) throws SQLException {
		closeConnection();
		connection = connectReadOnly(resolve(dbName));
		currentName = dbName.trim();
		return connection;
	}

	public static Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			throw new SQLException("No database is open. Type a name and press Fill.");
		}
		return connection;
	}

	public static boolean isOpen() {
		try {
			return connection != null && !connection.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	public static String getCurrentName() {
		return currentName;
	}

	public static void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		connection = null;
		currentName = null;
	}
}

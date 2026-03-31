package org.example.project.admin

import org.example.project.db.createTables
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val ADMIN_DATABASE_DIRECTORY = ".agent-fantasy-store"
private const val ADMIN_DATABASE_FILE = "agent-fantasy-store.db"

fun adminDatabasePath(): Path = Paths.get(
    System.getProperty("user.home"),
    ADMIN_DATABASE_DIRECTORY,
    ADMIN_DATABASE_FILE
)

fun createAdminDatabase(path: Path = adminDatabasePath()): Database {
    val normalizedPath = path.toAbsolutePath().normalize()
    Files.createDirectories(normalizedPath.parent)

    val dataSource = SQLiteDataSource(SQLiteConfig()).apply {
        url = "jdbc:sqlite:${normalizedPath.toUri().toASCIIString()}"
    }

    return Database.connect(dataSource).createTables()
}

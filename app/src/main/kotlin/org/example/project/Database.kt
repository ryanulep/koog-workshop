package org.example.project

import org.example.project.db.createTables
import org.example.project.db.seedDemoDataIfEmpty
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.sql.DataSource

private const val DATABASE_DIRECTORY = ".agent-fantasy-store"
private const val DATABASE_FILE = "agent-fantasy-store.db"

fun adminDatabasePath(): Path =
    Paths.get(DATABASE_DIRECTORY, DATABASE_FILE)

fun createDatabase(dataSource: DataSource): Database =
    Database.connect(dataSource)
        .createTables()
        .seedDemoDataIfEmpty()

fun createDataSource(path: Path = adminDatabasePath()): SQLiteDataSource {
    val normalizedPath = path.toAbsolutePath().normalize()
    Files.createDirectories(normalizedPath.parent)

    return SQLiteDataSource(SQLiteConfig().apply {
        enforceForeignKeys(true)
    }).apply {
        url = "jdbc:sqlite:${normalizedPath.toUri().toASCIIString()}"
    }
}

package org.example.project.admin.data

import org.example.project.db.createTables
import org.example.project.db.seedAdminDemoDataIfEmpty
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.sql.DataSource
import kotlin.time.Clock

private const val ADMIN_DATABASE_DIRECTORY = ".agent-fantasy-store"
private const val ADMIN_DATABASE_FILE = "agent-fantasy-store.db"

fun adminDatabasePath(): Path =
    Paths.get(ADMIN_DATABASE_DIRECTORY, ADMIN_DATABASE_FILE)

fun createAdminDatabase(dataSource: DataSource, clock: Clock = Clock.System): Database {
    return Database.connect(dataSource)
        .createTables()
        .seedAdminDemoDataIfEmpty(clock)
}

fun createDataSource(path: Path = adminDatabasePath()): SQLiteDataSource {
    val normalizedPath = path.toAbsolutePath().normalize()
    Files.createDirectories(normalizedPath.parent)

    return SQLiteDataSource(SQLiteConfig().apply {
        enforceForeignKeys(true)
    }).apply {
        url = "jdbc:sqlite:${normalizedPath.toUri().toASCIIString()}"
    }
}

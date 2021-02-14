import com.github.mustachejava.DefaultMustacheFactory
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSet.toImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.io.Resources
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files;
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import spark.Spark
import spark.kotlin.Http
import spark.kotlin.ignite

val PORT = 8080
val FLAG_PROFILE_PATH = "firefox_profile_path"
val QUERY_INDEX = Resources.toString(Resources.getResource("index.sql"), StandardCharsets.UTF_8)

data class Bookmark(
    val id: Int,
    val title: String,
    val url: String,
    val dateAdded: String,
    val tags: ImmutableSet<String>)

data class Row(
    val id: Int,
    val title: String,
    val url: String,
    val dateAdded: String,
    val parentTitle: String)

fun main(args: Array<String>) {
    // Parse arguments
    val options = Options()
    options.addOption(
        "p",
        FLAG_PROFILE_PATH,
        true /* hasArg */,
        "Path to Firefox profile directory")
    val commandLine = DefaultParser().parse(options, args)
    val profilePath = Paths.get(commandLine.getOptionValue(FLAG_PROFILE_PATH)).resolve("places.sqlite")

    // Connect to database
    // Make a copy since the database will be locked if Firefox is still open.
    val copy = Paths.get("/tmp/places.sqlite")
    Files.copy(profilePath, copy, StandardCopyOption.REPLACE_EXISTING)
    val connection = DriverManager.getConnection("jdbc:sqlite:$copy")

    // Configure HTTP
    val http: Http = ignite()
    http.port(PORT)
    http.staticFiles.location("/public")
    Spark.exception(Exception::class.java) { e, _, _ ->
        e.printStackTrace()
    }

    // Index action
    http.get("/") {
        val template = DefaultMustacheFactory().compile("index.mustache")
        val writer = StringWriter()
        val rows = readRows(connection, request.queryParams("tag") ?: "")
        val bookmarks = rowsToBookmarks(rows)
        val params = object {
            val bookmarks = bookmarks
        }
        template.execute(writer, params)
    }

    println("Running at http://localhost:$PORT")
}

fun readRows(connection: Connection, tag: String): ImmutableSetMultimap<String, Row> {
    val rowsByUrlBuilder = ImmutableSetMultimap.builder<String, Row>()
    val statement = connection.prepareStatement(QUERY_INDEX)
    statement.setString(1, "%$tag%")
    statement.executeQuery().use {
        while (it.next()) {
            val url = it.getString("url")
            val id = it.getInt("id")
            val title = it.getString("title") ?: ""
            val dateAdded = formatTimestamp(it.getLong("dateAdded"))
            val parentTitle = it.getString("parentTitle")
            rowsByUrlBuilder.put(url, Row(id, title, url, dateAdded, parentTitle))
        }
    }
    return rowsByUrlBuilder.build()
}

fun rowsToBookmarks(rowsByUrl: ImmutableSetMultimap<String, Row>): ImmutableList<Bookmark> {
    val bookmarks = ImmutableList.builder<Bookmark>()
    for (url in rowsByUrl.keySet()) {
        val rows = rowsByUrl.get(url)
        val id = rows.stream().findAny().get().id
        val title = rows.stream()
            .map { it.title }
            .filter { !it.isEmpty() }
            .findFirst()
            .orElse("")
        val dateAdded = rows.stream().findAny().get().dateAdded
        val tags = rows.stream().map { it.parentTitle }.collect(toImmutableSet())
        bookmarks.add(Bookmark(id, title, url, dateAdded, tags))
    }
    return bookmarks.build()
}

val formatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withLocale(Locale.US)
    .withZone(ZoneId.systemDefault())
fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochSecond(timestamp)
    return formatter.format(instant)
}


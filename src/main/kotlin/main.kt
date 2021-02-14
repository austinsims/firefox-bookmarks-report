import com.github.mustachejava.DefaultMustacheFactory
import com.google.common.collect.ImmutableList
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
import spark.kotlin.Http
import spark.kotlin.ignite

val PORT = 8080
val FLAG_PROFILE_PATH = "firefox_profile_path"
val QUERY_INDEX = Resources.toString(Resources.getResource("index.sql"), StandardCharsets.UTF_8)

fun main(args: Array<String>) {
    try {
      run(args)
    } catch (e: Exception) {
        println(e.message)
        e.printStackTrace()
    }
}

fun run(args: Array<String>) {
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

    // Index action
    http.get("/") {
        val template = DefaultMustacheFactory().compile("index.mustache")
        val writer = StringWriter()
        val bookmarks = readBookmarks(connection)
        val params = object {
            val bookmarks = bookmarks
        }
        template.execute(writer, params)
    }

    println("Running at http://localhost:$PORT")
}

data class Bookmark(
    val id: Int,
    val title: String,
    val url: String,
    val dateAdded: String)

fun readBookmarks(connection: Connection): ImmutableList<Bookmark> {
    val bookmarks = ImmutableList.builder<Bookmark>()
    try {
      connection.createStatement().executeQuery(QUERY_INDEX).use {
          while (it.next()) {
              bookmarks.add(
                  Bookmark(
                      id = it.getInt("id"),
                      title = it.getString("title"),
                      url = it.getString("url"),
                      dateAdded = formatTimestamp(it.getLong("dateAdded")),
                  )
              )
          }
      }
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException(e)
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


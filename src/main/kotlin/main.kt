import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.Resources
import com.google.template.soy.SoyFileSet
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import spark.kotlin.Http
import spark.kotlin.ignite

val PORT = 8080
val FLAG_PROFILE_PATH = "firefox_profile_path"
val QUERY_INDEX = Resources.toString(Resources.getResource("index.sql"), StandardCharsets.UTF_8)

fun main(args: Array<String>) {
    // Parse arguments
    val options = Options()
    options.addOption("p", FLAG_PROFILE_PATH, true /* hasArg */, "Path to Firefox profile directory")
    val parser = DefaultParser()
    val commandLine = parser.parse(options, args)
    val profilePath = commandLine.getOptionValue(FLAG_PROFILE_PATH)
    println("profilePath: $profilePath")

    // Connect to database
    val connection = DriverManager.getConnection("jdbc:sqlite:$profilePath/places.sqlite")

    // Configure Soy
    val fileSet = SoyFileSet.builder().add(Resources.getResource("index.soy")).build()
    val tofu = fileSet.compileToTofu()

    // Configure HTTP
    val http: Http = ignite()
    http.port(PORT)
    http.get("/") {
        val urls = ImmutableList.builder<String>()
        connection.createStatement().executeQuery(QUERY_INDEX).use {
            while(it.next()) urls.add(it.getString("url"))
        }
        tofu
            .newRenderer("name.austinsims.bookmarks.index.render")
            .setData(ImmutableMap.builder<String, Any>()
                .put("urls", urls.build())
                .build())
            .renderHtml()
            .toString()
    }
    println("Running at http://localhost:$PORT")
}
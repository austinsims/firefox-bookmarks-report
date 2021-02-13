import com.google.common.io.Resources
import com.google.template.soy.SoyFileSet
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import spark.kotlin.Http
import spark.kotlin.ignite

val PORT = 8080
val FLAG_PROFILE_PATH = "firefox_profile_path"

fun main(args: Array<String>) {
    // Parse arguments
    val options = Options()
    options.addOption("p", FLAG_PROFILE_PATH, true /* hasArg */, "Path to Firefox profile directory")
    val parser = DefaultParser()
    val commandLine = parser.parse(options, args)
    val profilePath = commandLine.getOptionValue(FLAG_PROFILE_PATH)
    println("profilePath: $profilePath")

    // Configure Soy
    val fileSet = SoyFileSet.builder().add(Resources.getResource("index.soy")).build()
    val tofu = fileSet.compileToTofu()

    // Configure HTTP
    val http: Http = ignite()
    http.port(PORT)
    http.get("/") {
        tofu.newRenderer("name.austinsims.bookmarks.index.render").renderHtml().toString()
    }
    println("Running at http://localhost:$PORT")
}
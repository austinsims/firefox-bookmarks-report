import com.google.common.io.Resources
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu
import spark.kotlin.Http
import spark.kotlin.ignite

val PORT = 8080

fun main() {
    val fileSet = SoyFileSet.builder()
        .add(Resources.getResource("index.soy"))
        .build()
    val tofu = fileSet.compileToTofu()

    val http: Http = ignite()
    http.port(PORT)
    http.get("/") {
        tofu.newRenderer("name.austinsims.bookmarks.index.render").renderHtml().toString()
    }

    println("Running at http://localhost:$PORT")
}
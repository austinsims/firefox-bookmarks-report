import spark.kotlin.*

fun main() {
    val http: Http = ignite()
    http.get("/") {
        "Hello, world"
    }
}
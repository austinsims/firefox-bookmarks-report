import spark.kotlin.*

val PORT = 8080

fun main() {
    val http: Http = ignite()
    http.port(PORT)
    http.get("/") {
        "Hello, world"
    }
    println("Running at http://localhost:$PORT")
}
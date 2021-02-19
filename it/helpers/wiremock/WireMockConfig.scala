
package helpers.wiremock

trait WireMockConfig {
  val wireMockPort: Int    = 11111
  val wireMockHost: String = "localhost"
  val wireMockUrl: String  = s"http://$wireMockHost:$wireMockPort"
}
package scoperties

import com.github.shiverawe.{Property, Scoptions}

case class TestOptions() extends Scoptions {
  val host: Property = Property("host", "localhost")
  val port: Property = Property("port", "7345")
  val mode: Property = Property("mode", "production")
}
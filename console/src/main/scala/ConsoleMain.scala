object ConsoleMain {
  def main(args: Array[String]): Unit = {
    ammonite.Main(
      predef =
        """
          |println("Loading the VictorOps Example Console!")
          |import akka.actor._
          |import akka.pattern._
          |import victorops.example.console._
          |initializeConsole()
        """.stripMargin
    ).run()
  }
}

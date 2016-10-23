object ConsoleMain {
  def main(args: Array[String]): Unit = {
    ammonite.Main(
      predef =
        """
          |println("Loading the VictorOps Example Console!")
          |import victorops.example.console._
        """.stripMargin
    ).run()
  }
}

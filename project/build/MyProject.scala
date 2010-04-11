import sbt._

class MyProject(info: ProjectInfo) extends DefaultProject(info) {
  val repo = "Java.net Repository" at "http://download.java.net/maven/2/"
  val javamail = "javax.mail" % "mail" % "1.4.2"
  val scalaTest = "org.scalatest" % "scalatest" % "1.0"
  val sqlite = "org.xerial" % "sqlite-jdbc" % "3.6.20"
  override def compileOptions = super.compileOptions ++
     Seq("-encoding", "utf8").map(x => CompileOption(x))
}

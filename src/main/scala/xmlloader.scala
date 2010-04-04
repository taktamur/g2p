package taktamur
import scala.xml._
import scala.xml.parsing.{ConstructingParser,XhtmlParser}
import scala.io.Source

object implicitConvert{
  implicit def xmlloader(s:String) = new XMLLoader(s)
}

class XMLLoader(url:String) {
  def xml():Elem = xml("UTF-8")
  def xml(enc:String):Elem = {
    XML.loadString( Source.fromURL(url,"UTF-8").getLines.mkString)
  }
}


package taktamur
import scala.io.Source
import scala.xml.XML
import scala.xml.parsing.{ConstructingParser,XhtmlParser}
import java.util.Properties
import java.io._
import java.util.Date
import taktamur.implicitConvert.xmlloader;   //"".xml の暗黙の型変換

class Posterous(p:Properties){
  import java.net.{Authenticator,  PasswordAuthentication}
  val user = p.getProperty("posterous.auth.usermail")
  val pass = p.getProperty("posterous.auth.password")
  Authenticator.setDefault(
    new Authenticator {
      override def getPasswordAuthentication =
        new PasswordAuthentication( user, pass.toCharArray)
      }
    )
  val site=  "http://posterous.com/api/getsites".xml

  def siteID():String = (site \\ "id").toList.first.text

  val posts = ("http://posterous.com/api/readposts?num_posts=50&site_id=" + siteID).xml
  val titles:List[String] = (posts \\ "post" \ "title").toList.map(t => t.text)
}


class GoogleReader(u:String){
  val url:String = u
  def getAlternateURL():String = {
    val dt = """.*<link rel="alternate" type=".*" href="(.*?)" title.*""".r
    val lines = Source.fromURL(url).getLines.mkString
    val t = dt.findFirstIn(lines)
    val dt(rss) = t match{ case Some(x)=>x; case _ => "" }
    rss
  }
  def getRSSFeed():RSSFeed = {
    new RSSFeed( getAlternateURL() )
  }
}

class RSSFeed(u:String){
  val url:String = u;
  def getEntries():List[Entry] = {
    val elem = url.xml
    (elem \\ "feed" \ "entry").toList.map(e => new Entry(e.toString))
  }
}

class Entry(v:String){
  val e = XML.loadString(v)
  def title():String = {
    val entry_title = (e \ "title").first.text;
    val blog_title = (e \ "source" \ "title").first.text;
    val titles = entry_title + " - " + blog_title
    titles.replace("&#39;","'")   // 実体参照のすげぇ手抜き解決 切込隊長blog専用 fixme
  }
  def annotation():String = (e \\ "annotation" \ "content").text
  def link():String ={
    val ret = (e \ "link").toList
    ret.filter(_.attribute("rel") match{  // Nodeのパターンマッチで簡単に出来るはず
      case Some(x) => (x.text == "alternate")
      case _ => false
    }).map( _.attribute("href") ).first match{
      case Some(x) => x.text
      case _ => ""
    }
  }
  import java.text.SimpleDateFormat
  import java.util.TimeZone
  val s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  s.setTimeZone(TimeZone.getTimeZone("GMT"))
  def published():Date = s.parse(( e \ "published").first.text)

  def firstImageTag():String = {
    val dt = """.*(<img .*?>).*""".r
    val contents = (e \\ "content").text
    val t = dt.findFirstIn(contents)
    if( t == None ){
      ""
    }else{
      val dt(img) = t match {case Some(x)=>x;  case _ => ""}
      img
    }
  }
  def id():String = (e \ "id").first.text;
}

// fixme 使いにくいクラス
trait Sender{
  def send(to:String, subject:String, body:String);
}

object Mailler{
  def build(p:Properties):Sender ={
    new GMailler(p)
  }

  class NullMailler() extends Sender{
    def send(to:String,subject:String,body:String):Unit ={
      println("sending mail. subject="+subject)
    }
  }
class GMailler(p:Properties) extends Sender{
  val mail = new GMail(p)

  def send(to:String,subject:String,body:String):Unit = {
    println("sending mail. subject="+subject)
    mail.send( to,subject,body)
  }
}
}

// fixme imgもあればそれを追加する
class Convert(template:String){
  def toPosterous(e:Entry):String = {
    template.replace("##BQ##",e.annotation()).
      replace("##URL##",e.link()).
      replace("##TITLE##",e.title()).
      replace("##FIRSTIMG##",e.firstImageTag())
  }
}


object main extends Application{
  val p = new Properties()
  p.load(new FileInputStream("./g2p.properties") )
  val pl = new PostLog4SQLite()
  val g = new GoogleReader(p.getProperty("googlereader.url"))
  val now = new Date()
  val posts:List[Entry] = g.getRSSFeed().getEntries()
    .filter(p => pl.isNotPosted(p.id) )
    .filter(_.annotation().length()!=0)
    .filter(_.published.getTime() > now.getTime()-60*60*1000)
println("send posts. count="+posts.size)
  val c = new Convert("""<div class="posterous_bookmarklet_entry"><blockquote class="posterous_long_quote">##FIRSTIMG##<br/> ##BQ##</blockquote><div class="posterous_quote_citation">via <a href="##URL##">##TITLE##</a> share from <a href="http://www.google.com/reader/shared/taktamur"my google reader</a></div></div>""")
  val mailler = Mailler.build(p)
  val postto = p.getProperty("posterous.post.mailto")
  posts.foreach( post => {
    mailler.send(postto, post.title, c.toPosterous(post))
    pl.addId(post.id)
  });
}

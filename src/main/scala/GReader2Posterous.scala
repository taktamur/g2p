package taktamur
import scala.io.Source
import scala.xml.XML
import scala.xml.parsing.{ConstructingParser,XhtmlParser}
import java.util.Properties
import java.io._
import java.util.Date

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
  val site=  XML.loadString( Source.fromURL("http://posterous.com/api/getsites","UTF-8").getLines.mkString)

  def siteID():String ={
    (site \\ "id").toList.first.text
  }

  def titles():List[String] ={
    val url = "http://posterous.com/api/readposts?num_posts=50&site_id=" + siteID
    val site = XML.loadString( Source.fromURL(url,"UTF-8").getLines.mkString)
    val titles:List[String] = (site \\ "post" \ "title").toList.map(t => t.text)
    titles
  }    
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
    val src = scala.io.Source.fromURL(url,"UTF-8").getLines.mkString
    val elem = XML.loadString(src)
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
class PostLog(p:Properties){
  val posterous = new Posterous(p)
  val posted:List[String] = posterous.titles

  def isNonPosted(t:String):Boolean = !posted.contains(t)
}

// fixme imgもあればそれを追加する
object Convert{
  def toPosterous(e:Entry):String = {
    """<div class="posterous_bookmarklet_entry">
<blockquote class="posterous_long_quote">##BQ##</blockquote>
<div class="posterous_quote_citation">via <a href="##URL##">##TITLE##</a></div>
</div>""".replace("##BQ##",e.annotation()).replace("##URL##",e.link()).replace("##TITLE##",e.title())

  }
}

// fixme GoogleReaderの日付に合わせる
object main extends Application{
  val p = new Properties()
  p.load(new FileInputStream("./g2p.properties") )
  val pl = new PostLog(p)
  val g = new GoogleReader(p.getProperty("googlereader.url"))
  val now = new Date()
  val posts:List[Entry] = g.getRSSFeed().getEntries()
    .filter(p => pl.isNonPosted(p.title) )
    .filter(_.annotation().length()!=0)
    .filter(_.published.getTime() > now.getTime()-60*60*1000)
println("send posts. count="+posts.size)
  val mailler = Mailler.build(p)
  val postto = p.getProperty("posterous.post.mailto")
  posts.foreach( post => {
    mailler.send(postto, post.title, Convert.toPosterous(post))
  });
}

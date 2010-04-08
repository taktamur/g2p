import taktamur._
import java.util.Properties
import java.io._
import java.util.Date
import org.scalatest.FunSuite

class G2PSuite extends FunSuite { 
    val prop = new Properties()
    prop.load(new FileInputStream("./g2p.properties") )


    val e = new Entry("""<entry gr:crawl-timestamp-msec="1269600301091"><id gr:original-id="">tag:google.com,2005:reader/item/ceddcb3a4a4984a2</id><title type="html">読者の“目”が制御するeブックリーダーText 2.0, 狂気かそれとも天才の作品か？</title><published>2010-03-26T10:45:01Z</published><updated>2010-03-26T10:45:01Z</updated><link rel="alternate" href="http://www.pheedo.jp/click.phdo?i=52aa9ccc91a00dfb9d2ecb7ea8e69b48" type="text/html"/><link rel="related" href="http://jp.techcrunch.com" title="TechCrunch Japan"/><content xml:base="http://www.pheedo.jp/click.phdo?i=52aa9ccc91a00dfb9d2ecb7ea8e69b48" type="html">&lt;blockquote&gt; taktamur  さんと共有
&lt;br&gt;
このシステムは〜〜〜</content><author gr:unknown-author="true"><name>(投稿者不明)</name></author><gr:annotation><content type="html">このシステムは(HTML部分)</content><author gr:user-id="02442664282021633674" gr:profile-id="116046324767560132096"><name>taktamur</name></author></gr:annotation><source gr:stream-id="user/02442664282021633674/source/com.google/link"><id>tag:google.com,2005:reader/user/02442664282021633674/source/com.google/link</id><title type="html">TechCrunch Japan</title><link rel="alternate" href="http://jp.techcrunch.com" type="text/html"/></source></entry>""")
  test("GoogleReader test."){
    val g = new GoogleReader("http://www.google.com/reader/shared/taktamur")
    assert( g.url == "http://www.google.com/reader/shared/taktamur" )
    assert( g.getAlternateURL() == "http://www.google.com/reader/public/atom/user%2F02442664282021633674%2Fstate%2Fcom.google%2Fbroadcast", "g.getAlternateURL() failed")
  }
  test("RSSFeed test."){
    val r = new RSSFeed("http://www.google.com/reader/public/atom/user%2F02442664282021633674%2Fstate%2Fcom.google%2Fbroadcast")
    assert(r.getEntries().length == 20,"entry size failed")
    val now = new Date()
    r.getEntries.filter(_.published.getTime() > now.getTime()-60*60*1000).map(e=> println(e.published.toString) )
    
    true;
  }
  test("RSSFeed test2"){
    val r = new RSSFeed("http://www.google.com/reader/public/atom/user%2F02442664282021633674%2Fstate%2Fcom.google%2Fbroadcast")
    r.getEntries.foreach( e => println(e.firstImageTag() ))
    true
  }
  test("Entry test."){

    assert(e.title == "読者の“目”が制御するeブックリーダーText 2.0, 狂気かそれとも天才の作品か？ - TechCrunch Japan","title test failed.title="+e.title)
    assert(e.annotation == "このシステムは(HTML部分)")
    assert(e.link() == "http://www.pheedo.jp/click.phdo?i=52aa9ccc91a00dfb9d2ecb7ea8e69b48")
  }
  test("convert2Posteous test."){
    val ok = """<div class="posterous_bookmarklet_entry"><blockquote class="posterous_long_quote">このシステムは(HTML部分)</blockquote><div class="posterous_quote_citation">via <a href="http://www.pheedo.jp/click.phdo?i=52aa9ccc91a00dfb9d2ecb7ea8e69b48">読者の“目”が制御するeブックリーダーText 2.0, 狂気かそれとも天才の作品か？ - TechCrunch Japan</a></div></div>"""
    val c1 =(new Convert("##BQ##")) 
    assert( c1.toPosterous(e) == "このシステムは(HTML部分)", "BQ replace")
    assert( (new Convert("##URL##")).toPosterous(e) == "http://www.pheedo.jp/click.phdo?i=52aa9ccc91a00dfb9d2ecb7ea8e69b48", "URL replace")
    assert( (new Convert("##TITLE##")).toPosterous(e) == """読者の“目”が制御するeブックリーダーText 2.0, 狂気かそれとも天才の作品か？ - TechCrunch Japan""", "title replace")

  val c = new Convert("""<div class="posterous_bookmarklet_entry"><blockquote class="posterous_long_quote">##BQ##</blockquote><div class="posterous_quote_citation">via <a href="##URL##">##TITLE##</a></div></div>""")
    assert( ok == c.toPosterous(e),"ok result="+c.toPosterous(e) )
  }

  test("1 post to gmail"){
    val mailler = Mailler.build(prop)
    println("mailler class is " + mailler )
//    mailler.send("taktamur@gmail.com","Mail post test.","Mail body here.")
    true
  } 
  test("postlog test."){
    val log = new PostLog(prop)
    assert(log.isNonPosted("test") == false, "test is exist")
    assert(log.isNonPosted("testhoge") == true, "testhoge is not exist")
    true
  }
  test("Posterous test."){
    val p = new Posterous(prop)
    assert(p.siteID == "911419","posterous id failed.")
    assert(p.titles.size == 50, "posterous title count==50 failed")
//p.titles.foreach(println)
    true
  }

  test("googlereaderの1時間フィルタテスト"){
    true
  }

  test("日付テスト"){
    import java.text.SimpleDateFormat
    import java.util.TimeZone
    val s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    s.setTimeZone(TimeZone.getTimeZone("GMT"))
    val d = s.parse("2010-03-26T10:45:01Z")

    assert(d == e.published, "date time test.d="+d+" e.published="+e.published)
    // 1時間前を検出
    val dates:List[Date] = List(s.parse("2010-03-26T10:45:01Z"),
				s.parse("2010-03-26T10:55:01Z"),
				s.parse("2010-03-26T11:05:01Z"),
				s.parse("2010-03-26T11:35:01Z"),
				s.parse("2010-03-26T11:45:01Z"),
				s.parse("2010-03-26T11:55:01Z"))
    val now = s.parse("2010-03-26T12:00:00Z")
    dates.filter(_.getTime() > now.getTime()-60*60*1000).foreach(println)
    assert(dates.filter(_.getTime() > now.getTime()-60*60*1000).size == 4, "１時間前の検出")
    true
  }

  test("画像テスト"){
    true
  }


}

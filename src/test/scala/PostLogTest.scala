import taktamur._
import taktamur.PostLog
import org.scalatest.FunSuite

class PostLogTestSuite extends FunSuite { 
  test(""){
    val pl = new PostLog4SQLite()
    pl.addId("hoge")
    assert(pl.isNotPosted("hoge1") == true, "isNotPosted1")
    assert(pl.isNotPosted("hoge") == false, "isNotPosted2")
  }
}

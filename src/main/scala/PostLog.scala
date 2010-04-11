package taktamur
import java.util.Properties

class PostLog(p:Properties){
  val posterous = new Posterous(p)
  val posted:List[String] = posterous.titles

  def isNonPosted(t:String):Boolean = !posted.contains(t)
}

class PostLog4SQLite(){
  import java.sql._
  // sbt ~testしてると２度目からwarningが出るけど気にしない
  Class.forName("org.sqlite.JDBC");

  val conn:Connection = DriverManager.getConnection("jdbc:sqlite:postlog.db");
  val stat:Statement  = conn.createStatement();
  stat.executeUpdate("create table if not exists postlog (id) ;");

  val select:PreparedStatement =
    conn.prepareStatement("select count(*) as c from postlog where id=?")
  val update:PreparedStatement =
    conn.prepareStatement("insert into postlog(id) values(?);")


  def isNotPosted(id:String):Boolean ={
    select.setString(1,id)
    val rs:ResultSet = select.executeQuery()
    rs.next()
    val count:Int = rs.getInt("c")
    rs.close()
    if( count == 0 ){
      true
    }else{
      false
    }
  }

  def addId(id:String):Unit = {
    update.setString(1,id)
    update.executeUpdate()
  }

}

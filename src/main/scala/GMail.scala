package taktamur

import java.util.Properties
import javax.mail._
import javax.mail.internet._

// ref.
//  http://www.utilz.jp/wiki/JavaMail1

class GMail(p:Properties){
  val user = p.getProperty("mailer.gmail.user")
  val password = p.getProperty("mailer.gmail.password")
  val PROP = new Properties()
  PROP.put("mail.smtp.host", p.getProperty("mailer.gmail.smtp.host") )
  PROP.put("mail.smtp.port", p.getProperty("mailer.gmail.smtp.port"))
  PROP.put("mail.smtp.auth", p.getProperty("mailer.gmail.smtp.auth"));
  PROP.put("mail.smtp.starttls.enable", p.getProperty("mailer.gmail.smtp.starttls.enable"));
  val sess:Session = Session.getInstance(PROP);
  val transport = sess.getTransport("smtp")
  transport.connect(user, password);
  
  def send( to:String,
           subject:String,
           body:String ) ={
    val mm:MimeMessage = new MimeMessage(sess);
    mm.setFrom(new InternetAddress(user));
    mm.setSubject(subject);
    mm.setRecipient(
      Message.RecipientType.TO, new InternetAddress(to));
    mm.setContent(body, "text/plain; charset=iso-2022-jp");
    mm.setHeader("Content-Transfer-Encoding", "7bit");

    transport.sendMessage(mm, mm.getAllRecipients());
  }  

  override def finalize ={
    transport.close()
  }
}

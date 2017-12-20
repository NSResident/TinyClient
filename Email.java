package mailProject;

import javafx.beans.property.SimpleStringProperty;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Email{

    private SimpleStringProperty subject = new SimpleStringProperty("");
    private SimpleStringProperty sender = new SimpleStringProperty("");
    private SimpleStringProperty date = new SimpleStringProperty("");
    private SimpleStringProperty body = new SimpleStringProperty("");
    private SimpleStringProperty receiver = new SimpleStringProperty("");
    private MimeMessage msg;

    public Email(Email mail){
        setSender(mail.getSender());
        setReceiver(mail.getReceiver());
        setSubject(mail.getSubject());
        setDate(mail.getDate());
        setBody(mail.getBody());
    }

    public Email(){
        this("","", "", "","");
    }
    
    public Email(String sender, String receiver, String subject, String date, String body){
        setSubject(subject);
        setSender(sender);
        setDate(date);
        setBody(body);
        setReceiver(receiver);
    }
    
    public Email(Message msg){
        super();
        try {
            this.subject = new SimpleStringProperty(msg.getSubject());
            this.sender  = new SimpleStringProperty(Arrays.asList(msg.getFrom()).stream().map(addr -> addr.toString()).collect(Collectors.joining()));
            DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
            this.date = new SimpleStringProperty(df.format(msg.getReceivedDate()));
//            try {
//                text = (new MimeMessageParser((MimeMessage)msg)).parse().getPlainContent();
//            } catch (Exception ex) {
//                Logger.getLogger(Email.class.getName()).log(Level.SEVERE, null, ex);
//            }
            this.msg = (MimeMessage)msg;
            this.body = new SimpleStringProperty("");
            this.receiver = new SimpleStringProperty("");
        } catch (MessagingException ex) {
            Logger.getLogger(Email.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public MimeMessage getMsg() {
        return msg;
    }

    public void setMsg(MimeMessage msg) {
        this.msg = msg;
    }

    public String toString(){
        return getSubject() + " " + getSender();
    }

    public String getReceiver() {
        return receiver.get();
    }

    public void setReceiver(String receiver) {
        this.receiver.set(receiver);
    }
    
    
    public String getSubject() {
        return subject.get();
    }

    public final void setSubject(String subject) {
        this.subject.set(subject);
    }

    public String getSender() {
        return sender.get();
    }

    public final void setSender(String sender) {
        this.sender.set(sender);
    }

    public String getDate() {
        return date.get();
    }

    public final void setDate(String date) {
        this.date.set(date);
    }

   public String getBody() {
       return body.get();
   }

   public final void setBody(String body) {
       this.body.set(body);
   }

}
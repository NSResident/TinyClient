/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailProject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.stream.Collectors;

public class MailClientModel {
    
    private User loggedInUser = null;
    private ArrayList<User> users = new ArrayList<User>();
    private Session session;
    private final String host = "smtp.gmail.com";
    private Store store;
    private Folder inboxFolder = null;
    private Folder starred;
    private Folder sentMail;
    private int previousFolderCount;
    private ObservableList<Email> unfilteredList = FXCollections.observableArrayList();
    private boolean prevLoggedIn = false;
    public int currentCount = 0;
    private boolean loggedOut;

    public void initialize() throws MessagingException {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "587");
        properties.setProperty("mail.smtp.user", loggedInUser.getAddr());
        properties.setProperty("mail.smtp.user", loggedInUser.getPassword());
        this.session = Session.getInstance(properties, new javax.mail.Authenticator(){ 
            protected PasswordAuthentication getAuth(){return new PasswordAuthentication(loggedInUser.getAddr(), loggedInUser.getPassword());}});
        
        this.store = session.getStore("imaps");
        this.store.connect(host, loggedInUser.getAddr().toString(), loggedInUser.getPassword());
            //Check if folder exists
        this.inboxFolder = this.store.getFolder("INBOX");
        this.inboxFolder.open(Folder.READ_WRITE);
        this.currentCount = this.inboxFolder.getMessageCount();
        this.previousFolderCount = this.inboxFolder.getMessageCount();
    }

    public List<Email> msgToEmai(int low, int high) throws MessagingException{
        List<Email> result = Arrays.asList(this.inboxFolder.getMessages(low,high)).stream().map(msg -> new Email(msg)).collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    public void sendEmail(Email emailToSend) throws MessagingException{
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "587");
        Session session1 = Session.getInstance(properties, new javax.mail.Authenticator(){ 
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(loggedInUser.getAddr(), loggedInUser.getPassword());}});
        
        System.out.println(loggedInUser.getAddr() + " " + loggedInUser.getPassword());
        Message msg = new MimeMessage(session1);
        msg.setFrom(new InternetAddress(this.loggedInUser.getAddr()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailToSend.getReceiver()));
        msg.setSubject(emailToSend.getSubject());
        msg.setText(emailToSend.getBody());
        Transport.send(msg);
    }

    public boolean isPrevLoggedIn() {
        return prevLoggedIn;
    }

    public void setPrevLoggedIn(boolean prevLoggedIn) {
        this.prevLoggedIn = prevLoggedIn;
    }

    public ObservableList<Email> getUnfilteredList() {
        return unfilteredList;
    }

    public void setUnfilteredList(ObservableList<Email> unfilteredList) {
        this.unfilteredList = unfilteredList;
    }

    public int getPreviousFolderCount() {
        return previousFolderCount;
    }

    public void setPreviousFolderCount(int previousFolderCount) {
        this.previousFolderCount = previousFolderCount;
    }

    public Folder getInboxFolder() {
        return inboxFolder;
    }

    public void setInboxFolder(Folder inboxFolder) {
        this.inboxFolder = inboxFolder;
    }
    
    
    public Session getSession() {
        return session;
    }

    public String getHost() {
        return host;
    }

    public Store getStore() {
        return store;
    }

    public void addUser(User user){
        users.add(user);
    }
    
    public void removeUser(User user){
        users.remove(user);
    }

    
    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public void setLoggedOut(boolean loggedOut) {
        this.loggedOut = loggedOut;
    }

    public boolean ifLoggedOut() {
        return loggedOut;
    }
}
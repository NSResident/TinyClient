/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailProject;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.mail.util.MimeMessageParser;

import java.lang.Thread;
import java.util.*;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author ryan
 */
public class MailClientController {
    final static int MAX_AMOUNT_OF_ACCOUNTS = 6;
    Semaphore semaphore = new Semaphore(MAX_AMOUNT_OF_ACCOUNTS);
    Map<String, MailClientModel> users = new LinkedHashMap<>();
    static User currentUser;
    static boolean accSwitch = false;
    static boolean fromSerializedObject = false;
    public Object loggedInUser;
    private MailClientModel currentClient;
    private boolean clientClosed = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }

    public void compose(Email email) {
        if (currentClient.getLoggedInUser() == null) {
            return;
        }
        try {
            currentClient.sendEmail(email);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    public void login(User newUser) {
        if (accSwitch) {
            accSwitch = false;
            if(fromSerializedObject)
                currentClient.setLoggedInUser(currentUser);
            try {
                if (currentClient.getInboxFolder() == null)
                    currentClient.initialize();

                if (users.get(currentClient.getLoggedInUser().getAddr()) == null) {
                    users.put(currentClient.getLoggedInUser().getAddr(), currentClient);
                }
                Runnable inboxRunner = new UpdateInboxWorker();
                Thread t1 = new Thread(inboxRunner);
                t1.start();
            } catch (MessagingException ex) {
                Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        MailClientModel newClient = new MailClientModel();
        try{
            if(users.get(newUser.getAddr()) != null)
                throw new Exception();
            newClient.setLoggedInUser(newUser);
            newClient.initialize();
            currentClient = newClient;
            users.put(currentClient.getLoggedInUser().getAddr(), currentClient);
            Runnable inboxRunner = new UpdateInboxWorker();
            Thread t1 = new Thread(inboxRunner);
            t1.start();
            } catch (MessagingException ex) {
                Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Please check to make sure email credentials are correct and that your email"
                        + " is set to allow 3rd part applications to access them.");
                currentClient.setLoggedInUser(null);
            }
            catch (Exception e) {
                System.out.println("User is already signed into the currentClient");
            }
        ;
    }


    public void logout() {
        System.out.println(currentClient.getLoggedInUser().getAddr());
        users.remove(currentClient.getLoggedInUser().getAddr());
        currentClient.setLoggedOut(true);
        System.out.println(users.size());
        if (users.size() > 0) {
            Map.Entry<String, MailClientModel> temp = users.entrySet().iterator().next();
//        currentClient.setLoggedInUser(null);
            currentClient = temp.getValue();
            currentClient.setPrevLoggedIn(true);
            accSwitch = true;
        } else {
            currentClient = null;
            accSwitch = false;
        }
//        Iterator it = users.entrySet().iterator();
//        System.out.println("users " + users.size());
//        List<String> listOfUsers = new ArrayList<>();
//        while (it.hasNext()) {
//            listOfUsers.add(((Map.Entry<String, MailClientModel>) (it.next())).getKey());
//        }
//        login();
//        return listOfUsers;
    }


    public void openEmail(Email email) {
        //OPEN POP FOR EMAIL
        if (!currentClient.getInboxFolder().isOpen()) {
            try {
                currentClient.getInboxFolder().open(Folder.READ_ONLY);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        String subject = new String(email.getSubject());
        String body = "";
        try {
            body = (new MimeMessageParser(email.getMsg()).parse().getPlainContent());
        } catch (FolderClosedException ex) {
            ex.printStackTrace();
//            ConnectPop3(Username, Password);
        } catch (Exception ex) {
            Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void switchUser(String addr) {
        if (addr == currentClient.getLoggedInUser().getAddr()) {
            System.out.println("SAME ADDR");
            return;
        }
        accSwitch = true;
        System.out.println("DIFF ADDR");
        currentClient = users.get(addr);
        currentClient.setPrevLoggedIn(true);
    }


    public MailClientModel getCurrentClient() {
        return currentClient;
    }

    public void setClientClosed(boolean clientClosed) {
        this.clientClosed = clientClosed;
    }

    class NewMailWorker implements Runnable {

        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000);
                    if (currentClient == null) {
                        System.out.println("ENDING MAIL WORKER");
                        return;
                    }
                    while (semaphore.availablePermits() != MAX_AMOUNT_OF_ACCOUNTS)
                        Thread.sleep(20000);
                    if (currentClient.getLoggedInUser() == null) {
                        continue;
                    }
                    Folder inbox = currentClient.getStore().getFolder("INBOX");
                    inbox.open(Folder.READ_WRITE);
                    if (inbox.getMessageCount() > currentClient.getPreviousFolderCount()) {
                        List<Email> emails = Arrays.asList(inbox.getMessages(currentClient.getPreviousFolderCount() + 1, inbox.getMessageCount()))
                                .stream().map(msg -> new Email(msg)).collect(Collectors.toList());
//                        ObservableList<Email> fxInbox = tableView.getItems();
                        for (int i = 0; i < emails.size(); i++)
//                            fxInbox.add(0, emails.get(i));
                        currentClient.setInboxFolder(inbox);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MessagingException ex) {
                    Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                }
            }
        }
    }

    class UpdateInboxWorker implements Runnable {

        @Override
        public void run() {
            try {
                semaphore.acquire();
                MailClientModel threadClient = currentClient;
                accSwitch = false;
                System.out.println("NEW MAIL WORKER " + accSwitch);
//                ObservableList<Email> fxInbox = tableView.getItems();
//                fxInbox.clear();
                int count = threadClient.currentCount;
//                int count = currentClient.getInboxFolder().getMessageCount();
                int step = count < 1000 ? count - 1 : count / 1000;
                while (count > 1) {
                    if (threadClient.ifLoggedOut()) {
                        accSwitch = false;
//                        fxInbox.clear();
                        break;
                    }
                    threadClient.getUnfilteredList().addAll(threadClient.msgToEmai(count - step, count));
                    System.out.println(count);
                    count -= step;
                    threadClient.currentCount = count;

                }
                System.out.println("BEGINNING LISTENER");
                while(!clientClosed || !threadClient.ifLoggedOut()) {
                    Thread.sleep(3000);
                    System.out.println("LISTENING");
                    if(!threadClient.getInboxFolder().isOpen()){
                        threadClient.getInboxFolder().open(Folder.READ_ONLY);
                    }
                    Folder inbox = threadClient.getInboxFolder();
                    if (inbox.getMessageCount() > threadClient.getPreviousFolderCount()) {
                        System.out.println("FOUND");
                        List<Email> emails = Arrays.asList(inbox.getMessages(threadClient.getPreviousFolderCount() + 1, inbox.getMessageCount()))
                                .stream().map(msg -> new Email(msg)).collect(Collectors.toList());
                        for (int i = 0; i < emails.size(); i++)
                            threadClient.getUnfilteredList().add(0,emails.get(i));
                        threadClient.setInboxFolder(inbox);
                        threadClient.setPreviousFolderCount(inbox.getMessageCount());
                    }
                }
            } catch (MessagingException ex) {
                Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }

}

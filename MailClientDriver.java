package mailProject;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
public class MailClientDriver extends Application {
    static MailClientController controller = new MailClientController();
    static boolean accSwitch = false;
    static boolean fromSerializedObject = false;
    static User lastUser = null;

    @FXML
    Label EmailOfUser;
    @FXML
    ScrollPane EmailViewer;
    @FXML
    Label ViewerSubject;
    @FXML
    Label ViewerBody;
    @FXML
    TableView<Email> tableView;
    @FXML
    TableColumn<Email, Boolean> Checked;
    @FXML
    TableColumn<Email, String> Subject;
    @FXML
    TableColumn<Email, String> Sender;
    @FXML
    TableColumn<Email, String> Date;
    @FXML
    TextField Query;
    @FXML
    ComboBox<String> Users;
    private boolean fromLogoutOrSwitch;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        //UnSerialize if available, should serialize on close
        try {
            FileInputStream fileIn = new FileInputStream("ser/user.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            lastUser = (User) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
        if (lastUser != null) {
            System.out.println("NOT NULL");
            accSwitch = true;
            fromSerializedObject = true;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MailClientView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1500, 1000);

            primaryStage.setTitle("Mailing Client");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    try {
                        FileOutputStream fileOut = new FileOutputStream("ser/user.ser");
                        ObjectOutputStream out = new ObjectOutputStream(fileOut);
                        out.writeObject(controller.getCurrentClient().getLoggedInUser());
                        out.close();
                        fileOut.close();
                        controller.setClientClosed(true);
                        System.out.println("SAVED OBJECT " + controller.getCurrentClient().getLoggedInUser());
                    } catch (IOException i) {
                        i.printStackTrace();
                    }
                }
            });
            primaryStage.show();
        } catch (IOException ex) {
            Logger.getLogger(MailClientController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @FXML
    public void initialize() {
        Subject.setCellValueFactory(new PropertyValueFactory<Email, String>("subject"));
        Sender.setCellValueFactory(new PropertyValueFactory<Email, String>("sender"));
        Date.setCellValueFactory(new PropertyValueFactory<Email, String>("date"));
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Selected " + newValue);
            if(!fromLogoutOrSwitch)
                openEmail(newValue);
        });
        login();
        Users.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switchUser(newValue);
        });
        EmailViewer.setFitToWidth(true);
        EmailViewer.setFitToHeight(true);
    }

    @FXML
    public void compose(ActionEvent e) {
        if (controller.getCurrentClient() == null) {
            return;
        }
        Stage compose = new Stage();
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setStyle("-fx-background-color: azure");
        grid.setVgap(10);
        grid.setPadding(new Insets(15,15,15,15));
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        grid.getColumnConstraints().addAll(col1,col2);
        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        RowConstraints row3 = new RowConstraints();
        RowConstraints row4 = new RowConstraints();
        RowConstraints row5 = new RowConstraints();
        row1.setPercentHeight(20);
        row2.setPercentHeight(10);
        row3.setPercentHeight(10);
        row4.setPercentHeight(30);
        row5.setPercentHeight(10);
        grid.getRowConstraints().addAll(row1, row2, row3, row4, row5);
        Text composeHeader = new Text("Compose");
        composeHeader.setFont(Font.font(composeHeader.getFont().getFamily(), FontWeight.BOLD, composeHeader.getFont().getSize() * 2));
        grid.add(composeHeader, 0, 0);
        Label toEmail = new Label("To: ");
        grid.add(toEmail, 0, 1);
        TextField emailField = new TextField();
        toEmail.setLabelFor(emailField);
        grid.add(emailField, 1, 1);
        Label subject = new Label("Subject: ");
        grid.add(subject, 0, 2);
        TextField subjectField = new TextField();
        subject.setLabelFor(subjectField);
        grid.add(subjectField, 1, 2);
        TextArea body = new TextArea();
        body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        grid.add(body, 0, 3, 3, 3);
        Button send = new Button("Send");
        send.setOnAction(e2 -> {
            controller.compose(new Email("", emailField.getText(), subjectField.getText(), "", body.getText()));
            compose.close();
        });
        grid.add(send, 0,6 );
        compose.setAlwaysOnTop(true);
        Scene scene = new Scene(grid, 600, 400);
        grid.prefWidthProperty().bind(scene.widthProperty());
        compose.setScene(scene);
        compose.show();
    }

    @FXML
    public void login() {
        if(fromSerializedObject){
            controller.login(lastUser);
            Users.getItems().add(controller.getCurrentClient().getLoggedInUser().getAddr());
            tableView.setItems(controller.getCurrentClient().getUnfilteredList());
            EmailOfUser.setText(controller.getCurrentClient().getLoggedInUser().getAddr());
            fromSerializedObject = false;
            return;
        }
        else if(fromLogoutOrSwitch){
            tableView.setItems(controller.getCurrentClient().getUnfilteredList());
            EmailOfUser.setText(controller.getCurrentClient().getLoggedInUser().getAddr());
            fromLogoutOrSwitch = false;
            return;
        }
        Stage login = new Stage();
        GridPane loginForm = new GridPane();
        loginForm.setStyle("-fx-background-color: azure");
        Text loginText = new Text("Login");
        loginText.setStyle("-fx-font-weight: bold");
        loginText.setFont(Font.font(loginText.getFont().getName(), FontWeight.BOLD, loginText.getFont().getSize() * 2));
        loginForm.add(loginText, 0, 0);
        Label email = new Label("Email: ");
        loginForm.add(email, 0, 1);
        Label password = new Label("Password: ");
        loginForm.add(password, 0, 2);
        TextField emailForm = new TextField();
        email.setLabelFor(emailForm);
        loginForm.add(emailForm, 1, 1);
        PasswordField passwordForm = new PasswordField();
        password.setLabelFor(passwordForm);
        loginForm.add(passwordForm, 1, 2);
        Button submit = new Button("Submit");
        submit.setOnAction(e2 -> {
            User newUser = new User("", "", passwordForm.getText(), emailForm.getText());
            fromLogoutOrSwitch = true;
            controller.login(newUser);
            Users.getItems().add(controller.getCurrentClient().getLoggedInUser().getAddr());
            tableView.setItems(controller.getCurrentClient().getUnfilteredList());
            EmailOfUser.setText(controller.getCurrentClient().getLoggedInUser().getAddr());
            fromLogoutOrSwitch = false;
            login.close();
        });
        loginForm.add(submit, 1, 3);
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(30);
        loginForm.getColumnConstraints().add(column);
        column = new ColumnConstraints();
        column.setPercentWidth(60);
        loginForm.getColumnConstraints().add(column);

        loginForm.setPrefSize(300, 300); // Default width and height
        loginForm.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        loginForm.setVgap(10);
        loginForm.setAlignment(Pos.CENTER);
        Scene scene = new Scene(loginForm, 300, 300);
        loginForm.prefWidthProperty().bind(scene.widthProperty());
        login.setAlwaysOnTop(true);
        login.setScene(scene);
        login.show();
    }

    @FXML
    public void logout(ActionEvent e) {
        System.out.println(controller.getCurrentClient());
        if (controller.getCurrentClient() == null)
            return;
        fromLogoutOrSwitch = true;
        ViewerSubject.setText("");
        ViewerBody.setText("No email to display");
        tableView.setItems(FXCollections.observableArrayList());
        EmailOfUser.setText("");
        Users.getItems().remove(controller.getCurrentClient().getLoggedInUser().getAddr());
        controller.logout();
        fromLogoutOrSwitch = controller.getCurrentClient() != null;
        login();
    }

    @FXML
    public void openEmail(Email email) {
        //OPEN POP FOR EMAIL
        if (!controller.getCurrentClient().getInboxFolder().isOpen()) {
            try {
                controller.getCurrentClient().getInboxFolder().open(Folder.READ_ONLY);
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
        ViewerSubject.setText(subject);
        ViewerBody.setText(body);
    }

    @FXML
    public void search(ActionEvent e) {
        Runnable searchRunner = new InboxSearcher(Query.getText());
        new Thread(searchRunner).start();
    }

    @FXML
    public void clearQuery() {
        tableView.getItems().clear();
        tableView.setItems(controller.getCurrentClient().getUnfilteredList());
        Query.setText("");
    }

    @FXML
    public void switchUser(String addr) {
        if (addr == controller.getCurrentClient().getLoggedInUser().getAddr()) {
            System.out.println("SAME ADDR");
            return;
        }
        fromLogoutOrSwitch = true;
        controller.switchUser(addr);
        EmailOfUser.setText(controller.getCurrentClient().getLoggedInUser().getAddr());
        login();
    }
    class InboxSearcher implements Runnable {

        String delimter = "";

        InboxSearcher(String delimter) {
            this.delimter = delimter;
        }

        @Override
        public void run() {
            controller.getCurrentClient().setUnfilteredList(tableView.getItems());
            List<Email> filteredList = tableView.getItems().parallelStream().
                    filter(e -> e.getSubject().contains(this.delimter)).collect(Collectors.toList());
            ObservableList<Email> temp = FXCollections.observableArrayList();
            temp.addAll(filteredList);
            tableView.setItems(temp);
        }
    }

}

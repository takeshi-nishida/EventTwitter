package eventtwitter;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class Main implements Initializable {

  // Configurations
  private static final String configFileName = "config.properties";
  private Properties config;

  // Data structure
  private ObservableList<Status> favorited;
  private Status latestStatus;
  private TwitterStream stream, stream2;
  private LinkedList<Status> statusBuffer;
  private boolean doFireworkFromBuffer = false;
  private Timeline intervalChecker;
  
  // FXML components
  @FXML
  private Label label;
  @FXML
  private ListView list;
  @FXML
  private Pane pane, centerStack;
  @FXML
  private TextField trackField;
  
  @FXML
  private void handleButtonAction(ActionEvent event) {  
    AccessToken accessToken = loadAccessToken();
    if(accessToken == null){
      oauth();
    }
    else{
      openTwitterStream(accessToken);
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    config = new Properties();
    try {
      config.load(new FileInputStream(new File(configFileName)));
      trackField.setText(config.getProperty("track"));
    } catch (IOException e) {
    }
    
    favorited = FXCollections.observableArrayList();
    statusBuffer = new LinkedList<>();

    list.setItems(favorited);
    list.setCellFactory(new Callback<ListView<Status>, ListCell<Status>>() {

      @Override
      public ListCell<Status> call(ListView<Status> arg0) {
        return new StatusCell();
      }
    });

    NumberBinding binding = centerStack.widthProperty().multiply(0.9).divide(label.widthProperty());
    label.scaleXProperty().bind(binding);
    label.scaleYProperty().bind(binding);
    
    intervalChecker = new Timeline(new KeyFrame(Duration.seconds(5), new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        synchronized (statusBuffer) {
          if (doFireworkFromBuffer) {
            System.out.println("firework from buffer");
            doFirework(statusBuffer.get((int) (Math.random() * statusBuffer.size())));
          }
          if(!statusBuffer.isEmpty()){
            doFireworkFromBuffer = true;
          }
        }
      }
    }));
    intervalChecker.setCycleCount(Timeline.INDEFINITE);
  }

  public void cleanup() {
    try {
      config.setProperty("track", trackField.getText());
      config.store(new FileOutputStream(new File(configFileName)), "");
    } catch (IOException ex) {
    }

    if (stream != null) {
      stream.cleanUp();
    }
    if (stream2 != null) {
      stream2.cleanUp();
    }
  }

  public void doFirework(Status status) {
    double x = pane.widthProperty().getValue() * Math.random();
    double y = pane.heightProperty().getValue();
    double h = (pane.heightProperty().getValue() - Firework.size) * 0.5 * (1 + Math.random());
    Firework firework = Firework.getInstance();
    if (!pane.getChildren().contains(firework)) {
      AddFireworkRunner runner = new AddFireworkRunner(firework);
      Platform.runLater(runner);
    }
    firework.play(x, y, h, status.getUser().getProfileImageURL());
  }

  private void showNewTrustedStatus(Status s) {
    if (latestStatus != null) {
      favorited.add(0, latestStatus);
      if (favorited.size() > 4) {
        favorited.remove(favorited.size() - 1);
      }
      int i = favorited.indexOf(latestStatus);
      list.scrollTo(i);
    }

    if (s != null) {
      latestStatus = s;
      User u = latestStatus.getUser();
      label.setText(u.getName() + "\n" + latestStatus.getText());
      Image image = new Image(u.getProfileImageURL().toString());
      label.setGraphic(new ImageView(image));
    }

  }
  
  //<editor-fold defaultstate="collapsed" desc="Twitter">
  private Twitter twitter;
  private RequestToken requestToken;
  private Stage oAuthDialog;
  private TextField pinField;

  public void openTwitterStream(AccessToken accessToken) {
    if (trackField.getText().length() > 0) {
      String[] track = trackField.getText().split(" ");
      TwitterStreamFactory factory = new TwitterStreamFactory();
      stream = factory.getInstance(accessToken);
      stream2 = factory.getInstance(accessToken);
      stream.addListener(new PublicStreamHandler());
      stream2.addListener(new UserStreamHandler());
      FilterQuery query = new FilterQuery();
      query.track(track);
      stream.filter(query);
      stream2.user();
      
      intervalChecker.playFromStart();
    } else {
      Stage dialog = new Stage();
      dialog.initModality(Modality.WINDOW_MODAL);
      dialog.setScene(new Scene(new Label("Input filter keyword(s)/hashtag(s)")));
      dialog.setTitle("No filter specified");
      dialog.show();
    }
  }
  
  private AccessToken loadAccessToken(){
    String token = config.getProperty("oAuthAccessToken");
    String tokenSecret = config.getProperty("oAuthAccessTokenSecret");
    return (token != null && tokenSecret != null) ? new AccessToken(token, tokenSecret) : null;
  }
  
  private void saveAccessToken(AccessToken accessToken){
    config.setProperty("oAuthAccessToken", accessToken.getToken());
    config.setProperty("oAuthAccessTokenSecret", accessToken.getTokenSecret());    
  }
  
  private void oauth(){
    try {
      twitter = new TwitterFactory().getInstance();
      requestToken = twitter.getOAuthRequestToken();
      String url = requestToken.getAuthorizationURL();

      if(oAuthDialog == null){
        oAuthDialog = new Stage();
        oAuthDialog.setTitle("初回接続時は認証する必要があります");
        pinField = new TextField();
        Button closeButton = new Button("このPINで認証");
        VBox box = new VBox();

        oAuthDialog.initModality(Modality.WINDOW_MODAL);
        if(Desktop.isDesktopSupported()){
          Desktop.getDesktop().browse(new URI(url));
        }
        else{
          WebView webView = new WebView();
          webView.getEngine().load(url);
          box.getChildren().addAll(webView);
        }

        closeButton.disabledProperty().isEqualTo(pinField.lengthProperty().lessThan(1));
        closeButton.setOnAction(new EventHandler<ActionEvent>(){
          @Override
          public void handle(ActionEvent arg0) {
            oAuthDialog.close();
            try {
              AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, pinField.getText());
              saveAccessToken(accessToken);
              openTwitterStream(accessToken);
            } catch (TwitterException ex) {
              Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        });
        box.getChildren().addAll(new Label("別ウィンドウで認証手続き後\nPINを入力してください"), pinField, closeButton);
        oAuthDialog.setScene(new Scene(box));
      }
      
      oAuthDialog.show();
    } catch (TwitterException | URISyntaxException | IOException ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
    
  class PublicStreamHandler extends StatusAdapter {

    @Override
    public void onStatus(Status status) {
      synchronized (statusBuffer) {
        doFireworkFromBuffer = false;
        if (statusBuffer.size() > 10) {
          statusBuffer.remove();
        }
        statusBuffer.add(status);
      }
      
      doFirework(status);
    }
  }

  class UserStreamHandler extends UserStreamAdapter {

    @Override
    public void onFavorite(User source, User target, Status favoritedStatus) {
      UpdateLatestRunner runner = new UpdateLatestRunner(favoritedStatus);
      Platform.runLater(runner);
    }
  }  
  //</editor-fold>
  
  //<editor-fold defaultstate="collapsed" desc="Runners">
    class AddFireworkRunner implements Runnable {

    private Firework firework;

    public AddFireworkRunner(Firework firework) {
      this.firework = firework;
    }

    @Override
    public void run() {
      pane.getChildren().add(firework);
    }
  }

  class UpdateLatestRunner implements Runnable {

    private Status status;

    public UpdateLatestRunner(Status status) {
      this.status = status;
    }

    @Override
    public void run() {
      showNewTrustedStatus(status);
    }
  }

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Cell renderers">
  class StatusCell extends ListCell<Status> {

    @Override
    protected void updateItem(Status s, boolean empty) {
      super.updateItem(s, empty);

      if (s != null) {
        double w = getListView().getWidth() * 0.33;
        setPrefWidth(w);
        setMaxWidth(w);
        setWrapText(true);
        setFont(Font.font("Meiryo UI", 20));
        Image image = new Image(s.getUser().getProfileImageURL().toString());
        setGraphic(new ImageView(image));
        setText(s.getUser().getName() + "\n" + s.getText());
      }
    }
  }

//  class StatusIconCell extends ListCell<Status> {
//
//    @Override
//    protected void updateItem(Status s, boolean empty) {
//      super.updateItem(s, empty);
//
//      if (s != null) {
//        Image image = new Image(s.getUser().getProfileImageURL().toString());
//        setGraphic(new ImageView(image));
//      }
//    }
//  }  
  //</editor-fold>

}

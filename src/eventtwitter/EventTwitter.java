/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eventtwitter;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author tnishida
 */
public class EventTwitter extends Application {
  private FXMLLoader loader;

  public static void main(String[] args) {
    Application.launch(EventTwitter.class, args);
  }
  
  @Override
  public void start(Stage stage) throws Exception {
    URL location = getClass().getResource("Main.fxml");
    loader = new FXMLLoader();
    loader.setLocation(location);
    loader.setBuilderFactory(new JavaFXBuilderFactory());
    Parent root = (Parent) loader.load(location.openStream());
    
    Scene scene = new Scene(root);
    scene.getStylesheets().add("eventtwitter/style.css");
    stage.setScene(scene);
//    stage.setFullScreen(true);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    Main main = (Main) loader.getController();
    main.cleanup();
  }
}

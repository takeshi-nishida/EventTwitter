package eventtwitter;

import java.net.URL;
import java.util.Stack;
import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class Firework extends Group {
  private static final Color[] colors = { Color.RED, Color.MAGENTA, Color.BLUE, Color.LIMEGREEN };  
  private static final AudioClip audioClip;
  private static final Stack<Firework> pool;
  public static final int size = 100;

  static{
    audioClip = new AudioClip(Firework.class.getResource("firework.mp3").toString());
    pool = new Stack<>();
  }
  
  public static Firework getInstance(){
    synchronized(pool){
      if(pool.isEmpty()){
        Firework firework = new Firework();
        return firework;
      }
      else{
        return pool.pop();
      }
    }
  }

  private SequentialTransition st;
  private TranslateTransition upTransition;
  private Group tail, body;
  private ImageView icon;

  private Firework() {
    super();
    initTransition();
  }

  public void play(double x, double y, double h, URL url) {
    tail.setLayoutX(x);
    tail.setLayoutY(y);
    body.setLayoutX(x);
    body.setLayoutY(y - h);
    upTransition.setByY(- h);
    setVisible(true);
    body.setOpacity(0.0);
    Image image = new Image(url.toString());
    icon.setImage(image);
    icon.setX(- image.getWidth() / 2);
    icon.setY(- image.getHeight() / 2);
    st.playFromStart();
  }
    
  private void initTransition() {
    // Create firework tail that goes up
    tail = new Group();
    for (int i = 0; i < 3; i++) {
      Circle c = new Circle(0, i * 10, 5 - i);
      c.setFill(Color.WHITE);
      c.setOpacity(1.0 / (i + 1));
      tail.getChildren().add(c);
    }
    getChildren().add(tail);
    
    st = new SequentialTransition();

    upTransition = new TranslateTransition(Duration.millis(800), tail);
    upTransition.setFromY(0);
    upTransition.setInterpolator(Interpolator.EASE_OUT);
    
    FadeTransition ft = new FadeTransition(Duration.millis(800), tail);
    ft.setFromValue(1.0);
    ft.setToValue(0.0);
    
    ParallelTransition pt = new ParallelTransition();
    pt.getChildren().addAll(upTransition, ft);
    st.getChildren().add(pt);
    
    // Create firework body that explodes
    body = new Group();
 
    ParallelTransition bodyTransition = new ParallelTransition();
    int n = 16;
    Color c = getRandomColor();
    for(int i = 0; i < n; i++){
      double theta = 2 * Math.PI * i / n;
      double cos = Math.cos(theta), sin = Math.sin(theta);
      Shape s = new Line(cos * 5, sin * 5, cos * 15, sin * 15);
      s.setStroke(c);
      s.setStrokeWidth(6);
      s.setStrokeLineCap(StrokeLineCap.ROUND);
      body.getChildren().add(s);
      TranslateTransition tt = new TranslateTransition(Duration.millis(300), s);
      tt.setFromX(cos * 5);
      tt.setFromY(sin * 5);
      tt.setByX(cos * size);
      tt.setByY(sin * size);
      bodyTransition.getChildren().add(tt);
    }
    icon = new ImageView();
    body.getChildren().add(icon);
    ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), icon);
    scaleTransition.setFromX(1.0); scaleTransition.setFromY(1.0);
    scaleTransition.setToX(2.0); scaleTransition.setToY(2.0);
    bodyTransition.getChildren().add(scaleTransition);

    getChildren().add(body);

    ft = new FadeTransition(Duration.millis(600), body);
    ft.setFromValue(0.0);
    ft.setToValue(1.0);
    ft.setAutoReverse(true);
    ft.setCycleCount(2);
    pt = new ParallelTransition();
    pt.setInterpolator(Interpolator.EASE_OUT);
    pt.getChildren().addAll(bodyTransition, ft);
    st.getChildren().add(pt);
    
    st.setOnFinished(new EventHandler<ActionEvent>(){
      @Override
      public void handle(ActionEvent arg0) {
        handleFinish();
      }
    });
  }
  
  private Color getRandomColor(){
    return colors[(int) (Math.random() * colors.length)];
  }
  
  private void handleFinish(){
    setVisible(false);
    audioClip.play();
    synchronized(pool){
      pool.push(this);
    }
  }
}

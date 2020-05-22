package ru.akman.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) {
    if (LOG.isInfoEnabled()) {
      LOG.info("BEGIN");
    }
    Application.launch(args);
    if (LOG.isInfoEnabled()) {
      LOG.info("END");
    }
  }

  @Override
  public void start(final Stage stage) {
    if (LOG.isInfoEnabled()) {
      LOG.info("GUI start");
    }
    stage.setTitle("Hello Modular Application");
    stage.setScene(new Scene(new Group(), 800, 600));
    stage.centerOnScreen();
    stage.show();
  }

  @Override
  public void stop() {
    if (LOG.isInfoEnabled()) {
      LOG.info("GUI stop");
    }
  }

}

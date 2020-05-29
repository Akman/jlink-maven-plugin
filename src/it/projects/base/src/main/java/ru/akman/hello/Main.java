package ru.akman.hello;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
// import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import static org.fusesource.jansi.Ansi.*;
// import static org.fusesource.jansi.Ansi.Color.*;

public class Main extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) {
    // AnsiConsole.systemInstall();
    // System.out.println(ansi()
    //     .eraseScreen()
    //     .fg(RED).a("Hello,")
    //     .fg(GREEN).a(" JavaFX!")
    //     .reset());
    if (LOG.isInfoEnabled()) {
      LOG.info("BEGIN");
    }
    Application.launch(args);
    if (LOG.isInfoEnabled()) {
      LOG.info("END");
    }
    // System.out.println(ansi()
    //     .eraseScreen()
    //     .render("@|red Good|@ @|green bye.|@"));
    // AnsiConsole.systemUninstall();    
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

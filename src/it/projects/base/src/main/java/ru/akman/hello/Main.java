package ru.akman.hello;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "hello",
  mixinStandardHelpOptions = true,
  version = "Version 1.0",
  description = "Simple JavaFX application"
)
public class Main extends Application implements Callable<Integer> {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  @Option(
    names = {"-d", "--debug"},
    description = "Log in debug mode."
  )
  private boolean isDebugEnabled = false;

  public static void main(String... args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);    
  }

  @Override
  public Integer call() throws Exception {
    if (isDebugEnabled) {
      ch.qos.logback.classic.Logger rootLogger =
          (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
              ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("BEGIN");
    }
    Application.launch();
    if (LOG.isDebugEnabled()) {
      LOG.debug("END");
    }
    return 0;
  }

  @Override
  public void start(final Stage stage) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("GUI start");
    }
    stage.setTitle("Hello Modular Application");
    stage.setScene(new Scene(new Group(), 800, 600));
    stage.centerOnScreen();
    stage.show();
  }

  @Override
  public void stop() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("GUI stop");
    }
  }

}

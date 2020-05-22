module hello {

  // slf4j + logback
  requires org.slf4j;
  requires java.naming;
  requires ch.qos.logback.classic;
  // requires ch.qos.logback.core;

  // javafx
  requires javafx.graphics;
  exports ru.akman.hello to javafx.graphics;

}

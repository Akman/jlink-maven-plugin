


hello                     file:///F:/Workspace/java/jlink-maven-plugin/target/it/projects/base/target/classes/

ch.qos.logback.classic    file:///F:/Workspace/java/jlink-maven-plugin/target/local-repo/ch/qos/logback/logback-classic/1.3.0-alpha4/logback-classic-1.3.0-alpha4.jar
ch.qos.logback.core       file:///F:/Workspace/java/jlink-maven-plugin/target/local-repo/ch/qos/logback/logback-core/1.3.0-alpha4/logback-core-1.3.0-alpha4.jar
javafx.base               file:///F:/Workspace/java/jlink-maven-plugin/target/local-repo/org/openjfx/javafx-base/13.0.2/javafx-base-13.0.2-win.jar
javafx.graphics           file:///F:/Workspace/java/jlink-maven-plugin/target/local-repo/org/openjfx/javafx-graphics/13.0.2/javafx-graphics-13.0.2-win.jar
org.slf4j                 file:///F:/Workspace/java/jlink-maven-plugin/target/local-repo/org/slf4j/slf4j-api/2.0.0-alpha1/slf4j-api-2.0.0-alpha1.jar

java.base                 file:///D:/Programs/Java/jdk-13/jmods/java.base.jmod
java.datatransfer         file:///D:/Programs/Java/jdk-13/jmods/java.datatransfer.jmod
java.desktop              file:///D:/Programs/Java/jdk-13/jmods/java.desktop.jmod
java.logging              file:///D:/Programs/Java/jdk-13/jmods/java.logging.jmod
java.naming               file:///D:/Programs/Java/jdk-13/jmods/java.naming.jmod
java.prefs                file:///D:/Programs/Java/jdk-13/jmods/java.prefs.jmod
java.security.sasl        file:///D:/Programs/Java/jdk-13/jmods/java.security.sasl.jmod
java.xml                  file:///D:/Programs/Java/jdk-13/jmods/java.xml.jmod
jdk.unsupported           file:///D:/Programs/Java/jdk-13/jmods/jdk.unsupported.jmod




Providers:
  java.desktop            provides java.net.ContentHandlerFactory used by java.base
  java.desktop            provides javax.print.PrintServiceLookup used by java.desktop
  java.desktop            provides javax.print.StreamPrintServiceFactory used by java.desktop
  java.desktop            provides javax.sound.midi.spi.MidiDeviceProvider used by java.desktop
  java.desktop            provides javax.sound.midi.spi.MidiFileReader used by java.desktop
  java.desktop            provides javax.sound.midi.spi.MidiFileWriter used by java.desktop
  java.desktop            provides javax.sound.midi.spi.SoundbankReader used by java.desktop
  java.desktop            provides javax.sound.sampled.spi.AudioFileReader used by java.desktop
  java.desktop            provides javax.sound.sampled.spi.AudioFileWriter used by java.desktop
  java.desktop            provides javax.sound.sampled.spi.FormatConversionProvider used by java.desktop
  java.desktop            provides javax.sound.sampled.spi.MixerProvider used by java.desktop
  java.desktop            provides sun.datatransfer.DesktopDatatransferService used by java.datatransfer
  java.base               provides java.nio.file.spi.FileSystemProvider used by java.base
  java.naming             provides java.security.Provider used by java.base
  java.security.sasl      provides java.security.Provider used by java.base
  java.logging            provides jdk.internal.logger.DefaultLoggerFinder used by java.base
  ch.qos.logback.classic  provides org.slf4j.spi.SLF4JServiceProvider used by org.slf4j



REQUIRES::MODIFIERS

MANDATED   - The dependence was implicitly declared in
             the source of the module declaration.
STATIC     - The dependence is mandatory in the static phase,
             during compilation, but is optional in the dynamic phase,
             during execution.
SYNTHETIC  - The dependence was not explicitly or implicitly declared
             in the source of the module declaration.
TRANSITIVE - The dependence causes any module which depends on
             the current module to have an implicitly declared
             dependence on the module named by the Requires.





file: javafx-base-13.0.2-win.jar
name: javafx.base
automatic: false
requires:
  java.desktop : {}
  jdk.jfr : { STATIC }
  java.base : {}
exports:
  com.sun.javafx.logging : { javafx.web, javafx.graphics, javafx.controls, javafx.swing, javafx.fxml }
  javafx.collections.transformation : {}
  javafx.beans.property : {}
  javafx.collections : {}
  javafx.util.converter : {}
  com.sun.javafx.binding : { javafx.graphics, javafx.controls }
  javafx.beans.binding : {}
  com.sun.javafx.reflect : { javafx.fxml, javafx.web }
  javafx.beans.value : {}
  javafx.beans.property.adapter : {}
  com.sun.javafx.property : { javafx.controls }
  javafx.event : {}
  com.sun.javafx : { javafx.fxml, javafx.swing, javafx.graphics, javafx.controls }
  com.sun.javafx.runtime : { javafx.graphics }
  com.sun.javafx.event : { javafx.graphics, javafx.controls }
  com.sun.javafx.beans : { javafx.graphics, javafx.controls, javafx.fxml }
  com.sun.javafx.collections : { javafx.swing, javafx.graphics, javafx.controls, javafx.media }
  javafx.beans : {}
  javafx.util : {}
provides:
uses:

file: javafx-controls-13.0.2-win.jar
name: javafx.controls
automatic: false
requires:
  javafx.graphics : {}
  java.base : {}
  javafx.base : {}
exports:
  javafx.scene.control.cell : {}
  javafx.scene.chart : {}
  com.sun.javafx.scene.control.skin : { javafx.graphics, javafx.web }
  com.sun.javafx.scene.control : { javafx.web }
  com.sun.javafx.scene.control.inputmap : { javafx.web }
  com.sun.javafx.scene.control.behavior : { javafx.web }
  javafx.scene.control : {}
  javafx.scene.control.skin : {}
provides:
uses:

file: javafx-graphics-13.0.2-win.jar
name: javafx.graphics
automatic: false
requires:
  java.xml : {}
  java.desktop : {}
  jdk.unsupported : {}
  javafx.base : {}
  java.base : {}
exports:
  com.sun.javafx.scene.layout : { javafx.controls, javafx.web }
  com.sun.prism.image : { javafx.web }
  javafx.stage : {}
  com.sun.glass.ui : { javafx.media, javafx.web }
  com.sun.javafx.stage : { javafx.swing, javafx.controls }
  com.sun.javafx.sg.prism : { javafx.swing, javafx.web, javafx.media }
  com.sun.javafx.scene : { javafx.media, javafx.swing, javafx.controls, javafx.web }
  com.sun.scenario.effect.impl : { javafx.web }
  com.sun.javafx.cursor : { javafx.swing }
  javafx.css.converter : {}
  javafx.scene : {}
  com.sun.prism : { javafx.media, javafx.web }
  javafx.css : {}
  com.sun.glass.utils : { javafx.media, javafx.web }
  com.sun.javafx.tk : { javafx.media, javafx.swing, javafx.controls, javafx.web }
  javafx.scene.effect : {}
  javafx.scene.canvas : {}
  com.sun.javafx.font : { javafx.web }
  com.sun.javafx.geom : { javafx.media, javafx.swing, javafx.controls, javafx.web }
  com.sun.javafx.text : { javafx.web }
  javafx.scene.paint : {}
  com.sun.javafx.util : { javafx.web, javafx.controls, javafx.swing, javafx.fxml, javafx.media }
  com.sun.javafx.scene.text : { javafx.controls, javafx.web }
  com.sun.javafx.scene.input : { javafx.swing, javafx.web, javafx.controls }
  javafx.scene.text : {}
  javafx.geometry : {}
  javafx.scene.image : {}
  com.sun.javafx.scene.traversal : { javafx.controls, javafx.web }
  javafx.scene.shape : {}
  javafx.print : {}
  com.sun.javafx.iio : { javafx.web }
  com.sun.javafx.css : { javafx.controls }
  com.sun.javafx.geom.transform : { javafx.media, javafx.swing, javafx.controls, javafx.web }
  com.sun.javafx.embed : { javafx.swing }
  javafx.concurrent : {}
  javafx.scene.layout : {}
  javafx.scene.transform : {}
  com.sun.prism.paint : { javafx.web }
  javafx.animation : {}
  com.sun.scenario.effect : { javafx.web }
  javafx.application : {}
  com.sun.javafx.menu : { javafx.controls }
  com.sun.scenario.effect.impl.prism : { javafx.web }
  com.sun.javafx.application : { javafx.swing,java.base, javafx.controls, javafx.web }
  javafx.scene.input : {}
  javafx.scene.robot : {}
provides:
uses:

file: javafx-fxml-13.0.2-win.jar
name: javafx.fxml
automatic: false
requires:
  java.xml : {}
  javafx.graphics : {}
  java.scripting : {}
  javafx.base : {}
  java.base : {}
exports:
  javafx.fxml : {}
provides:
uses:

file: javafx-swing-13.0.2-win.jar
name: javafx.swing
automatic: false
requires:
  java.desktop : {}
  java.datatransfer : {}
  java.base : {}
  javafx.base : {}
  javafx.graphics : {}
  jdk.unsupported.desktop : {}
exports:
  javafx.embed.swing : {}
  com.sun.javafx.embed.swing : { javafx.graphics }
provides:
uses:

file: slf4j-api-2.0.0-alpha1.jar
name: org.slf4j
automatic: false
requires:
  java.base : {}
exports:
  org.slf4j : {}
  org.slf4j.spi : {}
  org.slf4j.event : {}
  org.slf4j.helpers : {}
provides:
uses: { org.slf4j.spi.SLF4JServiceProvider }

file: logback-classic-1.3.0-alpha4.jar
name: ch.qos.logback.classic
automatic: false
requires:
  java.management : { STATIC }
  org.slf4j : {}
  java.base : {}
  ch.qos.logback.core : {}
  javax.servlet.api : { STATIC }
exports:
  ch.qos.logback.classic.encoder : {}
  ch.qos.logback.classic.util : {}
  ch.qos.logback.classic.turbo : {}
  ch.qos.logback.classic.jul : {}
  ch.qos.logback.classic.db : {}
  ch.qos.logback.classic.jmx : {}
  ch.qos.logback.classic.selector.servlet : {}
  ch.qos.logback.classic.db.names : {}
  ch.qos.logback.classic.servlet : {}
  ch.qos.logback.classic.net : {}
  ch.qos.logback.classic.html : {}
  ch.qos.logback.classic.filter : {}
  ch.qos.logback.classic.pattern.color : {}
  ch.qos.logback.classic.sift : {}
  ch.qos.logback.classic.spi : {}
  ch.qos.logback.classic.joran : {}
  ch.qos.logback.classic.net.server : {}
  ch.qos.logback.classic.pattern : {}
  ch.qos.logback.classic.boolex : {}
  ch.qos.logback.classic : {}
  ch.qos.logback.classic.selector : {}
  ch.qos.logback.classic.log4j : {}
  ch.qos.logback.classic.layout : {}
  ch.qos.logback.classic.helpers : {}
  ch.qos.logback.classic.joran.action : {}
provides:
  org.slf4j.spi.SLF4JServiceProvider : { ch.qos.logback.classic.spi.LogbackServiceProvider }
uses: { ch.qos.logback.classic.spi.Configurator }

file: logback-core-1.3.0-alpha4.jar
name: ch.qos.logback.core
automatic: false
requires:
  java.base : {}
  javax.mail.api : { STATIC }
  janino : { STATIC }
  javax.servlet.api : { STATIC }
  java.xml : { STATIC }
  java.naming : { STATIC }
  java.sql : { STATIC }
  commons.compiler : { STATIC }
exports:
  ch.qos.logback.core.sift : {}
  ch.qos.logback.core.net.server : {}
  ch.qos.logback.core.joran.conditional : {}
  ch.qos.logback.core.joran.event : {}
  ch.qos.logback.core.joran.util : {}
  ch.qos.logback.core.status : {}
  ch.qos.logback.core.encoder : {}
  ch.qos.logback.core.util : {}
  ch.qos.logback.core.net.ssl : {}
  ch.qos.logback.core : {}
  ch.qos.logback.core.pattern : {}
  ch.qos.logback.core.joran : {}
  ch.qos.logback.core.pattern.parser : {}
  ch.qos.logback.core.helpers : {}
  ch.qos.logback.core.html : {}
  ch.qos.logback.core.pattern.color : {}
  ch.qos.logback.core.boolex : {}
  ch.qos.logback.core.filter : {}
  ch.qos.logback.core.net : {}
  ch.qos.logback.core.joran.spi : {}
  ch.qos.logback.core.db : {}
  ch.qos.logback.core.joran.action : {}
  ch.qos.logback.core.spi : {}
provides:
uses:

file: javafx-base-13.0.2.jar
name: javafx.baseEmpty
automatic: true
requires:
exports:
provides:
uses:

file: javafx-controls-13.0.2.jar
name: javafx.controlsEmpty
automatic: true
requires:
exports:
provides:
uses:

file: javafx-graphics-13.0.2.jar
name: javafx.graphicsEmpty
automatic: true
requires:
exports:
provides:
uses:

file: javafx-fxml-13.0.2.jar
name: javafx.fxmlEmpty
automatic: true
requires:
exports:
provides:
uses:

file: javafx-swing-13.0.2.jar
name: javafx.swingEmpty
automatic: true
requires:
exports:
provides:
uses:

file: javax.mail-1.6.0.jar
name: javax.mail
automatic: true
requires:
exports:
provides:
uses:

file: activation-1.1.jar
name: activation
automatic: true
requires:
exports:
provides:
uses:
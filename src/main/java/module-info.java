module com.berrygobbler78.flacplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires java.desktop;
    requires jflac.codec;
    requires jaudiotagger;
    requires java.xml.crypto;
    requires com.pixelduke.fxthemes;
    requires com.jfoenix;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires java.logging;
    requires jdk.dynalink;
    requires JavaMediaTransportControls;

    opens com.berrygobbler78.flacplayer to javafx.fxml;
    exports com.berrygobbler78.flacplayer;
    exports com.berrygobbler78.flacplayer.controllers;
    opens com.berrygobbler78.flacplayer.controllers to javafx.fxml;
    exports com.berrygobbler78.flacplayer.util;
    opens com.berrygobbler78.flacplayer.util to javafx.fxml;
    exports com.berrygobbler78.flacplayer.userdata;
    opens com.berrygobbler78.flacplayer.userdata to javafx.fxml;
}
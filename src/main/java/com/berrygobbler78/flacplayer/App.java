package com.berrygobbler78.flacplayer;

import java.io.*;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * JavaFX App
 */
public class App extends Application {

    public static FileUtils fileUtils;
    public static MusicPlayer musicPlayer;
    public static UserData userData;

    private static File userDataFile = new File("src/main/resources/com/berrygobbler78/flacplayer/cache/UserData.ser");

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException, ClassNotFoundException {
        fileUtils = new FileUtils();
        musicPlayer = new MusicPlayer();

        FileInputStream fis;
        ObjectInputStream ois;
        try {
            fis = new FileInputStream(userDataFile);
            ois = new ObjectInputStream(fis);
            userData = (UserData) ois.readObject();

            fis.close();
            ois.close();

        } catch (IOException e) {
            userData = new UserData();
            File selectedDirectory = fileUtils.directoryChooser(new Stage(), "Pick a Directory", "C:");
            App.userData.setRootDirectoryPath(selectedDirectory.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(userDataFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(userData);
            oos.close();
            fos.close();

        }


        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("revised.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("css/styles.css")).toExternalForm());
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();



        musicPlayer.setController(fxmlLoader.getController());
    }

    static void main(String[] args) {
        launch();
    }

    public static void restore() {
        primaryStage.setMaximized(false);
    }

    public static void maximize() {
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(primaryScreenBounds.getMinX());
        primaryStage.setY(primaryScreenBounds.getMinY());

        primaryStage.setMaxWidth(primaryScreenBounds.getWidth());
        primaryStage.setMinWidth(primaryScreenBounds.getWidth());

        primaryStage.setMaxHeight(primaryScreenBounds.getHeight());
        primaryStage.setMinHeight(primaryScreenBounds.getHeight());
    }

    public static void minimize() {
        primaryStage.setIconified(true);
    }

    public static void exit() {
        System.exit(0);
    }



}
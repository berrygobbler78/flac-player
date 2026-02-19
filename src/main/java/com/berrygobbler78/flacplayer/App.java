package com.berrygobbler78.flacplayer;

import java.io.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.userdata.References;
import com.berrygobbler78.flacplayer.util.Constants;
import com.berrygobbler78.flacplayer.util.FileUtils;
import com.berrygobbler78.flacplayer.util.Constants.FXML_PATHS;

import com.pixelduke.window.ThemeWindowManagerFactory;
import com.pixelduke.window.Win11ThemeWindowManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

public class App extends Application {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    private static References references;

    private static final String referencesPath =
            "src/main/resources/com/berrygobbler78/flacplayer/cache/references.ser";

    private static Stage primaryStage;

    public enum OS {
        LINUX,
        WINDOWS_11
    }

    private static OS currentOS;

    void main() {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        switch (System.getProperty("os.name")) {
            case "Linux" -> currentOS = OS.LINUX;
            case "Windows 11" -> currentOS = OS.WINDOWS_11;
        }

        deleteTempFile();

        // Checks if userData already exists, if not prompt for new directory
        File referencesFile = new File(referencesPath);

        try {
            FileInputStream fis = new FileInputStream(referencesFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            references = (References) ois.readObject();

            fis.close();
            ois.close();

            LOGGER.log(Level.INFO, "Previous references loaded.");
        } catch (Exception e) {
            references = new References();

            setupWizard();

            FileOutputStream fos = new FileOutputStream(referencesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(references);

            oos.close();
            fos.close();

            LOGGER.log(Level.INFO, "Created new references.");
        }

        loadPlaylists();

        FXMLLoader fxmlLoader =
                new FXMLLoader(new File(FXML_PATHS.MAIN.get()).getAbsoluteFile().toURI().toURL());

        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("css/styles.css")).toExternalForm());

        primaryStage = stage;
        primaryStage.setTitle("BerryBush");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(Constants.IMAGES.BERRIES.get());
        primaryStage.show();
        primaryStage.setOnCloseRequest(_ -> saveUserData());

        if(currentOS == OS.WINDOWS_11) {
            Win11ThemeWindowManager themeWindowManager =
                    (Win11ThemeWindowManager) ThemeWindowManagerFactory.create(); // For coloring window border
            themeWindowManager.setWindowFrameColor(primaryStage, Color.web("#121212"));
            themeWindowManager.setDarkModeForWindowFrame(primaryStage, true);
        }

        LOGGER.log(Level.FINE, "Stage created.");
    }

    public void saveUserData() {
        savePlaylists();
        saveReferences();

        LOGGER.log(Level.INFO, "User data saved.");
    }

    public static void saveReferences() {
        references.clearPlaylists();

        try {
            FileOutputStream fos = new FileOutputStream(referencesPath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(references);

            oos.close();
            fos.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not save references.", e);
        }
    }

    public static void savePlaylists() {
        try{
            for(Playlist playlist : references.getPlaylists()){
                FileOutputStream fos = new FileOutputStream("src/main/resources/com/berrygobbler78/flacplayer/cache/playlists/" + playlist.getName().toLowerCase().replace(" ", "-") + ".ser");
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeObject(playlist);

                oos.close();
                fos.close();
            }

            LOGGER.log(Level.INFO, "Playlists saved.");
        } catch(IOException e){
            LOGGER.log(Level.SEVERE, "Could not save playlists.", e);
        }
    }

    public static void deletePlaylist(Playlist playlist) {
        File playlistFile = new File("src/main/resources/com/berrygobbler78/flacplayer/cache/playlists/" +  playlist.getName().toLowerCase().replace(" ", "-") + ".ser");

        if(playlistFile.delete()) {
            LOGGER.log(Level.INFO, "Deleted playlist." +  playlist.getName());
        } else {
            LOGGER.log(Level.WARNING, "Could not delete playlist." +  playlist.getName());
        }
    }

    public void loadPlaylists() {
        try {
            for (File file : Objects.requireNonNull(new File("src/main/resources/com/berrygobbler78/flacplayer/cache/playlists").listFiles())) {
                if (file.getName().endsWith(".ser")) {
                    FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    references.addPlaylist((Playlist) ois.readObject());

                    fis.close();
                    ois.close();

                    LOGGER.log(Level.INFO, "Loaded playlist " + file.getName());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Could not load playlists." + e);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    static void deleteTempFile() {
        File tempFile = new File("src/main/resources/com/berrygobbler78/flacplayer/cache/temp.wav");

        if(!tempFile.exists()) {
            LOGGER.log(Level.INFO, "Temp file does not exist.");
            return;
        }

        if(tempFile.delete()) {
            LOGGER.log(Level.INFO, "Deleted temp file.");
        } else {
            LOGGER.log(Level.WARNING, "Failed to delete temp file.");
        }
    }

    static void setupWizard() {
        Wizard wizard = new Wizard();

        // Page 1
        Label question1 = new Label("Enter Directory Location:");

        TextField textField1 = new TextField();
            textField1.setEditable(true);
            textField1.setPromptText("Select Directory");
            textField1.setMinWidth(300.0);
            textField1.setMaxHeight(10.0);
            textField1.setMaxWidth(300.0);

        Button button1 = new Button();
        switch (currentOS) {
            case WINDOWS_11 ->
                button1.setOnAction(_ -> textField1.setText(FileUtils.openDirectoryChooser(primaryStage, "Choose directory", "C://").getAbsolutePath()));
            case LINUX ->
                    button1.setOnAction(_ -> textField1.setText(FileUtils.openDirectoryChooser(primaryStage, "Choose directory", "/home").getAbsolutePath()));

        }

            button1.setText("Open Explorer");

        HBox hbox1 = new HBox();
            hbox1.getChildren().addAll(textField1, button1);
            hbox1.setSpacing(10.0);

        VBox vbox1 = new VBox();
            vbox1.getChildren().addAll(question1, hbox1);

        WizardPane page1 = new WizardPane();
            page1.setContent(vbox1);

        // Page2
        Label question2 = new Label("Enter Username:");

        TextField textField2 = new TextField();
            textField2.setEditable(true);
            textField2.setPromptText("Username...");
            textField2.setMinWidth(300.0);
            textField2.setMaxHeight(10.0);
            textField2.setMaxWidth(300.0);

        VBox vbox2 = new VBox();
            vbox2.getChildren().addAll(question2, textField2);

        WizardPane page2 = new WizardPane();
            page2.setContent(vbox2);

        wizard.setUserData(textField2.getText());

        // Wizard settings
        wizard.setTitle("BerryBush Setup Wizard");
        wizard.setFlow(new Wizard.LinearFlow(page1, page2));

        // Show wizard and wait, set userData
        wizard.showAndWait().ifPresent(result -> {
            if (result == ButtonType.FINISH) {
                references.setRootDirectoryPath(textField1.getText());
                references.setUserName(textField2.getText());

                saveReferences();
            }
        });
    }

    public static References getReferences() {
        return references;
    }

    public static OS getCurrentOS() {
        return currentOS;
    }
}
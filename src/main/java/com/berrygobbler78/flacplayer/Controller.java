package com.berrygobbler78.flacplayer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;

public class Controller implements  Initializable {
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private Label songLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Button playPauseButton, nextButton, previousButton, repeatButton, shuffleButton;
    @FXML
    private Button exitButton;
    @FXML
    private Slider volumeSlider;
    @FXML
    private JFXSlider songProgressSlider;
    @FXML
    public  BorderPane topBorderPane;
    @FXML
    private TreeView treeView;
    @FXML
    private ImageView currentPlayPauseImageView, repeatImageView, shuffleImageView, previousImageView, nextImageView;
    @FXML
    private ImageView currentAlbumImageView;
    @FXML
    private TabPane previewTabPane;
    @FXML
    private Label totTrackTime, currentTrackTime;

    public static FileUtils fileUtils;
    public static MusicPlayer musicPlayer;

    private final Image playImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/play.png")));
    private final Image pauseImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/pause.png")));

    private final Image repeatUnselectedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_unselected.png")));
    private final Image repeatSelectedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_selected.png")));
    private final Image repeatOneSelectedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_one_selected.png")));

    private final Image shuffleUnselectedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/shuffle_unselected.png")));
    private final Image shuffleSelectedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/shuffle_selected.png")));

    private int repeatStatus = 0;
    private boolean shuffleSelected = false;

    private boolean doRestore = false;

    private double x;
    private double y;

    private TreeItem<String> previousItem = null;

    private String previewArtist;
    private File previewAlbum;

    Font font = new Font(24);

    private FileFilter folderFilter = new FileFilter() {
        public boolean accept(File f)
        {
            return f.isDirectory();
        }
    };

    private FileFilter albumArt = new FileFilter() {
        public boolean accept(File f)
        {
            return f.getName().equals("albumArtImage.png") || f.getName().equals("albumArtIcon.png");
        }
    };

    private FileFilter flacFilter = new FileFilter() {
        public boolean accept(File f)
        {
            return f.getName().endsWith("flac");
        }
    };

    boolean paused = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        fileUtils = App.fileUtils;
        musicPlayer = App.musicPlayer;

        previewTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        previewTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        previewTabPane.setTabMinWidth(125);
        previewTabPane.setTabMaxWidth(125);

        refreshTreeView();
        resetBottomBar();


//        DoubleClick check
        treeView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getButton().equals(MouseButton.PRIMARY)){
                    if(event.getClickCount() == 2){
                        selectAlbum();
                    }
                }
            }
        });

        songProgressSlider.setValueFactory(new Callback<JFXSlider, StringBinding>() {
            @Override
            public StringBinding call(JFXSlider arg0) {
                return Bindings.createStringBinding(new java.util.concurrent.Callable<String>(){
                    @Override
                    public String call() throws Exception {
                        return "";
                    }
                }, songProgressSlider.valueProperty());
            }
        });


        songProgressSlider.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::updateSongPos);

//        volumeSlider.valueProperty().addListener((ObservableValue<? extends Number> arg0, Number arg1, Number arg2) -> {
//            if (musicPlayer.isActive()) {
//                musicPlayer.mediaPlayer.setVolume(volumeSlider.getValue() / 150.0);
//            }
//        });
    }

    public void setSongProgressSliderPos(int currentSongDuration, int totalSongDuration) {
        songProgressSlider.setValue((double) currentSongDuration /totalSongDuration * 100 + 0.00001);
    }

    private Controller getController() {
        return this;
    }

    @FXML
    private void forceGenerateCache() {
        for (File artistFolder : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(folderFilter))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(folderFilter))) {
                try {
                    File albumArtImage = new File(albumFolder, "albumArtImage.png");
                    File albumArtIcon = new File(albumFolder, "albumArtIcon.png");

                    ImageIO.write(fileUtils.getRoundedAlbumImage(Objects.requireNonNull(albumFolder.listFiles())[0], 50), "png", albumArtImage);
                    ImageIO.write(fileUtils.getAlbumImage(Objects.requireNonNull(albumFolder.listFiles())[0], 20, 20), "png", albumArtIcon);

                } catch (IOException e) {
                    System.err.println("Error generating image cache with exception: " + e);
                }

            }
        }
    }

    public void setTotTrackTime(int sec) {
        int min = sec / 60;
        String text;
        if(min < 10){
            text = "0" + String.valueOf(min);
        } else {
            text = String.valueOf(min);
        }

        if((sec % 60) < 10){
            text = text + ":0" + String.valueOf(sec % 60);
        } else {
            text = text + ":" + String.valueOf(sec % 60);
        }
        this.totTrackTime.setText(text);
    }

    public void setCurrentTrackTime(int sec) {
        int min = sec / 60;
        String text;
        if(min < 10){
            text = "0" + String.valueOf(min);
        } else {
            text = String.valueOf(min);
        }

        if((sec % 60) < 10){
            text = text + ":0" + String.valueOf(sec % 60);
        } else {
            text = text + ":" + String.valueOf(sec % 60);
        }
        this.currentTrackTime.setText(text);
    }

    private void generateCache() {
        for (File artistFolder : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(folderFilter))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(folderFilter))) {
                if (albumFolder.listFiles(albumArt).length == 0) {
                    try {

                        File albumArtImage = new File(albumFolder, "albumArtImage.png");
                        File albumArtIcon = new File(albumFolder, "albumArtIcon.png");

                        ImageIO.write(fileUtils.getRoundedAlbumImage(Objects.requireNonNull(albumFolder.listFiles())[0], 50), "png", albumArtImage);
                        ImageIO.write(fileUtils.getAlbumImage(Objects.requireNonNull(albumFolder.listFiles())[0], 20, 20), "png", albumArtIcon);

                    } catch (IOException e) {
                        System.err.println("Error generating image cache with exception: " + e);
                    }
                }
            }
        }
    }

    @FXML
    private void openDirectory() {
        fileUtils.openDirectoryExplorer();
    }

    @FXML
    private void refreshTreeView() {
        generateCache();
        TreeItem<String> rootItem = new TreeItem<>(new File(App.userData.getRootDirectoryPath()).getName(), new ImageView(fileUtils.getFileIcon(new File(App.userData.getRootDirectoryPath()))));

        for (File artistFile : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(folderFilter))) {
            TreeItem<String> artistItem = new TreeItem<>(artistFile.getName(), new ImageView(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/cd.png")))
            ));
            for (File albumFile : Objects.requireNonNull(artistFile.listFiles(folderFilter))) {
                TreeItem<String> albumItem;
                try {
                    File albumArtIcon = new File(albumFile, "albumArtIcon.png");
                    albumItem = new TreeItem<>(albumFile.getName(), new ImageView(new Image(albumArtIcon.toURI().toString())));
                } catch (Exception e) {
                    String tempName = albumFile.getName().replace("-!", "?");
                    albumItem = new TreeItem<>(tempName, new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/warning.png")))));
                }

                artistItem.getChildren().add(albumItem);

            }

            rootItem.getChildren().add(artistItem);

        }
        treeView.setRoot(rootItem);

        treeView.setShowRoot(false);
        treeView.refresh();
    }

    @FXML
    private void selectAlbum() {
        TreeItem<String> selectedItem = (TreeItem<String>) treeView.getSelectionModel().getSelectedItem();
        boolean operationAvailable = true;
        for(Tab tab : previewTabPane.getTabs()) {
            if(tab.getText().equals(selectedItem.getValue())) {
                previewTabPane.getSelectionModel().select(tab);
                operationAvailable = false;
            }
        }
        File albumFile = new File(App.userData.getRootDirectoryPath() + "\\" + selectedItem.getParent().getValue() + "\\" + selectedItem.getValue());

        if (new File(albumFile.getParent()).getName().equals(treeView.getRoot().getValue()) ) {

            operationAvailable = false;
        }

        if(operationAvailable) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("previewTab.fxml"));
            try {
                Node previewNode = loader.load();
                Tab previewTab = new Tab(albumFile.getName());
                previewTab. setContent(previewNode);

                PreviewTabController previewTabController = loader.getController();
                previewTabController.setMainController(this);
                previewTabController.setValues(albumFile, fileUtils.getAlbumFXImage(albumFile, "album"), albumFile.getName(), new File(albumFile.getParent()).getName(), "album");
                previewTabController.refreshSongItemVBox();
                previewTabController.setPlayPauseImageView(true);

                previewTab.setGraphic(new ImageView(fileUtils.getAlbumFXIcon(albumFile, "album")));

                previewTabPane.getTabs().add(previewTab);
                previewTabPane.getSelectionModel().select(previewTab);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    private void topBorderPaneDragged(MouseEvent event) {
        Stage stage = (Stage) topBorderPane.getScene().getWindow();
        stage.setY(event.getScreenY()-y);
        stage.setX(event.getScreenX()-x);
    }

    @FXML
    private void topBorderPanePressed(MouseEvent event) {
        x=event.getSceneX();
        y=event.getSceneY();
    }

    @FXML
    public void maximizeRestoreToggle() {
        if (doRestore) {
            doRestore = false;
            App.restore();
        } else {
            doRestore = true;
            App.maximize();
        }

    }

    @FXML
    public void minimize() {
        App.minimize();
    }

    @FXML
    public void exit() {
        App.exit();
    }

    @FXML
    public void playPauseMedia() {
        if (paused) {;
            musicPlayer.play();
            setCurrentPlayPauseImageViewPaused(!paused);
            paused = !paused;
        } else {
            musicPlayer.pause();
            setCurrentPlayPauseImageViewPaused(!paused);
            paused = !paused;
        }
    }

    public void setCurrentPlayPauseImageViewPaused(Boolean paused) {
        if(paused) {
            currentPlayPauseImageView.setImage(playImage);
        } else  {
            currentPlayPauseImageView.setImage(pauseImage);
        }
    }

    @FXML
    public void resetBottomBar() {
        songLabel.setText("No Song Playing");
        artistLabel.setText("No Artist");
//        currentPlayPauseImageView.setImage();
    }

    public void updateBottomBar() {
        if(musicPlayer.getSongTitle() != null && musicPlayer.getArtistName() != null){
            songLabel.setText(musicPlayer.getSongTitle());
            artistLabel.setText(musicPlayer.getArtistName());
            System.out.println("Setting labels to: " + musicPlayer.getSongTitle() + " by " + musicPlayer.getArtistName());

            try {
                currentAlbumImageView.setImage(fileUtils.getAlbumFXImage(new File(musicPlayer.getCurrentSongFile().getParent()), "album"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            resetBottomBar();
        }
    }

    @FXML
    public void nextMedia() {
        musicPlayer.next();
        setCurrentPlayPauseImageViewPaused(false);
    }

    @FXML
    public void previousMedia() {
        musicPlayer.previous();
        setCurrentPlayPauseImageViewPaused(false);
    }

    @FXML
    public void repeatCycle() {
        repeatStatus = (repeatStatus +1) % 3;
        musicPlayer.setRepeatStatus(repeatStatus);

        switch(repeatStatus) {
            case 0:
                repeatImageView.setImage(repeatUnselectedImage);
                break;
            case 1:
                repeatImageView.setImage(repeatSelectedImage);
                break;
            case 2:
                repeatImageView.setImage(repeatOneSelectedImage);
                break;
        }
    }

    @FXML
    public void shuffleToggle() {
        shuffleSelected = !shuffleSelected;
        musicPlayer.setShuffleStatus(shuffleSelected);

        if(shuffleSelected) {
            shuffleImageView.setImage(shuffleSelectedImage);
        } else {
            shuffleImageView.setImage(shuffleUnselectedImage);
        }
    }

    @FXML
    public void initChangeSongPos() {
        musicPlayer.pauseTimeline();
    }

    @FXML
    public void changeSongPos() {
        musicPlayer.changeSongPos(songProgressSlider.getValue());
    }

    private void updateSongPos(MouseEvent e) {
        setCurrentTrackTime(musicPlayer.getSongPosFromSlider((int) songProgressSlider.getValue()));
    }

    public void pickDirectory() {
        File selectedDirectory = fileUtils.directoryChooser(new Stage(), "Pick a Directory", new File(App.userData.getRootDirectoryPath()).getParent());
        App.userData.setRootDirectoryPath(selectedDirectory.getAbsolutePath());
        if (selectedDirectory != null) {
            fileUtils.changeDirectoryPath(selectedDirectory.getAbsolutePath());
            refreshTreeView();
        }
    }


}

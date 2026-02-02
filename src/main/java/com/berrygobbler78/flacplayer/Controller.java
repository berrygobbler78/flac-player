package com.berrygobbler78.flacplayer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import com.jfoenix.controls.JFXSlider;
import com.pixelduke.window.ThemeWindowManagerFactory;
import com.pixelduke.window.Win11ThemeWindowManager;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    private Slider volumeSlider;
    @FXML
    private JFXSlider songProgressSlider;
    @FXML
    public  BorderPane topBorderPane;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private ImageView currentPlayPauseImageView, repeatImageView, shuffleImageView, previousImageView, nextImageView;
    @FXML
    private ImageView currentAlbumImageView;
    @FXML
    private TabPane previewTabPane;
    @FXML
    private Label totTrackTime, currentTrackTime;
    @FXML
    private TextField searchBar;

    private TreeItem<String> defaultRoot = null;

    private HashMap<TreeItem<String>, String> artistItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, String> albumItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, String> songItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, UserData.Playlist> playlistItemMap = new HashMap<>();

    public static FileUtils fileUtils = App.fileUtils;
    public static MusicPlayer musicPlayer = App.musicPlayer;
    public static UserData userData = App.userData;

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

    private Stage primaryStage = App.getPrimaryStage();

    boolean paused = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previewTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        previewTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        previewTabPane.setTabMinWidth(125);
        previewTabPane.setTabMaxWidth(125);

        refreshTreeView();
        resetBottomBar();


//        DoubleClick check
        treeView.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)){
                if(event.getClickCount() == 2){
                    selectAlbum();
                }
            }
        });

//        Hides JFX thumb
        songProgressSlider.setValueFactory(arg0 -> Bindings.createStringBinding(() -> "", songProgressSlider.valueProperty()));
        songProgressSlider.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::updateSongPos);

//        volumeSlider.valueProperty().addListener((ObservableValue<? extends Number> arg0, Number arg1, Number arg2) -> {
//            if (musicPlayer.isActive()) {
//                musicPlayer.mediaPlayer.setVolume(volumeSlider.getValue() / 150.0);
//            }
//        });
    }

    @FXML
    private void search() {
        treeView.getRoot().getChildren().clear();
        try {
            treeView.getRoot().getChildren().addAll(searchList(searchBar.getText()));
            treeView.getSelectionModel().selectFirst();
        } catch (Exception e) {
            TreeItem<String> root = new TreeItem<>("");
            for(TreeItem<String> item : artistItemMap.keySet()){
                root.getChildren().add(item);
            }
            treeView.setRoot(root);
        }
    }

    private List<TreeItem<String>> searchList(String search) {
        List<TreeItem<String>> foundItems = new ArrayList<>();

        if(search.isEmpty()){
            return null;
        }

        for(TreeItem<String> item : artistItemMap.keySet()){
            if(item.getValue().toLowerCase().contains(search.toLowerCase())){
                foundItems.add(item);
            }
        }

        for(TreeItem<String> item : albumItemMap.keySet()){
            if(item.getValue().toLowerCase().contains(search.toLowerCase())){
                foundItems.add(item);
            }
        }

        for(TreeItem<String> item : songItemMap.keySet()){
            if(item.getValue().toLowerCase().contains(search.toLowerCase())){
                foundItems.add(item);
            }
        }

        for(TreeItem<String> item : playlistItemMap.keySet()){
            if(item.getValue().toLowerCase().contains(search.toLowerCase())){
                foundItems.add(item);
            }
        }

        return foundItems;
    }

    public void setSongProgressSliderPos(int currentSongDuration, int totalSongDuration) {
        songProgressSlider.setValue((double) currentSongDuration /totalSongDuration * 100 + 0.00001);
    }

    @FXML
    private void forceGenerateCache() {
        for (File artistFolder : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(fileUtils.folderFilter))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(fileUtils.folderFilter))) {
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
        this.totTrackTime.setText(formatTime(sec));
    }

    private String formatTime(int sec) {
        int min = sec / 60;
        String text;
        if(min < 10){
            text = "0" + min;
        } else {
            text = String.valueOf(min);
        }

        if((sec % 60) < 10){
            text = text + ":0" + sec % 60;
        } else {
            text = text + ":" + sec % 60;
        }

        return text;
    }

    public void setCurrentTrackTime(int sec) {
        this.currentTrackTime.setText(formatTime(sec));
    }

    private void generateCache() {
        for (File artistFolder : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(fileUtils.folderFilter))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(fileUtils.folderFilter))) {
                if (Objects.requireNonNull(albumFolder.listFiles(fileUtils.albumArtFilter)).length == 0) {
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
    public void refreshTreeView() {
        generateCache();
        artistItemMap.clear();
        albumItemMap.clear();
        songItemMap.clear();

        TreeItem<String> rootItem = new TreeItem<>(new File(App.userData.getRootDirectoryPath()).getName(), new ImageView(fileUtils.getFileIcon(new File(App.userData.getRootDirectoryPath()))));

        for (File artistFile : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(fileUtils.folderFilter))) {
            TreeItem<String> artistItem = new TreeItem<>(artistFile.getName(), new ImageView(Images.getImage(Images.ImageName.CD)));
            for (File albumFile : Objects.requireNonNull(artistFile.listFiles(fileUtils.folderFilter))) {
                TreeItem<String> albumItem = null;
                File albumArtIcon = new File(albumFile, "albumArtIcon.png");
                try{
                    String tempName = albumFile.getName().replaceAll("#00F3", "?");
                    albumItem = new TreeItem<String>(tempName, new ImageView(new Image(albumArtIcon.toURI().toString())));
                } catch(Exception e) {
                    String tempName = albumFile.getName().replaceAll("#00F3", "?");
                    albumItem = new TreeItem<>(tempName, new ImageView(Images.getImage(Images.ImageName.WARNING)));
                }
                int songNum = 1;
                for(File songFile : Objects.requireNonNull(albumFile.listFiles(fileUtils.flacFilter))) {
                    TreeItem<String> songItem = null;
                    songItem = new TreeItem<>(songNum + ". " + fileUtils.getSongTitle(songFile), new ImageView(new Image(albumArtIcon.toURI().toString())));
                    songNum++;
                    songItemMap.put(songItem, songFile.getAbsolutePath());
                }

                albumItemMap.put(albumItem, albumFile.getAbsolutePath());
                artistItem.getChildren().add(albumItem);

            }
            artistItemMap.put(artistItem, artistFile.getAbsolutePath());
            rootItem.getChildren().add(artistItem);

        }

        TreeItem<String> userItem = new TreeItem<>("- User -", new ImageView(Images.getImage(Images.ImageName.CD)));
        if(userData.getPlaylists() != null || userData.getPlaylists().isEmpty()) {
            for(UserData.Playlist playlist : Objects.requireNonNull(App.userData.getPlaylists())) {
                TreeItem<String> playlistItem = new TreeItem<>(playlist.getName());

                playlistItemMap.put(playlistItem, playlist);
                userItem.getChildren().add(playlistItem);
            }
        }
        rootItem.getChildren().add(userItem);


        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.refresh();

//        rootItem.getChildren().sort(Comparator.comparing(t->t.getValue().length()));
    }

    @FXML
    private void newPlaylist() {
        Win11ThemeWindowManager themeWindowManager = (Win11ThemeWindowManager) ThemeWindowManagerFactory.create();
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("newPlaylistWindow.fxml"));
        try {
            AnchorPane playlistPane = loader.load();
            PopupWindowsController controller = loader.getController();
            controller.setValues(dialog, this);

            Scene dialogScene = new Scene(playlistPane, 300, 100);
            dialogScene.setFill(Color.TRANSPARENT);

            dialog.setTitle("Create Playlist");
            dialog.initStyle(StageStyle.UNIFIED);
            dialog.setResizable(false);
            dialog.setScene(dialogScene);
            dialog.show();

            themeWindowManager.setDarkModeForWindowFrame(dialog, true);
            themeWindowManager.setWindowBackdrop(dialog, Win11ThemeWindowManager.Backdrop.ACRYLIC);
        } catch (Exception e) {
            System.err.println("Error opening playlist window: " + e.getMessage());
        }
    }

    @FXML
    private void selectAlbum() {
        TreeItem<String> selectedItem = (TreeItem<String>) treeView.getSelectionModel().getSelectedItem();
        if(selectedItem.getParent().getValue().equals("- User -")) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("previewTab.fxml"));
            try {
                Node previewNode = loader.load();
                Tab previewTab = new Tab(selectedItem.getValue(), previewNode);
                previewTab.setContent(previewNode);

                PreviewTabController previewTabController = loader.getController();
                previewTabController.setMainController(this);
                previewTabController.setPlaylistValues(playlistItemMap.get(selectedItem));
                previewTabController.refreshSongItemVBox();
                previewTabController.setPlayPauseImageViewPaused(true);

//                previewTab.setGraphic(new ImageView(fileUtils.getAlbumFXIcon(albumFile, "album")));

                previewTabPane.getTabs().add(previewTab);
                previewTabPane.getSelectionModel().select(previewTab);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if(artistItemMap.containsKey(selectedItem)) {
//            Artist action
        } else if (albumItemMap.containsKey(selectedItem)) {
            boolean operationAvailable = true;
            for (Tab tab : previewTabPane.getTabs()) {
                if (tab.getText().equals(selectedItem.getValue())) {
                    previewTabPane.getSelectionModel().select(tab);
                    operationAvailable = false;
                }
            }

            File albumFile = new File(albumItemMap.get(selectedItem));

            if (new File(albumFile.getParent()).getName().equals(treeView.getRoot().getValue())) {
                operationAvailable = false;
            }

            if (operationAvailable) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("previewTab.fxml"));
                try {
                    Node previewNode = loader.load();
                    Tab previewTab = new Tab(albumFile.getName());
                    previewTab.setContent(previewNode);

                    PreviewTabController previewTabController = loader.getController();
                    previewTabController.setMainController(this);
                    previewTabController.setAlbumValues(albumFile);
                    previewTabController.refreshSongItemVBox();
                    previewTabController.setPlayPauseImageViewPaused(true);

                    previewTab.setGraphic(new ImageView(fileUtils.getAlbumFXIcon(albumFile, "album")));

                    previewTabPane.getTabs().add(previewTab);
                    previewTabPane.getSelectionModel().select(previewTab);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if(songItemMap.containsKey(selectedItem)) {
            musicPlayer.loadSong(songItemMap.get(selectedItem));
            musicPlayer.play();
        } else {
            System.err.println("Unknown item selected: " + selectedItem.getValue());
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
        try {
            App.exit();
        } catch (InterruptedException e) {
            System.err.println("Couldn't exit with error: " + e.getMessage());
        }
    }

    @FXML
    public void playPauseMedia() {
        if (paused) {
            musicPlayer.play();
        } else {
            musicPlayer.pause();
        }
        paused = !paused;
        setCurrentPlayPauseImageViewPaused(paused);
    }

    public void setCurrentPlayPauseImageViewPaused(Boolean paused) {
        if(paused) {
            currentPlayPauseImageView.setImage(Images.getImage(Images.ImageName.PLAY));
        } else  {
            currentPlayPauseImageView.setImage(Images.getImage(Images.ImageName.PAUSE));
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

            currentAlbumImageView.setImage(fileUtils.getAlbumFXImage(new File(musicPlayer.getCurrentSongPath()).getParentFile(), ParentType.ALBUM));

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

        switch(repeatStatus) {
            case 0:
                repeatImageView.setImage(repeatUnselectedImage);
                musicPlayer.setRepeatStatus(RepeatStatus.OFF);
                break;
            case 1:
                repeatImageView.setImage(repeatSelectedImage);
                musicPlayer.setRepeatStatus(RepeatStatus.REPEAT_ALL);
                break;
            case 2:
                repeatImageView.setImage(repeatOneSelectedImage);
                musicPlayer.setRepeatStatus(RepeatStatus.OFF);
                break;
        }
    }

    @FXML
    public void shuffleToggle() {
        shuffleSelected = !shuffleSelected;
        musicPlayer.setShuffleStatus(shuffleSelected);

        if(shuffleSelected) {
            shuffleImageView.setImage(shuffleSelectedImage);
            musicPlayer.shuffle();
        } else {
            shuffleImageView.setImage(shuffleUnselectedImage);
            musicPlayer.refreshAlbumSongQueue();
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
        refreshTreeView();
    }


}

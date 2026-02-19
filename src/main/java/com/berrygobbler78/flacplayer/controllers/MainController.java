package com.berrygobbler78.flacplayer.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import com.berrygobbler78.flacplayer.App;
import com.berrygobbler78.flacplayer.util.MusicPlayer;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.userdata.References;
import com.berrygobbler78.flacplayer.util.Constants.*;
import com.berrygobbler78.flacplayer.util.FileUtils.*;

import com.berrygobbler78.flacplayer.util.FileUtils;
import com.jfoenix.controls.JFXSlider;
import com.pixelduke.window.ThemeWindowManagerFactory;
import com.pixelduke.window.Win11ThemeWindowManager;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
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

import static com.berrygobbler78.flacplayer.util.FileUtils.getCoverIcon;

public class MainController implements  Initializable {
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

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

    private HashMap<TreeItem<String>, String> artistItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, String> albumItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, String> songItemMap = new HashMap<>();
    private HashMap<TreeItem<String>, Playlist> playlistItemMap = new HashMap<>();

    private TreeItem<String> userItem = null;

    public static HashMap<Tab, PreviewTabController> tabControllerMap = new HashMap<>();

    private final Stage primaryStage = App.getPrimaryStage();

    boolean paused = true;

    private MusicPlayer musicPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        musicPlayer = new MusicPlayer(this);

        previewTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        previewTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        previewTabPane.setTabMaxWidth(125);

        refreshTreeView();
        resetBottomBar();

//        DoubleClick check
        treeView.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)){
                if(event.getClickCount() == 2){
                    selectPreview();
                }
            }
        });

       // Hides JFX thumb
        songProgressSlider.setValueFactory(_ -> Bindings.createStringBinding(() -> "", songProgressSlider.valueProperty()));
        songProgressSlider.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::updateSongPos);

       volumeSlider.valueProperty().addListener((ObservableValue<? extends Number> _, Number _, Number _) -> {
           if (musicPlayer.getMusicPlayerState() != MusicPlayer.MUSIC_PLAYER_STATUS.STOPPED) {
               musicPlayer.setVolume((float) (volumeSlider.getValue() / 150.0));
           }
       });
    }

    public void removeTab(PreviewTabController previewTabController){
        for(Map.Entry<Tab, PreviewTabController> previewTabController1 : tabControllerMap.entrySet()){
            if(previewTabController1.getValue() == previewTabController){
                previewTabPane.getTabs().remove(previewTabController1.getKey());
            }
        }
    }

    @FXML
    private void search() {
        treeView.getRoot().getChildren().clear();
        try {
            treeView.getRoot().getChildren().addAll(Objects.requireNonNull(searchList(searchBar.getText())));
            treeView.getSelectionModel().selectFirst();
        } catch (Exception e) {
            TreeItem<String> root = new TreeItem<>("");
            root.getChildren().add(userItem);

            TreeMap<String, TreeItem<String>> map = new TreeMap<>();
            for(TreeItem<String> item : artistItemMap.keySet()){
                map.put(item.getValue(), item);
            }

            for(Map.Entry<String, TreeItem<String>> item : map.entrySet()){
                item.getValue().setExpanded(false);
                root.getChildren().add(item.getValue());
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
                item.setExpanded(false);
                foundItems.add(item);
            }
        }

        for(TreeItem<String> item : albumItemMap.keySet()){
            if(item.getValue().toLowerCase().contains(search.toLowerCase())){
                item.setExpanded(false);
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

        if(userItem.getValue().toLowerCase().contains(search.toLowerCase())){
            foundItems.add(userItem);
        }

        return foundItems;
    }

    public void setSongProgressSliderPos(int currentSongDuration, int totalSongDuration) {
        songProgressSlider.setValue((double) currentSongDuration /totalSongDuration * 100 + 0.00001);
    }

    @FXML
    private void forceGenerateCache() {
        for (File artistFolder : Objects.requireNonNull(new File(App.getReferences().getRootDirectoryPath()).listFiles(FileUtils.getFileFilter(FILTER_TYPE.FOLDER)))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(FileUtils.getFileFilter(FILTER_TYPE.FOLDER)))) {
                for(File coverImage : Objects.requireNonNull(albumFolder.listFiles(FileUtils.getFileFilter(FILTER_TYPE.COVER_IMAGE)))){
                    if(coverImage.delete()) {
                        LOGGER.fine(String.format("[%s] deleted", coverImage.getName()));
                    } else {
                        LOGGER.warning(String.format("[%s] could not be deleted", coverImage.getName()));
                    }
                }
                for(File iconImage : Objects.requireNonNull(albumFolder.listFiles(FileUtils.getFileFilter(FILTER_TYPE.COVER_IMAGE)))){
                    if(iconImage.delete()) {
                        LOGGER.fine(String.format("[%s] deleted", iconImage.getName()));
                    } else {
                        LOGGER.warning(String.format("[%s] could not be deleted", iconImage.getName()));
                    }
                }
            }
        }

        FileUtils.refreshAllArt();
    }

    public void setTotTrackTime(int sec) {
        this.totTrackTime.setText(formatTime(sec));
    }

    private String formatTime(int sec) {
        String text;
        int min = sec / 60;

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

    @FXML
    private void openDirectory() {
        FileUtils.openFileExplorer(App.getReferences().getRootDirectoryPath());
    }

    @FXML
    public void refreshTreeView() {
        FileUtils.refreshAllArt();

        artistItemMap.clear();
        albumItemMap.clear();
        songItemMap.clear();

        TreeItem<String> rootItem = new TreeItem<>(new File(App.getReferences().getRootDirectoryPath()).getName());

        TreeItem<String> userItem = new TreeItem<>(App.getReferences().getUserName(), new ImageView(IMAGES.USER.get()));
        if(App.getReferences().getPlaylists() != null || !App.getReferences().getPlaylists().isEmpty()) {
            for(Playlist playlist : Objects.requireNonNull(App.getReferences().getPlaylists())) {
                Image playlistIcon;

                try {
                    playlistIcon = getCoverIcon(playlist.getPath(), FILE_TYPE.PLAYLIST);
                } catch (Exception e) {
                    LOGGER.warning("Could not get cover image for playlist: " + playlist.getPath());
                    playlistIcon = IMAGES.WARNING.get();
                }

                TreeItem<String> playlistItem = new TreeItem<>(playlist.getName(), new ImageView(playlistIcon));
                playlistItemMap.put(playlistItem, playlist);

                userItem.getChildren().add(playlistItem);
            }

            this.userItem = userItem;
            rootItem.getChildren().add(userItem);
        }

        for (File artistFile : Objects.requireNonNull(new File(App.getReferences().getRootDirectoryPath()).listFiles(FileUtils.getFileFilter(FILTER_TYPE.FOLDER)))) {
            TreeItem<String> artistItem = new TreeItem<>(artistFile.getName(), new ImageView(IMAGES.CD.get()));
            for (File albumFile : Objects.requireNonNull(artistFile.listFiles(FileUtils.getFileFilter(FILTER_TYPE.FOLDER)))) {
                TreeItem<String> albumItem;

                Image albumArtIcon;

                try {
                    albumArtIcon = getCoverIcon(albumFile.getAbsolutePath(), FILE_TYPE.ALBUM);
                } catch (IOException e) {
                    LOGGER.warning("Could not get cover image for album: " + albumFile.getAbsolutePath());
                    albumArtIcon = IMAGES.WARNING.get();
                }

                String tempName = albumFile.getName().replaceAll("#00F3", "?");

                albumItem = new TreeItem<>(tempName, new ImageView(albumArtIcon));

                int songNum = 1;
                for(File songFile : Objects.requireNonNull(albumFile.listFiles(FileUtils.getFileFilter(FILTER_TYPE.FLAC)))) {
                    TreeItem<String> songItem =
                            new TreeItem<>(songNum + ". " + FileUtils.getSongTitle(songFile.getAbsolutePath()), new ImageView(albumArtIcon));
                    songItemMap.put(songItem, songFile.getAbsolutePath());

                    songNum++;
                }

                albumItemMap.put(albumItem, albumFile.getAbsolutePath());
                artistItem.getChildren().add(albumItem);

            }

            artistItemMap.put(artistItem, artistFile.getAbsolutePath());
            rootItem.getChildren().add(artistItem);
        }

        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);

        treeView.getRoot().getChildren().sort((obj1, obj2) -> {

            if (Objects.equals(obj1.getValue(), userItem.getValue()) || Objects.equals(obj2.getValue(), userItem.getValue())) {
                return 0;
            } else {
                return obj1.getValue().compareTo(obj2.getValue());
            }
        });

        treeView.refresh();
    }

    @FXML
    private void newPlaylist() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        FXMLLoader loader = new FXMLLoader();
        AnchorPane playlistPane = null;

        try{
            loader.setLocation(Path.of(FXML_PATHS.NEW_PLAYLIST.get()).toUri().toURL());
            playlistPane = loader.load();
        } catch (IOException e) {
            LOGGER.severe("Could not load playlist window: " + e.getMessage());
        }
        PopupWindowsController controller = loader.getController();
        controller.setValues(dialog, this);

        Scene dialogScene = new Scene(playlistPane, 300, 100);
        dialogScene.setFill(Color.TRANSPARENT);

        dialog.setTitle("Create Playlist");
        dialog.initStyle(StageStyle.UNIFIED);
        dialog.getIcons().add(IMAGES.BERRIES.get());
        dialog.setResizable(false);
        dialog.setScene(dialogScene);
        dialog.show();

        switch (App.getCurrentOS()) {
            case WINDOWS_11 -> {
                Win11ThemeWindowManager themeWindowManager = (Win11ThemeWindowManager) ThemeWindowManagerFactory.create();
                themeWindowManager.setDarkModeForWindowFrame(dialog, true);
                themeWindowManager.setWindowBackdrop(dialog, Win11ThemeWindowManager.Backdrop.ACRYLIC);
            }
        }
    }

    @FXML
    private void selectPreview() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();

        if(selectedItem == null) return;

        for (Tab tab : previewTabPane.getTabs()) {
            if (tab.getText().equals(selectedItem.getValue())) {
                previewTabPane.getSelectionModel().select(tab);
                tabControllerMap.get(tab).refreshSongItemVBox();
                return;
            }
        }

        if(playlistItemMap.containsKey(selectedItem)) {
            FXMLLoader loader = new FXMLLoader();

            try {
                loader.setLocation(Path.of(FXML_PATHS.PREVIEW_TAB.get()).toUri().toURL());
            } catch (MalformedURLException e) {
                LOGGER.severe("Could not create preview playlist: " + e.getMessage());
                return;
            }

            Node previewNode;

            try{
                previewNode = loader.load();
            } catch (IOException e) {
                LOGGER.severe("Could not load preview fxml: " + e.getMessage());
                return;
            }

            Tab previewTab = new Tab(selectedItem.getValue(), previewNode);
            previewTab.setContent(previewNode);

            try {
                previewTab.setGraphic(new ImageView(getCoverIcon(playlistItemMap.get(selectedItem).getPath(), FILE_TYPE.PLAYLIST)));
            } catch (IOException e) {
                previewTab.setGraphic(new ImageView(IMAGES.WARNING.get()));

                LOGGER.warning("Could not get cover image for preview tab");
            }

            PreviewTabController previewTabController = loader.getController();
            previewTabController.setMainController(this);
            previewTabController.setPlaylistValues(playlistItemMap.get(selectedItem));
            previewTabController.refreshSongItemVBox();
            previewTabController.setPaused(true);

            previewTabPane.getTabs().add(previewTab);
            previewTabPane.getSelectionModel().select(previewTab);

            previewTab.setOnSelectionChanged(event -> {
                tabControllerMap.get(previewTab).refreshSongItemVBox();
            });

            if(!tabControllerMap.containsKey(previewTab)) {
                tabControllerMap.put(previewTab, previewTabController);
            }

        } else if(artistItemMap.containsKey(selectedItem)) {
            // TODO: Artist page
        } else if (albumItemMap.containsKey(selectedItem)) {
            boolean operationAvailable = true;

            File albumFile = new File(albumItemMap.get(selectedItem));

            if (new File(albumFile.getParent()).getName().equals(treeView.getRoot().getValue())) {
                operationAvailable = false;
            }

            if (operationAvailable) {
                FXMLLoader loader = new FXMLLoader();

                try {
                    loader.setLocation(Path.of(FXML_PATHS.PREVIEW_TAB.get()).toUri().toURL());
                } catch (MalformedURLException e) {
                    LOGGER.severe("Could not create preview playlist: " + e.getMessage());
                    return;
                }

                Node previewNode;

                try{
                    previewNode = loader.load();
                } catch (IOException e) {
                    LOGGER.severe("Could not load preview fxml: " + e.getMessage());
                    return;
                }

                Tab previewTab = new Tab(albumFile.getName());
                previewTab.setContent(previewNode);

                PreviewTabController previewTabController = loader.getController();
                previewTabController.setMainController(this);
                previewTabController.setAlbumValues(albumFile);
                previewTabController.refreshSongItemVBox();
                previewTabController.setPaused(true);

                try {

                    previewTab.setGraphic(new ImageView(getCoverIcon(albumFile.getAbsolutePath(), FILE_TYPE.ALBUM)));
                } catch (IOException e) {
                    previewTab.setGraphic(new ImageView(IMAGES.WARNING.get()));

                    LOGGER.warning("Could not get cover image for preview tab");
                }

                previewTabPane.getTabs().add(previewTab);
                previewTabPane.getSelectionModel().select(previewTab);

                if(!tabControllerMap.containsKey(previewTab)) {
                    tabControllerMap.put(previewTab, previewTabController);
                }
            }
        } else if(songItemMap.containsKey(selectedItem)) {
            musicPlayer.loadSong(songItemMap.get(selectedItem), true);
        } else {
            LOGGER.warning("Unknown item selected: " + selectedItem.getValue());
        }
    }

    @FXML
    public void playPauseMedia() {
        if (musicPlayer.getMusicPlayerState() == MusicPlayer.MUSIC_PLAYER_STATUS.PLAYING) {
            musicPlayer.pause();
        } else {
            musicPlayer.play();
        }
    }

    public void setPaused(Boolean paused) {
        if(paused) {
            currentPlayPauseImageView.setImage(IMAGES.PLAY.get());
        } else  {
            currentPlayPauseImageView.setImage(IMAGES.PAUSE.get());
        }

        this.paused = paused;
    }

    @FXML
    public void resetBottomBar() {
        songLabel.setText("No Song Playing");
        artistLabel.setText("No Artist");
    }

    public void updateBottomBar() {
        songLabel.setText(FileUtils.getSongTitle(musicPlayer.getCurrentSongPath()));
        artistLabel.setText(FileUtils.getSongArtist(musicPlayer.getCurrentSongPath()));

        switch (musicPlayer.getMusicPlayerState()) {
            case MusicPlayer.MUSIC_PLAYER_STATUS.PLAYING:
                setPaused(false);
                break;
            default:
                setPaused(true);
                break;
        }

        try{
            currentAlbumImageView.setImage(FileUtils.getCoverImage(musicPlayer.getCurrentSongPath(), FILE_TYPE.SONG));
        } catch (Exception e) {
            System.err.println("Error loading song image: " + e.getMessage());
        }
    }

    @FXML
    public void nextMedia() {
        musicPlayer.next();
    }

    @FXML
    public void previousMedia() {
        musicPlayer.previous();
    }

    @FXML
    public void repeatCycle() {
        musicPlayer.cycleRepeatStatus();
    }

    public void updateRepeatButton() {
        switch(musicPlayer.getRepeatStatus()) {
            case OFF:
                repeatImageView.setImage(IMAGES.REPEAT_UNSELECTED.get());
                break;
            case REPEAT_ALL:
                repeatImageView.setImage(IMAGES.REPEAT_ALL.get());
                break;
            case REPEAT_ONE:
                repeatImageView.setImage(IMAGES.REPEAT_ONE.get());
                break;
        }
    }
    @FXML
    public void shuffleToggle() {
        musicPlayer.toggleShuffleStatus();
    }

    public void updateShuffleButton() {
        switch(musicPlayer.getShuffleStatus()) {
            case OFF -> shuffleImageView.setImage(IMAGES.SHUFFLE_UNSELECTED.get());
            case SHUFFLE -> shuffleImageView.setImage(IMAGES.SHUFFLE_SELECTED.get());
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
        File selectedDirectory = FileUtils.openDirectoryChooser(new Stage(), "Pick a Directory", new File(App.getReferences().getRootDirectoryPath()).getParent());
        App.getReferences().setRootDirectoryPath(selectedDirectory.getAbsolutePath());
        refreshTreeView();
    }

    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }


}

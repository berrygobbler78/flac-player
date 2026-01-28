package com.berrygobbler78.flacplayer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import com.jfoenix.controls.JFXSlider;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
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
import javafx.stage.Stage;

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

    private TreeView<String> defaultTreeView = new TreeView<>();

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

    private final FileFilter folderFilter = File::isDirectory;

    private ArrayList<TreeItem<String>> supaList = new ArrayList<>();
    

    private final FileFilter albumArt = new FileFilter() {
        public boolean accept(File f)
        {
            return f.getName().equals("albumArtImage.png") || f.getName().equals("albumArtIcon.png");
        }
    };

    private final FileFilter flacFilter = f -> f.getName().endsWith("flac");

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
        treeView.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)){
                if(event.getClickCount() == 2){
                    selectAlbum();
                }
            }
        });

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
        treeView.getRoot().getChildren().addAll(searchList(searchBar.getText(), defaultTreeView));
    }

    private List<TreeItem<String>> searchList(String search, TreeView<String> treeView) {
        List<TreeItem<String>> foundItems = new ArrayList<>();
        for(int i = 0; i < supaList.size(); i++){
            if(supaList.get(i).getValue().toLowerCase().contains(search.toLowerCase())){
                foundItems.add(supaList.get(i));
            }
        }

        return foundItems;
    }

    public void setSongProgressSliderPos(int currentSongDuration, int totalSongDuration) {
        songProgressSlider.setValue((double) currentSongDuration /totalSongDuration * 100 + 0.00001);
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
        for (File artistFolder : Objects.requireNonNull(new File(App.userData.getRootDirectoryPath()).listFiles(folderFilter))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(folderFilter))) {
                if (Objects.requireNonNull(albumFolder.listFiles(albumArt)).length == 0) {
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
            System.out.println(artistItem.getValue());
            for (File albumFile : Objects.requireNonNull(artistFile.listFiles(folderFilter))) {
                TreeItem<String> albumItem = null;
                for(File f : albumFile.listFiles()){
                    try{
                        File albumArtIcon = new File(albumFile, "albumArtIcon.png");
                        String tempName = albumFile.getName().replaceAll("#00F3", "?");
                        albumItem = new TreeItem<String>(tempName, new ImageView(new Image(albumArtIcon.toURI().toString())));
                    } catch(Exception e) {
                        String tempName = albumFile.getName().replaceAll("#00F3", "?");
                        albumItem = new TreeItem<>(tempName, new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/warning.png")))));
                    }
                }

                artistItem.getChildren().add(albumItem);

            }
            supaList.add(artistItem);
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
                currentAlbumImageView.setImage(fileUtils.getAlbumFXImage(new File(new File(musicPlayer.getCurrentSongPath()).getParent()), "album"));
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

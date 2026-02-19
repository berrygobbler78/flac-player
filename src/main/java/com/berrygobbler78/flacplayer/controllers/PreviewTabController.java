package com.berrygobbler78.flacplayer.controllers;

import com.berrygobbler78.flacplayer.*;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.util.Constants;
import com.berrygobbler78.flacplayer.util.FileUtils;
import com.berrygobbler78.flacplayer.util.Constants.FXML_PATHS;

import com.berrygobbler78.flacplayer.util.MusicPlayer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class PreviewTabController implements Initializable {

    @FXML
    private ImageView imageView, playPauseImageView;
    @FXML
    private Label titleLabel, artistLabel;
    @FXML
    private VBox songItemVBox, vbox;
    @FXML
    private MenuButton optionsMenuButton;

    private Constants.PARENT_TYPE type;

    private File parentFile;
    private Playlist playlist;

    private MainController controller;
    private MusicPlayer musicPlayer;

    private String[] songList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void playPreview() {
        if(type == Constants.PARENT_TYPE.ALBUM) {
            musicPlayer.setParentType(null, parentFile);
            musicPlayer.setPreviewTabController(this);
            musicPlayer.playFirstSong();
        } else if(type == Constants.PARENT_TYPE.PLAYLIST) {
            musicPlayer.setParentType(playlist,null);
            musicPlayer.setPreviewTabController(this);
            musicPlayer.playFirstSong();
        }
    }

    @FXML
    private void addToQueue() {
        musicPlayer.addToUserQueue(parentFile.getAbsolutePath());
    }

    public void setAlbumValues(File file) {
        this.parentFile = file;

        type = Constants.PARENT_TYPE.ALBUM;

        imageView.setImage(FileUtils.getCoverImage(file.getAbsolutePath(), FileUtils.FILE_TYPE.ALBUM));
        titleLabel.setText(file.getName());
        artistLabel.setText(file.getParentFile().getName());
    }

    public void setPlaylistValues(Playlist playlist) {
        this.playlist = playlist;

        type = Constants.PARENT_TYPE.PLAYLIST;

        imageView.setImage(FileUtils.getCoverImage(playlist.getPath(), FileUtils.FILE_TYPE.PLAYLIST));
        titleLabel.setText(playlist.getName());
        artistLabel.setText(App.getReferences().getUserName());

        MenuItem deletePlaylistItem = new MenuItem("Delete Playlist");
        deletePlaylistItem.setOnAction(_ -> {
            App.getReferences().removePlaylist(playlist);
            controller.removeTab(this);
            controller.refreshTreeView();
        });

        optionsMenuButton.getItems().addAll(deletePlaylistItem);
    }

    public void setPaused(boolean paused) {
        if(paused) {
            playPauseImageView.setImage(Constants.IMAGES.PLAY.get());
        } else  {
            playPauseImageView.setImage(Constants.IMAGES.PAUSE.get());
        }
    }

    public void refreshSongItemVBox() {

        songItemVBox.getChildren().clear();

        Task<Void> refreshVbox = new Task<>() {
            @Override
            protected Void call() {
                if(type == Constants.PARENT_TYPE.ALBUM) {
                    try {;

                        ArrayList<File> songListArray =
                                new ArrayList<>(Arrays.asList(Objects.requireNonNull(parentFile.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)))));
                        Collections.sort(songListArray);

                        Node[] nodes = new Node[songListArray.size()];

                        for(int i = 0; i < nodes.length; i++){
                            FXMLLoader loader = new FXMLLoader();
                            loader.setLocation(new File(FXML_PATHS.SONG_ITEM.get()).toURI().toURL());
                            nodes[i] = loader.load();

                            SongItemController songItemController = loader.getController();

                            File song = songListArray.get(i);

                            songItemController.setItemInfo(
                                    i + 1,
                                    song.getAbsolutePath(),
                                    Constants.PARENT_TYPE.ALBUM
                            );

                            songItemController.setControllers(controller, PreviewTabController.this);

                            int finalI = i;
                            Platform.runLater(() -> {
                                songItemVBox.getChildren().add(nodes[finalI]);
                            });
                        }
                    } catch (IOException e) {
                        System.err.println("Song list failed with exception: " + e);
                    }
                } else if(type == Constants.PARENT_TYPE.PLAYLIST) {
                    try {
                        int nodesLength = playlist.getSongList().size();

                        Node[] nodes = new Node[nodesLength];

                        for(int i = 0; i < nodes.length; i++){
                            FXMLLoader loader = new FXMLLoader();
                            loader.setLocation(Path.of("src/main/resources/com/berrygobbler78/flacplayer/fxml/songItem.fxml").toUri().toURL());
                            nodes[i] = loader.load();

                            SongItemController songItemController = loader.getController();

                            File song = new File(playlist.getSongList().get(i));

                            songItemController.setItemInfo(
                                    i + 1,
                                    song.getAbsolutePath(),
                                    Constants.PARENT_TYPE.PLAYLIST
                            );

                            songItemController.setControllers(controller, PreviewTabController.this);

                            int finalI = i;
                            Platform.runLater(() -> {
                                songItemVBox.getChildren().add(nodes[finalI]);
                            });
                        }
                    } catch (IOException e) {
                        System.err.println("Song list failed with exception: " + e);
                    }

                }
                return null;
            }
        };

        new Thread(refreshVbox).start();
    }

    public void setMainController(MainController controller) {
        this.controller = controller;
        musicPlayer = controller.getMusicPlayer();
    }

    public Constants.PARENT_TYPE getType() {
        return type;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

}

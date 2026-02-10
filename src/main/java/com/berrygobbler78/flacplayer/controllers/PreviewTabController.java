package com.berrygobbler78.flacplayer.controllers;

import com.berrygobbler78.flacplayer.*;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.util.FileUtils;
import com.berrygobbler78.flacplayer.Constants.FXML_PATHS;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void playPreview() {
        if(type == Constants.PARENT_TYPE.ALBUM) {
            MusicPlayer.setParentTypeAlbum(parentFile.getPath());
            MusicPlayer.playFirstSong();
            MusicPlayer.setPreviewTabController(this);
        } else if(type == Constants.PARENT_TYPE.PLAYLIST) {
            MusicPlayer.setParentTypePlaylist(playlist);
            MusicPlayer.playFirstSong();
            MusicPlayer.setPreviewTabController(this);
        }
    }

    @FXML
    private void addToQueue() {
        MusicPlayer.addToUserQueue(parentFile.getAbsolutePath());
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
        artistLabel.setText(App.references.getUserName());

        MenuItem deletePlaylistItem = new MenuItem("Delete Playlist");
        deletePlaylistItem.setOnAction(_ -> {
            App.references.removePlaylist(playlist);
            controller.removeTab(this);
            controller.refreshTreeView();
        });

        optionsMenuButton.getItems().addAll(deletePlaylistItem);
    }

    public void setPlayPauseImageViewPaused(boolean paused) {
        if(paused) {
            playPauseImageView.setImage(Constants.IMAGES.PLAY.get());
        } else  {
            playPauseImageView.setImage(Constants.IMAGES.PAUSE.get());
        }
    }

    public void refreshSongItemVBox() {
        songItemVBox.getChildren().clear();

        if(type == Constants.PARENT_TYPE.ALBUM) {
            try {;

                ArrayList<File> songListArray =
                        new ArrayList<>(Arrays.asList(Objects.requireNonNull(parentFile.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)))));
                Collections.sort(songListArray);

                for(File f: songListArray) {
                    System.out.println(f.getName());
                }

                Node[] nodes = new Node[songListArray.size()];

                for(int i = 0; i < nodes.length; i++){
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(new File(FXML_PATHS.SONG_ITEM.get()).toURI().toURL());
                    nodes[i] = loader.load();

                    SongItemController songItemController = loader.getController();

                    File song = songListArray.get(i);

                    System.out.println(song.getAbsolutePath() );

                    songItemController.setItemInfo(
                            i + 1,
                            song.getAbsolutePath(),
                            Constants.PARENT_TYPE.ALBUM
                    );

                    songItemController.setControllers(controller, this);

                    songItemVBox.getChildren().add(nodes[i]);
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

                    songItemController.setControllers(controller, this);

                    songItemVBox.getChildren().add(nodes[i]);
                }
            } catch (IOException e) {
                System.err.println("Song list failed with exception: " + e);
            }

        }
    }

    public void setMainController(MainController controller) {
        this.controller = controller;
    }

    public Constants.PARENT_TYPE getType() {
        return type;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

}

package com.berrygobbler78.flacplayer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.berrygobbler78.flacplayer.App.fileUtils;

public class PreviewTabController implements Initializable {

    @FXML
    private ImageView albumImageView, playPauseImageView;
    @FXML
    private Label albumLabel, artistLabel;
    @FXML
    private VBox songItemVBox, vbox;

    private String type;
    private File file;
    private Controller controller;
    private MusicPlayer musicPlayer;


    private double font;
    private final int error = 5;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.musicPlayer = App.musicPlayer;
    }

    @FXML
    private void playPreview() {
        controller.setCurrentPlayPauseImageViewPaused(false);

        musicPlayer.setDirectoryPath(file.getPath(), ParentType.ALBUM);
        musicPlayer.playFirstSong();
        musicPlayer.refreshAlbumSongQueue();
        musicPlayer.SetPreviewTabController(this);
        musicPlayer.play();
    }

    @FXML
    private void addToQueue() {
        musicPlayer.addToUserQueue(file);
    }

    public void setValues(File file, Image albumImage, String albumName, String artistName, String albumOrPlaylist) {
        this.file = file;
        albumImageView.setImage(albumImage);
        albumLabel.setText(albumName);
        artistLabel.setText(artistName);
        type = albumOrPlaylist;
    }

    public void setPlayPauseImageViewPaused(boolean paused) {
        if(paused) {
            playPauseImageView.setImage(Images.getImage(Images.ImageName.PLAY));
        } else  {
            playPauseImageView.setImage(Images.getImage(Images.ImageName.PAUSE));
        }
    }

    public void refreshSongItemVBox() {
        songItemVBox.getChildren().clear();
        try {
            int nodesLength = 0;

            for(File fileToCheck : Objects.requireNonNull(file.listFiles(fileUtils.flacFilter))){
                nodesLength ++;
            }

            Node[] nodes = new Node[nodesLength];

            for(int i = 0; i < nodes.length; i++){
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("songItem.fxml"));
                nodes[i] = loader.load();

                SongItemController songItemController = loader.getController();

                File song = Objects.requireNonNull(file.listFiles())[i];

                songItemController.setItemInfo(
                        i + 1,
                        fileUtils.getSongTitle(song),
                        fileUtils.getSongArtist(song),
                        fileUtils.getAlbumFXImage(file, type),
                        song
                );

                songItemController.setMainController(controller);

                songItemVBox.getChildren().add(nodes[i]);
            }
        } catch (IOException e) {
            System.err.println("Song list failed with exception: " + e);
        }
    }

    public void setMainController(Controller controller) {
        this.controller = controller;
    }

    public String getType() {
        return type;
    }

}

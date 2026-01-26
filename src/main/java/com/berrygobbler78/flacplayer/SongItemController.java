package com.berrygobbler78.flacplayer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SongItemController implements Initializable {
    @FXML
    private Label songNumberLabel, songTitleLabel, songArtistLabel;
    @FXML
    private ImageView playIV, songAlbumIV;
    @FXML
    private StackPane stackPane;

    private File songFile;

    private MusicPlayer musicPlayer;
    private Controller controller;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.musicPlayer = App.musicPlayer;
    }

    public void setItemInfo(int songNumber, String songTitle, String songArtist, Image albumImage, File songFile) {
        this.songNumberLabel.setText(String.valueOf(songNumber));
        this.songTitleLabel.setText(songTitle);
        this.songArtistLabel.setText(songArtist);
        this.songAlbumIV.setImage(albumImage);
        this.songFile = songFile;
    }

    void setMainController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    private void playSong() {
        musicPlayer.setDirectoryPath(songFile.getParent(), "album");
        musicPlayer.setCurrentSongIndex(Integer.parseInt(songNumberLabel.getText())-1);
        controller.setCurrentPlayPauseImageViewPaused(false);
        musicPlayer.play();

        musicPlayer.refreshAlbumSongQueue();
    }
}

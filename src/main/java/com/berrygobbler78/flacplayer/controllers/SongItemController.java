package com.berrygobbler78.flacplayer.controllers;

import com.berrygobbler78.flacplayer.*;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.userdata.References;
import com.berrygobbler78.flacplayer.util.Constants;
import com.berrygobbler78.flacplayer.util.FileUtils;
import com.berrygobbler78.flacplayer.util.MusicPlayer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class SongItemController implements Initializable {
    @FXML
    private Label songNumberLabel, songTitleLabel, songArtistLabel;
    @FXML
    private ImageView playIV, songAlbumIV;
    @FXML
    private StackPane stackPane;
    @FXML
    private Menu playlistMenu;

    private String songPath;
    
    private MainController mainController;
    private PreviewTabController previewTabController;

    private Constants.PARENT_TYPE parentType;

    private MusicPlayer musicPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void setItemInfo(int songNumber, String songPath, Constants.PARENT_TYPE parentType) {
        this.songPath = songPath;
        this.parentType = parentType;
        
        this.songNumberLabel.setText(String.valueOf(songNumber));
        this.songTitleLabel.setText(FileUtils.getSongTitle(songPath));
        // FIXME: Reduce runtime of below line
        this.songAlbumIV.setImage(FileUtils.getCoverImage(songPath, FileUtils.FILE_TYPE.SONG));
        
        switch(parentType) {
            case PLAYLIST -> songArtistLabel.setText(FileUtils.getSongArtist(songPath) + " // " + FileUtils.getSongAlbum(songPath));
            case ALBUM -> songArtistLabel.setText(FileUtils.getSongArtist(songPath));
        }

        for(Playlist playlist : App.getReferences().getPlaylists()) {
            CheckMenuItem playlistMenuItem = getCheckMenuItem(songPath, playlist);
            playlistMenu.getItems().add(playlistMenuItem);
        }
    }

    private static CheckMenuItem getCheckMenuItem(String songPath, Playlist playlist) {
        CheckMenuItem playlistMenuItem = new CheckMenuItem(playlist.getName());

        if(playlist.getSongList().contains(songPath)) {
            playlistMenuItem.setSelected(true);
        }

        playlistMenuItem.setOnAction(_ -> {
            if(playlistMenuItem.isSelected() && !playlist.getSongList().contains(songPath)) {
                playlist.addSong(songPath);
            } else if(playlist.getSongList().contains(songPath)) {
                playlist.removeSong(playlist.getSongList().indexOf(songPath));
            }
        });

        return playlistMenuItem;
    }

    public void setControllers(MainController mainController, PreviewTabController previewTabController) {
        this.mainController = mainController;
        this.previewTabController = previewTabController;

        musicPlayer = mainController.getMusicPlayer();
    }

    @FXML
    private void playSong() {
        switch(parentType) {
            case PLAYLIST:
                musicPlayer.setParentType(previewTabController.getPlaylist(), null);
                break;
            case ALBUM:
                musicPlayer.setParentType(null, new File(songPath).getParentFile());
        }

        musicPlayer.setPreviewTabController(previewTabController);
        musicPlayer.playSongNum(Integer.parseInt(songNumberLabel.getText())-1);
    }

    @FXML
    private void addToQueue() {
        musicPlayer.addToUserQueue(songPath);
    }
}

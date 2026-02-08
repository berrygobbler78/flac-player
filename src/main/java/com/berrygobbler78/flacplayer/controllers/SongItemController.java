package com.berrygobbler78.flacplayer.controllers;

import com.berrygobbler78.flacplayer.*;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.userdata.References;
import com.berrygobbler78.flacplayer.util.FileUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.nio.file.Path;
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

    private final References references = App.references;

    private Constants.PARENT_TYPE parentType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void setItemInfo(int songNumber, String songPath, Constants.PARENT_TYPE parentType) {
        this.songPath = songPath;
        this.parentType = parentType;
        
        this.songNumberLabel.setText(String.valueOf(songNumber));
        this.songTitleLabel.setText(FileUtils.getSongTitle(songPath));
        this.songAlbumIV.setImage(FileUtils.getCoverImage(songPath, FileUtils.FILE_TYPE.SONG));
        
        switch(parentType) {
            case PLAYLIST -> songArtistLabel.setText(FileUtils.getSongArtist(songPath) + " // " + FileUtils.getSongAlbum(songPath));
            case ALBUM -> songArtistLabel.setText(FileUtils.getSongArtist(songPath));
        }
        
        for(Playlist playlist : App.references.getPlaylists()) {
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
    }

    @FXML
    private void playSong() {
        switch(parentType) {
            case PLAYLIST:
                MusicPlayer.setParentTypePlaylist(previewTabController.getPlaylist());
                break;
            case ALBUM:
                MusicPlayer.setParentTypeAlbum(Path.of(songPath).getParent().toString());
        }

        MusicPlayer.setPreviewTabController(previewTabController);
        MusicPlayer.playSongNum(Integer.parseInt(songNumberLabel.getText())-1);
    }

    @FXML
    private void addToQueue() {
        MusicPlayer.addToUserQueue(songPath);
    }
}

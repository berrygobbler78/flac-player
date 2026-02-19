package com.berrygobbler78.flacplayer.controllers;

import com.berrygobbler78.flacplayer.App;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PopupWindowsController {
    @FXML
    private TextField playlistNameField;

    private Stage stage;
    private MainController controller;

    public void setValues(Stage stage, MainController controller) {
        this.stage = stage;
        this.controller = controller;
    }

    @FXML
    private void enterPlaylistName(){
        App.getReferences().addPlaylist(new Playlist(playlistNameField.getText()));
        App.savePlaylists();
        stage.close();

        controller.refreshTreeView();
    }
}

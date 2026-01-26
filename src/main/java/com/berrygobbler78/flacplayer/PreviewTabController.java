package com.berrygobbler78.flacplayer;

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
    private VBox songItemVBox;

    private String type;
    private File file;
    private Controller controller;

    private final Image playImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/play.png")));
    private final Image pauseImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/pause.png")));

    private final FileFilter flacFilter = new FileFilter() {
        public boolean accept(File f)
        {
            return f.getName().endsWith("flac");
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void playPreview() {

    }

    public void setValues(File file, Image albumImage, String albumName, String artistName, String albumOrPlaylist) {
        this.file = file;
        albumImageView.setImage(albumImage);
        albumLabel.setText(albumName);
        artistLabel.setText(artistName);
        type = albumOrPlaylist;
    }

    public void refreshSongItemVBox() {
        songItemVBox.getChildren().clear();
        try {
            int nodesLength = 0;

            for(File fileToCheck : Objects.requireNonNull(file.listFiles(flacFilter))){
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

    public void setPlayPauseImageView(boolean paused) {
        if(paused) {
            playPauseImageView.setImage(playImage);
        } else  {
            playPauseImageView.setImage(pauseImage);
        }
    }

}

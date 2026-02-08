package com.berrygobbler78.flacplayer.userdata;

import com.berrygobbler78.flacplayer.App;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Playlist implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Playlist.class.getName());

    private final String path;
    private ArrayList<String> songList =new ArrayList<>();
    private String playlistName;

    public Playlist(String playlistName) {
        this.playlistName = playlistName;
        path = "src/main/resources/com/berrygobbler78/flacplayer/graphics/playlist-art/" +
                playlistName.toLowerCase().replace(" ", "-");

        LOGGER.info(String.format("Created playlist: [%s]", playlistName));
    }

    public void setName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getName() {
        return playlistName;
    }

    public String getPath() {
        return path;
    }

    public ArrayList<String> getSongList(){
        songList.removeIf(s -> !new File(s).exists());
        return songList;
    }

    public void addSong(String song) {
        songList.add(song);
        App.savePlaylists();

        LOGGER.info(String.format("Added song: [%s]", song));
    }

    public void addSong(int index, String song) {
        songList.add(index, song);
        App.savePlaylists();

        LOGGER.info(String.format("Added song: [%s] at position: %d", song, index));
    }

    public void removeSong(int index) {
        songList.remove(index);
        App.savePlaylists();
    }
}

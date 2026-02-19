package com.berrygobbler78.flacplayer.userdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.berrygobbler78.flacplayer.App;

public class References implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(References.class.getName());

    private String rootDirectoryPath;
    private String userName;

    private List<Playlist> playlists = new ArrayList<>();

    public void setUserName(String userName) {
        this.userName = userName;

        LOGGER.log(Level.INFO, "Username set to: [" + userName + "]");
    }

    public String getUserName() {
        return userName;
    }

    public void setRootDirectoryPath(String rootDirectoryPath) {
        this.rootDirectoryPath = rootDirectoryPath;

        LOGGER.info(String.format("Root directory path set to: [%s]", rootDirectoryPath));
    }

    public String getRootDirectoryPath() {
        return rootDirectoryPath;
    }

    public void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        LOGGER.info(String.format("Added playlist: [%s]", playlist.getName()));

        App.savePlaylists();
    }

    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
        App.deletePlaylist(playlist);

        LOGGER.info(String.format("Removed playlist: [%s]", playlist.getName()));
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void clearPlaylists() {
        playlists.clear();
    }
}

package com.berrygobbler78.flacplayer.userdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.berrygobbler78.flacplayer.userdata.Playlist;

public class References implements Serializable {
    private String rootDirectoryPath;
    private String userName;

    private List<Playlist> playlists = new ArrayList<>();

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setRootDirectoryPath(String rootDirectoryPath) {
        this.rootDirectoryPath = rootDirectoryPath;
    }

    public String getRootDirectoryPath() {
        return rootDirectoryPath;
    }

    public void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        System.out.println("Added playlist " + playlist.getName());
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void clearPlaylists() {
        playlists.clear();
    }
}

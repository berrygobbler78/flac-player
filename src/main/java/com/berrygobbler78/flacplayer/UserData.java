package com.berrygobbler78.flacplayer;

import java.io.File;
import java.io.Serializable;

public class UserData implements Serializable {
    private String rootDirectoryPath;

    public void setRootDirectoryPath(String rootDirectoryPath) {
        this.rootDirectoryPath = rootDirectoryPath;
        App.fileUtils.directoryPath = rootDirectoryPath;
    }

    public String getRootDirectoryPath() {
        return rootDirectoryPath;
    }
}

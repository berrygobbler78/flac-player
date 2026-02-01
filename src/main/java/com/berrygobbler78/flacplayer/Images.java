package com.berrygobbler78.flacplayer;

import javafx.scene.image.Image;

import java.util.Objects;

public class Images {
//    General Icons
    private static final Image warningImage = new Image(Objects.requireNonNull(Images.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/warning.png")));
    private static final Image cdImage = new Image(Objects.requireNonNull(Images.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/cd.png")));
    private static final Image songImage = new Image(Objects.requireNonNull(Images.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/song.png")));

//    Interactables
    private static final Image playImage = new Image(Objects.requireNonNull(Images.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/play.png")));
    private static final Image pauseImage = new Image(Objects.requireNonNull(Images.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/pause.png")));

    public enum ImageName {
      WARNING, CD, SONG, PLAY, PAUSE
    }

    public static Image getImage(ImageName image) {
        switch (image) {
            case WARNING:
                return warningImage;
            case CD:
                return cdImage;
            case SONG:
                return songImage;
            case PLAY:
                return playImage;
            case PAUSE:
                return pauseImage;
            default:
                return warningImage;
        }
    }
}
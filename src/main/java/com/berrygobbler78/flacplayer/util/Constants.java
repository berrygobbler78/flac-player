package com.berrygobbler78.flacplayer.util;

import javafx.scene.image.Image;

import java.util.Objects;

public class Constants {
    private static final String SONG_ITEM_PATH = "src/main/resources/com/berrygobbler78/flacplayer/fxml/songItem.fxml";
    private static final String PREVIEW_TAB_PATH = "src/main/resources/com/berrygobbler78/flacplayer/fxml/previewTab.fxml";
    private static final String MAIN_PATH =  "src/main/resources/com/berrygobbler78/flacplayer/fxml/revised.fxml";
    private static final String NEW_PLAYLIST_WINDOW_PATH = "src/main/resources/com/berrygobbler78/flacplayer/fxml/newPlaylistWindow.fxml";

    public enum FXML_PATHS {
        SONG_ITEM(SONG_ITEM_PATH),
        PREVIEW_TAB(PREVIEW_TAB_PATH),
        MAIN(MAIN_PATH),
        NEW_PLAYLIST(NEW_PLAYLIST_WINDOW_PATH);

        private final String PATH;

        FXML_PATHS(String path) {
            this.PATH = path;
        }

        public String get() {
            return PATH;
        }
    }

    public enum PARENT_TYPE {
        ALBUM,
        PLAYLIST
    }

    private static final Image berriesImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/berries.png")));

    //    General Icons
    private static final Image warningImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/warning.png")));
    private static final Image cdImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/cd.png")));
    private static final Image songImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/song.png")));
    private static final Image userImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/user.png")));

    //    Interactable
    private static final Image playImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/play.png")));
    private static final Image pauseImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/pause.png")));

    // Bottom bar
    private static final Image repeatUnselectedImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_unselected.png")));
    private static final Image repeatSelectedImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_selected.png")));
    private static final Image repeatOneSelectedImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/repeat_one_selected.png")));

    private static final Image shuffleUnselectedImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/shuffle_unselected.png")));
    private static final Image shuffleSelectedImage =
            new Image(Objects.requireNonNull(Constants.class.getResourceAsStream("/com/berrygobbler78/flacplayer/graphics/shuffle_selected.png")));


    public enum IMAGES {
        BERRIES(berriesImage),
        WARNING(warningImage),
        CD(cdImage),
        SONG(songImage),
        USER(userImage),
        PLAY(playImage),
        PAUSE(pauseImage),
        REPEAT_UNSELECTED(repeatUnselectedImage),
        REPEAT_ALL(repeatSelectedImage),
        REPEAT_ONE(repeatOneSelectedImage),
        SHUFFLE_UNSELECTED(shuffleUnselectedImage),
        SHUFFLE_SELECTED(shuffleSelectedImage);

        private final Image IMAGE;
        IMAGES(Image image) {
            this.IMAGE = image;
        }

        public Image get() {
            return IMAGE;
        }
    }
}

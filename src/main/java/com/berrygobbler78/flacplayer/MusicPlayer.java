package com.berrygobbler78.flacplayer;

import com.berrygobbler78.flacplayer.Constants.*;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.berrygobbler78.flacplayer.controllers.MainController;
import com.berrygobbler78.flacplayer.controllers.PreviewTabController;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.util.FileUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public final class MusicPlayer {
    private static final Logger LOGGER = Logger.getLogger(MusicPlayer.class.getName());

    public enum REPEAT_STATUS {
        OFF,
        REPEAT_ALL,
        REPEAT_ONE
    }

    // Related to parentType
    private static Constants.PARENT_TYPE parentType;

    private static String albumPath;
    private static File albumFile;

    private static Playlist playlist;

    private static String currentSongPath;

    // Queueing
    private static ArrayList<String> nextSongsQueue = new ArrayList<>();
    private static ArrayList<String> previousSongsQueue = new ArrayList<>();
    private static ArrayList<String> userQueue = new ArrayList<>();

    // Controllers
    private static MainController mainController;
    private static PreviewTabController previewTabController;

    private static MediaPlayer mediaPlayer;

    private static Timeline songTimeline;

    private static REPEAT_STATUS repeatStatus = REPEAT_STATUS.OFF;
    private static boolean shuffleSelected = false;

    private static final Random random = new Random();

    private static boolean playing = false;

    public static void setController(MainController controller) {
        mainController = controller;
    }

    public static void addToUserQueue(String path) {
        File file =  new File(path);

        if(!file.exists()) {
            LOGGER.log(Level.WARNING, "File does not exist: " + path);
            return;
        }

        if(file.isDirectory()) {
            try {
                if(file.getAbsolutePath().equals(albumPath)) {
                    LOGGER.log(Level.WARNING, "Album is currently playing: " + path);
                    return;
                }
                for (File songFile : Objects.requireNonNull(file.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)))) {
                    userQueue.add(songFile.getAbsolutePath());
                    LOGGER.log(Level.INFO, "Added song: " + songFile.getAbsolutePath());
                }
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Folder provided is empty: " + path);
            }
        } else if(file.getName().endsWith(".flac")) {
            userQueue.addLast(file.getAbsolutePath());
            LOGGER.log(Level.INFO, "Added song: " + file.getAbsolutePath());
        }

        if(!isPlaying()) {
            loadSong(userQueue.getFirst());
        }
    }

    public static void playFirstSong() {
        switch(parentType) {
            case ALBUM:
                loadSong(Objects.requireNonNull(new File(albumPath).listFiles())[0].getAbsolutePath());
                play();
                break;
            case PLAYLIST:
                loadSong(playlist.getSongList().getFirst());
                play();
                break;
        }

        refreshSongQueue();
    }

    public static void playSongNum(int num) {
        switch(parentType) {
            case ALBUM:
                loadSong(Objects.requireNonNull(albumFile.listFiles())[num].getAbsolutePath());
                play();
                break;
            case PLAYLIST:
                loadSong(playlist.getSongList().get(num));
                play();
                break;
        }

        refreshSongQueue();
    }

    public static void setParentTypeAlbum(String albumPath) {
        MusicPlayer.albumPath = albumPath;
        albumFile = new File(MusicPlayer.albumPath);
        parentType = PARENT_TYPE.ALBUM;
    }

    public static void setParentTypePlaylist(Playlist playlist) {
        MusicPlayer.playlist = playlist;
        parentType = PARENT_TYPE.PLAYLIST;
    }

    public static void pauseTimeline() {
        if (songTimeline != null) {
            songTimeline.pause();
        }
    }

    public static void refreshSongQueue() {
        nextSongsQueue = new ArrayList<>();
        previousSongsQueue = new ArrayList<>();
        boolean add = false;

        switch (parentType) {
            case ALBUM:
                for(File file : Objects.requireNonNull(albumFile.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)))) {
                    if(add) {
                        nextSongsQueue.add(file.getAbsolutePath());
                    } else {
                        previousSongsQueue.add(file.getAbsolutePath());
                    }
                    if(file.getAbsolutePath().equals(currentSongPath)) {
                        add = true;
                    }
                }
                break;
            case PLAYLIST:
                for(String path : playlist.getSongList()) {
                    if(add) {
                        nextSongsQueue.add(path);
                    } else  {
                        previousSongsQueue.add(path);
                    }
                    if(path.equals(currentSongPath)) {
                        add = true;
                    }
                }
        }


        if(shuffleSelected) {
            shuffle();
        }
    }

    public static void loadSong(String songPath) {
        if(mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        if(songTimeline != null) {
            songTimeline.stop();
        }

        FileUtils.flacToWav(songPath, "src/main/resources/com/berrygobbler78/flacplayer/cache/temp.wav");
        String wavPath = "src/main/resources/com/berrygobbler78/flacplayer/cache/temp.wav";
        mediaPlayer = new MediaPlayer(new Media(new File(wavPath).toURI().toString()));
        mediaPlayer.setOnEndOfMedia(MusicPlayer::next);
        mediaPlayer.setOnPlaying(()-> {
            playing = true;
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPlayPauseImageViewPaused(false);
            }
        });
        mediaPlayer.setOnPaused(()-> {
            playing = false;
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPlayPauseImageViewPaused(true);
            }
        });
        mediaPlayer.setOnStopped(()-> {
            playing = false;
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPlayPauseImageViewPaused(true);
            }
        });

        currentSongPath = songPath;
    }

    public static boolean isReady() {
        return mediaPlayer != null;
    }

    public static void setVolume(float volume) { mediaPlayer.setVolume(volume); }

    public static void setPreviewTabController(PreviewTabController controller) {
        if(previewTabController != null) {
            previewTabController.setPlayPauseImageViewPaused(true);
        }

        previewTabController = controller;
    }

    public static String getCurrentSongPath() {
        return currentSongPath;
    }

    public static void play() {
        if(mediaPlayer != null) {

            // Make a new timeline if not open
            if(songTimeline == null || songTimeline.getStatus() == Timeline.Status.STOPPED) {
                songTimeline = new Timeline(new KeyFrame(Duration.millis(200), _ -> {
                            mainController.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
                            mainController.setSongProgressSliderPos((int) mediaPlayer.getCurrentTime().toMillis(), (int) mediaPlayer.getTotalDuration().toMillis());
                        }
                ));

                songTimeline.setCycleCount(Timeline.INDEFINITE);
            }

            if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                return;
            }

            if (mediaPlayer.getStatus() != MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.setOnReady(() -> {
                    // Player is ready to play the media
                    mainController.setTotTrackTime((int) mediaPlayer.getTotalDuration().toSeconds());

                    songTimeline.play();
                    mediaPlayer.play();
                    playing = true;
                });
            } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                songTimeline.play();
                mediaPlayer.play();
                playing = true;

            }

            if(previewTabController != null) {
                previewTabController.setPlayPauseImageViewPaused(false);
            }
        }
    }

    public static void pause() {
        if(mediaPlayer != null) {
            songTimeline.pause();
            mainController.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
            mediaPlayer.pause();
            playing = false;

            if(previewTabController != null) {

                previewTabController.setPlayPauseImageViewPaused(true);
            }
        }
    }

    public static void stop() {
        if(mediaPlayer != null) {
            songTimeline.stop();
            mediaPlayer.stop();
            playing = false;

            if(previewTabController != null) {
                previewTabController.setPlayPauseImageViewPaused(true);
            }
        }
    }

    public static void next() {
        if(repeatStatus == REPEAT_STATUS.REPEAT_ONE) {
            stop();
            mediaPlayer.seek(Duration.ZERO);
            play();
            return;

        }

        stop();

        if(!userQueue.isEmpty()) {
            previousSongsQueue.addFirst(currentSongPath);
            loadSong(userQueue.getFirst());
            userQueue.removeFirst();
            play();

        } else if(!nextSongsQueue.isEmpty()) {
            previousSongsQueue.addFirst(currentSongPath);
            loadSong(nextSongsQueue.getFirst());
            nextSongsQueue.removeFirst();
            play();

        } else if(repeatStatus == REPEAT_STATUS.REPEAT_ALL) {
            previousSongsQueue.addFirst(currentSongPath);
            playFirstSong();
        }
    }

    public static void previous() {
        if (mediaPlayer.getCurrentTime().toSeconds() > 3) {
            mediaPlayer.seek(Duration.ZERO);
            play();
        } else if(!previousSongsQueue.isEmpty()) {
            nextSongsQueue.addFirst(currentSongPath);
            loadSong(previousSongsQueue.getFirst());
            previousSongsQueue.removeFirst();
            play();
        }
    }

    public static void setRepeatStatus(REPEAT_STATUS status) {
        repeatStatus = status;
    }

    public static void setShuffleStatus(boolean shuffleStatus) {
        shuffleSelected = shuffleStatus;
    }

    public static void shuffle() {
        ArrayList<String> temp = new ArrayList<>(nextSongsQueue);
        nextSongsQueue.clear();

        while(!temp.isEmpty()) {
            nextSongsQueue.add(temp.remove(random.nextInt(temp.size())));
        }
    }

    public static void closeMediaPlayer() {
        mediaPlayer.dispose();
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static int getSongPosFromSlider(int value) {
        if(mediaPlayer != null) {
            return (int) mediaPlayer.getStartTime().add(mediaPlayer.getTotalDuration().multiply(value / 100.0)).toSeconds();
        } else {
            return 0;
        }
    }

    public static void changeSongPos(double pos) {
        if(mediaPlayer != null) {
            return;
        }

        Duration newTime = Duration.ZERO;

        try{
            newTime = mediaPlayer.getStartTime().add(mediaPlayer.getTotalDuration().multiply(pos / 100.0));
        } catch (NullPointerException e){
            LOGGER.log(Level.WARNING, mediaPlayer.toString() + ".getStartTime() produced NullPointerException.");
        }

        mediaPlayer.seek(newTime);

        if(songTimeline != null) {
            songTimeline.play();
        }
    }
}

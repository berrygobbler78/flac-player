package com.berrygobbler78.flacplayer.util;

import com.berrygobbler78.flacplayer.App;
import com.berrygobbler78.flacplayer.util.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.berrygobbler78.flacplayer.controllers.MainController;
import com.berrygobbler78.flacplayer.controllers.PreviewTabController;
import com.berrygobbler78.flacplayer.userdata.Playlist;
import io.github.selemba1000.JMTCParameters;
import io.github.selemba1000.JMTCPlayingState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public final class MusicPlayer {
    private final Logger LOGGER = Logger.getLogger(MusicPlayer.class.getName());

    public enum REPEAT_STATUS {
        OFF,
        REPEAT_ALL,
        REPEAT_ONE
    }

    public enum SHUFFLE_STATUS {
        OFF,
        SHUFFLE
    }

    public enum MUSIC_PLAYER_STATUS {
        STOPPED,
        PLAYING,
        PAUSED
    }

    // Current song information
    private String currentPath;

    private String currentTitle;
    private String currentAlbum;
    private String currentArtist;

    private double currentEnd;

    // Parent information
    private PARENT_TYPE currentParentType;

    private Playlist currentPlaylist;
    private File currentAlbumFile;

    // Queueing
    private ArrayList<String> previousSongsQueue = new ArrayList<>();
    private ArrayList<String> userQueue = new ArrayList<>();
    private ArrayList<String> nextSongsQueue = new ArrayList<>();

    // Controllers
    private MainController mainController;
    private PreviewTabController previewTabController;

    // Utilities
    private MediaPlayer mediaPlayer;
    private final MediaTransport MEDIA_TRANSPORT;
    private Timeline songTimeline;

    // Statuses
    private SHUFFLE_STATUS shuffleStatus = SHUFFLE_STATUS.OFF;
    private REPEAT_STATUS repeatStatus = REPEAT_STATUS.OFF;
    private MUSIC_PLAYER_STATUS musicPlayerStatus = MUSIC_PLAYER_STATUS.STOPPED;

    private final Random random = new Random();

    public MusicPlayer(MainController mainController) {
        setMainController(mainController);

        MEDIA_TRANSPORT = new MediaTransport("berrybush", "/home/berrygobbler78/IdeaProjects/BerryBush/src/main/java/com/berrygobbler78/flacplayer/App.java", this);
        MEDIA_TRANSPORT.setEnabled(true);
    }

    // Controllers

    public void setMainController(MainController controller) {
        mainController = controller;
    }

    public void setPreviewTabController(PreviewTabController controller) {
        previewTabController = controller;
    }

    public void setParentType(Playlist playlist, File albumFile) {
        if(playlist != null) {
            currentPlaylist = playlist;
            currentParentType = PARENT_TYPE.PLAYLIST;
            LOGGER.info(String.format("Playlist [%s] set as parent", playlist.getName()));
        } else if(albumFile != null) {
            currentAlbumFile = albumFile;
            currentParentType = PARENT_TYPE.ALBUM;
            LOGGER.info(String.format("Album [%s] set as parent", albumFile.getName()));
        } else {
            LOGGER.warning("Nothing to add");
        }
    }

    // Queueing

    public void addToUserQueue(String filePath) {
        File file =  new File(filePath);

        if(!file.exists()) {
            LOGGER.log(Level.WARNING, "File does not exist at path: " + filePath);
            return;
        }

        if(file.isDirectory()) {
            // Returns if album added is already playing
            if(file.equals(currentAlbumFile)) {
                LOGGER.log(Level.WARNING, "Album is currently playing: " + filePath);
                return;
            }

            // Add flac files from directory
            try {
                for (File songFile : Objects.requireNonNull(file.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)))) {
                    userQueue.add(songFile.getAbsolutePath());
                    LOGGER.log(Level.INFO, String.format("Added [%s] to user queue", FileUtils.getSongTitle(songFile.getAbsolutePath())));
                }
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Folder provided contains no flac files: " + filePath);
            }
        } else if(file.getName().endsWith(".flac")) {
            userQueue.addLast(filePath);
            LOGGER.log(Level.INFO, String.format("Added [%s] to user queue", FileUtils.getSongTitle(filePath)));
        }

        if(getMusicPlayerState() != MUSIC_PLAYER_STATUS.PLAYING) {
            loadSong(userQueue.getFirst(), true);
        }
    }

    public void generateParentQueue(int index, boolean playAfter) {
        clearQueues();

        boolean add = false;

        switch (currentParentType) {
            case ALBUM:
                File[] songs = Objects.requireNonNull(currentAlbumFile.listFiles(FileUtils.getFileFilter(FileUtils.FILTER_TYPE.FLAC)));
                Arrays.sort(songs);

                int i = 0;
                for(File song : songs) {
                    if(i == index) {
                        loadSong(song.getAbsolutePath(), playAfter);
                        add = true;
                    } else if(add) {
                        nextSongsQueue.add(song.getAbsolutePath());
                    } else {
                        previousSongsQueue.add(song.getAbsolutePath());
                    }

                    i++;
                }

                break;
            case PLAYLIST:
                for(String song : currentPlaylist.getSongList()) {
                    if(song.equals(currentPlaylist.getSongList().get(index))) {
                        loadSong(song, playAfter);
                        add = true;
                    } else if(add) {
                        nextSongsQueue.add(song);
                    } else  {
                        previousSongsQueue.add(song);
                    }
                }
        }


        if(shuffleStatus == SHUFFLE_STATUS.SHUFFLE) {
            shuffle();
        }

        LOGGER.info(
                "Generated new parent queue!\n" +
                        String.format("NextQueue length = [%s]\n", nextSongsQueue.size()) +
                        String.format("PrevQueue length = [%s]\n", previousSongsQueue.size())
                );
    }

    public void clearQueues() {
        previousSongsQueue = new ArrayList<>();
        nextSongsQueue = new ArrayList<>();
    }

    // Play utils

    public void playFirstSong() {
        LOGGER.log(Level.INFO, "Playing first song");
        generateParentQueue(0, true);
    }

    public void playSongNum(int index) {
        LOGGER.log(Level.INFO, String.format("Playing [%s]", index));
        generateParentQueue(index, true);
    }

    public void pauseTimeline() {
        if (songTimeline != null) {
            songTimeline.pause();
        }
    }

    public void loadSong(String songPath, boolean playAfter) {
        LOGGER.info(String.format("Loading song: [%s]", FileUtils.getSongTitle(songPath)));

        currentPath = songPath;

        if(mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        if(songTimeline != null) {
            songTimeline.stop();
        }

        // FIXME: Threading for loading song

        String wavPath = null;
        try {
            wavPath = FileUtils.flacToWav(songPath).getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mediaPlayer = new MediaPlayer(new Media(new File(wavPath).toURI().toString()));

        if(playAfter) {
            play();
        }

        mediaPlayer.setOnEndOfMedia(this::next);

        mediaPlayer.setOnPlaying(()-> {
            musicPlayerStatus = MUSIC_PLAYER_STATUS.PLAYING;
            if(songTimeline != null) {
                songTimeline.play();
            }

            // Update gui
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPaused(false);
            }

            // Update transport
            MEDIA_TRANSPORT.setState(JMTCPlayingState.PLAYING);
            updateJMTC();

        });

        mediaPlayer.setOnPaused(()-> {
            musicPlayerStatus = MUSIC_PLAYER_STATUS.PAUSED;
            if(songTimeline != null) {
                songTimeline.pause();
            }

            // Update gui
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPaused(false);
            }

            // Update transport
            MEDIA_TRANSPORT.setState(JMTCPlayingState.PAUSED);
            updateJMTC();

        });

        mediaPlayer.setOnStopped(()-> {
            musicPlayerStatus = MUSIC_PLAYER_STATUS.STOPPED;
            if(songTimeline != null) {
                songTimeline.stop();
            }

            // Update gui
            mainController.updateBottomBar();
            if(previewTabController != null) {
                previewTabController.setPaused(true);
            }

            // Update transport
            MEDIA_TRANSPORT.setState(JMTCPlayingState.STOPPED);
            updateJMTC();

        });

        if(playAfter) {
            pause();
            play();
        }

        LOGGER.info("Loading done...");

    }

    public void setVolume(double volume) {
        if(mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public String getCurrentSongPath() {
        return currentPath;
    }

    public void play() {
        if(mediaPlayer != null) {
            if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                return;
            }

            // Make a new timeline if not open
            if(songTimeline == null || songTimeline.getStatus() == Timeline.Status.STOPPED) {
                songTimeline = new Timeline(new KeyFrame(Duration.millis(200), _ -> {
                            mainController.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
                            mainController.setSongProgressSliderPos((int) mediaPlayer.getCurrentTime().toMillis(), (int) mediaPlayer.getTotalDuration().toMillis());
                        }
                ));

                songTimeline.setCycleCount(Timeline.INDEFINITE);
            }

            if (mediaPlayer.getStatus() != MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.setOnReady(() -> {
                    // Player is ready to play the media
                    mainController.setTotTrackTime((int) mediaPlayer.getTotalDuration().toSeconds());

                    songTimeline.play();
                    mediaPlayer.play();

                    LOGGER.info("First play");
                });
            } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                songTimeline.play();
                mediaPlayer.play();

                LOGGER.info("Resume");
            }

            if(previewTabController != null) {
                previewTabController.setPaused(false);
            }
        }
    }

    public void pause() {
        if(mediaPlayer != null) {
            songTimeline.pause();
            mainController.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
            mediaPlayer.pause();

            if(previewTabController != null) {

                previewTabController.setPaused(true);
            }
        }
    }

    public void stop() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();

            if(previewTabController != null) {
                previewTabController.setPaused(true);
            }
        }
    }

    public void next() {
        if(repeatStatus == REPEAT_STATUS.REPEAT_ONE) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }

        stop();

        if(!userQueue.isEmpty()) {
            previousSongsQueue.addFirst(currentPath);
            loadSong(userQueue.getFirst(), true);
            userQueue.removeFirst();

        } else if(!nextSongsQueue.isEmpty()) {
            previousSongsQueue.addFirst(currentPath);
            loadSong(nextSongsQueue.getFirst(), true);
            nextSongsQueue.removeFirst();
        } else if(repeatStatus == REPEAT_STATUS.REPEAT_ALL) {
            previousSongsQueue.addFirst(currentPath);
            playFirstSong();
        } else if(currentParentType == PARENT_TYPE.PLAYLIST) {

        }
    }

    public void previous() {
        if (mediaPlayer.getCurrentTime().toSeconds() > 3) {
            mediaPlayer.seek(Duration.ZERO);
        } else if(!previousSongsQueue.isEmpty()) {
            nextSongsQueue.addFirst(currentPath);
            loadSong(previousSongsQueue.getFirst(), true);
            previousSongsQueue.removeFirst();
        }
    }

    // Repeat status control

    public void cycleRepeatStatus() {
        switch (repeatStatus) {
            case REPEAT_ONE:
                repeatStatus = REPEAT_STATUS.OFF;
                break;
            case REPEAT_ALL:
                repeatStatus = REPEAT_STATUS.REPEAT_ONE;
                break;
            case OFF:
                repeatStatus = REPEAT_STATUS.REPEAT_ALL;
                break;
        }

        mainController.updateRepeatButton();
    }

    public void setRepeatStatus(JMTCParameters.LoopStatus loopStatus) {
        switch (loopStatus) {
            case None ->  repeatStatus = REPEAT_STATUS.OFF;
            case Track ->   repeatStatus = REPEAT_STATUS.REPEAT_ONE;
            case Playlist ->   repeatStatus = REPEAT_STATUS.REPEAT_ALL;
        }

        mainController.updateRepeatButton();
    }

    public REPEAT_STATUS getRepeatStatus() {
        return repeatStatus;
    }

    // Shuffle status control

    public void toggleShuffleStatus() {
        switch (shuffleStatus) {
            case OFF ->   shuffleStatus = SHUFFLE_STATUS.SHUFFLE;
            case SHUFFLE ->   shuffleStatus = SHUFFLE_STATUS.OFF;
        }

        mainController.updateShuffleButton();

        if(shuffleStatus == SHUFFLE_STATUS.SHUFFLE && !nextSongsQueue.isEmpty()) {
            shuffle();
        }
    }

    public void setShuffleStatus(boolean shuffle) {
        if(shuffle) {
            shuffleStatus = SHUFFLE_STATUS.SHUFFLE;
        } else {
            shuffleStatus = SHUFFLE_STATUS.OFF;
        }

        mainController.updateShuffleButton();
    }

    public SHUFFLE_STATUS getShuffleStatus() {
        return shuffleStatus;
    }

    public void shuffle() {
        ArrayList<String> temp = new ArrayList<>(nextSongsQueue);
        nextSongsQueue.clear();

        while(!temp.isEmpty()) {
            nextSongsQueue.add(temp.remove(random.nextInt(temp.size())));
        }
    }

    public void updateJMTC() {
        if(currentParentType != null && currentParentType == PARENT_TYPE.PLAYLIST) {
            MEDIA_TRANSPORT.setProperties(
                    FileUtils.getSongTitle(currentPath),
                    FileUtils.getSongArtist(currentPath),
                    currentPlaylist.getName(),
                    App.getReferences().getUserName(),
                    0,
                    0,
                    FileUtils.getCoverImageFile(currentPath, FileUtils.FILE_TYPE.SONG)
            );

            return;
        }

        String songArtist = FileUtils.getSongArtist(currentPath);
        MEDIA_TRANSPORT.setProperties(
                FileUtils.getSongTitle(currentPath),
                songArtist,
                FileUtils.getSongAlbum(currentPath),
                songArtist,
                0,
                0,
                FileUtils.getCoverImageFile(currentPath, FileUtils.FILE_TYPE.SONG)
        );

    }

    public void closeMediaPlayer() {
        mediaPlayer.dispose();
    }

    public MUSIC_PLAYER_STATUS getMusicPlayerState() {
        return musicPlayerStatus;
    }

    public int getSongPosFromSlider(int value) {
        try {
            return (int) mediaPlayer.getStartTime().add(mediaPlayer.getTotalDuration().multiply(value / 100.0)).toSeconds();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    public void changeSongPos(double pos) {
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

package com.berrygobbler78.flacplayer;

import java.io.File;
import java.util.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public final class MusicPlayer {

    private File directory;
    private String directoryPath;

    private ArrayList<File> songsList;

    private ArrayList<Integer> nextSongsQueue;
    private ArrayList<Integer> previousSongsQueue;
    private ArrayList<Integer> userQueue;

    private int currentSongIndex;

    public final FileUtils fileUtils;

    private boolean repeatAllSelected = false;
    private boolean repeatOneSelected = false;

    private boolean shuffleSelected = false;

    private String parentType;

    private Controller controller;

    private MediaPlayer mediaPlayer;

    Timeline songTimeline;

    String wavPath = "src/main/resources/com/berrygobbler78/flacplayer/cache/temp.wav";

    public MusicPlayer() {
        fileUtils = App.fileUtils;
        songsList = new ArrayList<>();

        nextSongsQueue = new ArrayList<>();
        userQueue = new ArrayList<>();
        previousSongsQueue = new ArrayList<>();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setCurrentSongIndex(int currentSongIndex) {
        this.currentSongIndex = currentSongIndex;
        loadSong(currentSongIndex);
    }

    public void setDirectoryPath(String directoryPath, String type) {
        System.out.println(directoryPath);
        this.directoryPath = directoryPath;

        switch (type) {
            case "album":
                this.parentType = "album";
                refreshAlbumSongList();
                break;
            case "playlist":
                this.parentType = "playlist";
                break;
            default:
                System.err.println("Invalid parent type");
                break;
        }

    }

    public void refreshAlbumSongList() {
        directory = new File(directoryPath);
        songsList = new ArrayList<>();

        try {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.getName().toLowerCase().endsWith(".flac")) {
                    songsList.add(file);
                }
            }
        } catch (NullPointerException e) {
            System.err.println("Error loading files from directory " + fileUtils.directoryPath);
        }
    }

    public void pauseTimeline() {
        if (songTimeline != null) {
            songTimeline.pause();
        }
    }

    public void refreshAlbumSongQueue() {
        nextSongsQueue = new ArrayList<>();
        previousSongsQueue = new ArrayList<>();

        for(int i = currentSongIndex + 1; i < songsList.size(); i++) {
            nextSongsQueue.add(i);
        }

        if(shuffleSelected) {
            ArrayList<Integer> temp = new ArrayList<Integer>();

            for (int i :  nextSongsQueue) {
//                temp.add(Random.nextInt(3), i);
            }
        }

        for(int i = currentSongIndex - 1; i >= 0; i--) {
            previousSongsQueue.add(i);
        }
    }

    public void loadSong(int songIndex) {
        if(mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        if(songTimeline != null) {
            songTimeline.stop();
        }

        System.out.println("Loading song " + songsList.get(songIndex).getName());

        fileUtils.flacToWav(songsList.get(songIndex).getAbsolutePath(), "src/main/resources/com/berrygobbler78/flacplayer/cache/temp.wav");

        mediaPlayer = new MediaPlayer(new Media(new File(wavPath).toURI().toString()));

        mediaPlayer.setOnEndOfMedia(this::next);

        currentSongIndex = songIndex;
    }

    public File getCurrentSongFile() {
        return songsList.get(currentSongIndex);
    }

    public void play() {
        if(mediaPlayer != null) {
            if (mediaPlayer.getStatus() != MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.setOnReady(() -> {
                    // Player is ready to play the media
                    controller.setTotTrackTime((int) mediaPlayer.getTotalDuration().toSeconds());
                    songTimeline = new Timeline(new KeyFrame(
                            Duration.millis(100),
                            ae -> {
                                controller.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
                                controller.setSongProgressSliderPos((int) mediaPlayer.getCurrentTime().toMillis(), (int) mediaPlayer.getTotalDuration().toMillis());
                            }
                    ));
                    songTimeline.setCycleCount(Timeline.INDEFINITE);

                    songTimeline.play();
                    mediaPlayer.play();
                    controller.updateBottomBar();
                });
            } else {
                controller.setTotTrackTime((int) mediaPlayer.getTotalDuration().toSeconds());

                songTimeline.play();
                mediaPlayer.play();
            }

        }
    }

    public void pause() {
        if(mediaPlayer != null) {
            songTimeline.pause();
            controller.setCurrentTrackTime((int) mediaPlayer.getCurrentTime().toSeconds());
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if(mediaPlayer != null) {
            songTimeline.stop();
            mediaPlayer.stop();
        }
    }

    public void next() {
        if(repeatOneSelected) {
            stop();
            mediaPlayer.seek(Duration.ZERO);
            play();
        } else {
            mediaPlayer.stop();
            if(!nextSongsQueue.isEmpty()) {
                previousSongsQueue.addFirst(currentSongIndex);
                loadSong(nextSongsQueue.getFirst());
                nextSongsQueue.removeFirst();
                play();
                controller.updateBottomBar();
            } else if(repeatAllSelected) {
                previousSongsQueue.addFirst(currentSongIndex);
                currentSongIndex = 0;
                refreshAlbumSongQueue();
                loadSong(currentSongIndex);
                play();
                controller.updateBottomBar();
            }
        }
    }

    public void previous() {
        if (mediaPlayer.getCurrentTime().toSeconds() > 3) {
            stop();
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        } else if(!previousSongsQueue.isEmpty()) {
            nextSongsQueue.addFirst(currentSongIndex);
            loadSong(previousSongsQueue.getFirst());
            previousSongsQueue.removeFirst();
            play();
        } else {
            stop();
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        }
    }

    public void setRepeatStatus(int repeatStatus) {
        switch(repeatStatus) {
            case 0:
                repeatAllSelected = false;
                repeatOneSelected = false;
                break;
            case 1:
                repeatAllSelected = true;
                repeatOneSelected = false;
                break;
            case 2:
                repeatAllSelected = false;
                repeatOneSelected = true;
                break;
            default:
                repeatAllSelected = false;
                System.err.println("Invalid repeat status");
                break;
        }
    }

    public void setShuffleStatus(boolean shuffleStatus) {
        shuffleSelected = shuffleStatus;
        switch (parentType) {
            case "album":
                refreshAlbumSongQueue();
                break;
            case "playlist":
                break;
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public String getSongTitle() {
        try {
            return songsList.get(currentSongIndex).getName().replace(".flac", "").substring(songsList.get(currentSongIndex).getName().indexOf(".")+1, songsList.get(currentSongIndex).getName().replace(".flac", "").lastIndexOf("-")).trim();

        } catch (Exception e) {
            System.err.println("Error getting song title " + e);
            return null;
        }
    }

    public String getArtistName() {
        try {
            return new File(new File(songsList.get(currentSongIndex).getParent()).getParent()).getName();
        } catch (Exception e) {
            System.err.println("Error getting artist name " + e);
            return null;
        }
    }

    public int getSongPosFromSlider(int value) {
        if(mediaPlayer != null) {
            return (int) mediaPlayer.getStartTime().add(mediaPlayer.getTotalDuration().multiply(value / 100.0)).toSeconds();
        } else {
            return 0;
        }
    }

    public void changeSongPos(double pos) {
        if(mediaPlayer != null) {
            mediaPlayer.seek(mediaPlayer.getStartTime().add(mediaPlayer.getTotalDuration().multiply(pos / 100.0)));
            if(songTimeline != null) {
                songTimeline.play();
            }
        }
    }
}

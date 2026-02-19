package com.berrygobbler78.flacplayer.util;

import io.github.selemba1000.*;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MediaTransport {
    private final JMTC JMTC_OBJECT;
    private final JMTCCallbacks CALLBACKS;

    private String currentSong = "";
    private Path copied;

    public MediaTransport(String playerName, String playerPath, MusicPlayer musicPlayer) {
        JMTC_OBJECT = JMTC.getInstance(
                new JMTCSettings(playerName,playerPath)
        );

        CALLBACKS = new JMTCCallbacks();
        CALLBACKS.onPlay = musicPlayer::play;
        CALLBACKS.onPause = musicPlayer::pause;
        CALLBACKS.onStop = musicPlayer::stop;

        CALLBACKS.onNext = musicPlayer::next;
        CALLBACKS.onPrevious = musicPlayer::previous;

        CALLBACKS.onLoop = musicPlayer::setRepeatStatus;
        CALLBACKS.onShuffle = musicPlayer::setShuffleStatus;

        CALLBACKS.onVolume = musicPlayer::setVolume;

        JMTC_OBJECT.setCallbacks(CALLBACKS);
        JMTC_OBJECT.setMediaType(JMTCMediaType.Music);
        JMTC_OBJECT.setPosition(400L);
        JMTC_OBJECT.setEnabled(true);
    }

    public void setEnabled(boolean enabled) {
        JMTC_OBJECT.setEnabled(enabled);
    }

    public void setProperties(String songTitle, String songArtist, String parentTitle, String parentArtist, int tracks, int index, File coverArt) {
        Task<Void> update = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (!currentSong.equals(songTitle)) {
                    try {
                        Path original = coverArt.toPath();
                        File currentArt = File.createTempFile("currentArt", "");
                        currentArt.deleteOnExit();
                        currentArt.setReadable(false, false);
                        currentArt.setWritable(false, false);
                        currentArt.deleteOnExit();
                        copied = currentArt.toPath();
                        Files.copy(original, copied, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }


                JMTC_OBJECT.setMediaProperties(
                        new JMTCMusicProperties(
                                songTitle,
                                songArtist,
                                parentTitle,
                                parentArtist,
                                new String[]{},
                                1,
                                1,
                                new File(copied.toString())
                        )
                );
                JMTC_OBJECT.updateDisplay();

                return null;
            }
        };

        new Thread(update).start();
    }

    public void setTimeline(long start, long end, long seekStart, long seekEnd) {
        JMTC_OBJECT.setTimelineProperties(
                new JMTCTimelineProperties(
                        0L,
                        100000L,
                        0L,
                        100000L
                )
        );
        JMTC_OBJECT.updateDisplay();
    }

    public void setState(JMTCPlayingState state) {
        JMTC_OBJECT.setPlayingState(state);
        JMTC_OBJECT.updateDisplay();
    }


}

package com.berrygobbler78.flacplayer.util;

import io.github.selemba1000.*;

import java.io.File;

public class MediaTransport {
    private final JMTC JMTC_OBJECT;
    private final JMTCCallbacks CALLBACKS;

    public MediaTransport(String playerName, String playerPath, MusicPlayer musicPlayer) {
        JMTC_OBJECT = JMTC.getInstance(
                new JMTCSettings(playerName,playerPath)
        );

        CALLBACKS = new JMTCCallbacks();
        CALLBACKS.onPlay = musicPlayer::play;
        CALLBACKS.onPause = musicPlayer::pause;
        CALLBACKS.onStop = musicPlayer::stop;

        JMTC_OBJECT.setCallbacks(CALLBACKS);
        JMTC_OBJECT.setMediaType(JMTCMediaType.Music);
        JMTC_OBJECT.setPosition(100L);
    }

    public void setEnabled(boolean enabled) {
        JMTC_OBJECT.setEnabled(enabled);
    }

    public void setProperties(String songTitle, String songArtist, String parentTitle, String parentArtist, int tracks, int index, File coverArt) {
        JMTC_OBJECT.setMediaProperties(
                new JMTCMusicProperties(
                        songTitle,
                        songArtist,
                        parentTitle,
                        parentArtist,
                        new String[]{},
                        tracks,
                        index,
                        coverArt
                )
        );
        JMTC_OBJECT.updateDisplay();
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

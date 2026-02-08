package com.berrygobbler78.flacplayer.util;

import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class FlacDecoder implements PCMProcessor {
    private final Logger LOGGER = Logger.getLogger(FlacDecoder.class.getName());

    private static WavWriter wav;

    public void flacToWav(String inPath, String outPath) {
        try (FileInputStream is = new FileInputStream(inPath); FileOutputStream os = new FileOutputStream(outPath)) {
            wav = new WavWriter(os);
            FLACDecoder flacDecoder = new FLACDecoder(is);
            flacDecoder.addPCMProcessor(this);
            flacDecoder.decode();
        } catch (IOException e) {
            LOGGER.warning("Error while converting file " + inPath);
        }
    }

    @Override
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            LOGGER.warning("Error while processing stream info: " + info);
        }
    }

    @Override
    public void processPCM(ByteData pcm) {
        try {
            wav.writePCM(pcm);
        } catch (IOException e) {
            LOGGER.warning("Error while processing PCM: " + pcm);
        }
    }
}

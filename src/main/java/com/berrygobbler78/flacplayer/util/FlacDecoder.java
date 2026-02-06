package com.berrygobbler78.flacplayer.util;

import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FlacDecoder implements PCMProcessor {
    private static WavWriter wav;

    public void flacToWav(String inFileName, String outFileName) {
        try (FileInputStream is = new FileInputStream(inFileName); FileOutputStream os = new FileOutputStream(outFileName)) {
            wav = new WavWriter(os);
            FLACDecoder flacDecoder = new FLACDecoder(is);
            flacDecoder.addPCMProcessor(this);
            flacDecoder.decode();
        } catch (IOException e) {
            System.err.println("Some");
        }
    }
    @Override
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            System.err.println("Couldn't process stream info: " + e);
        }
    }

    @Override
    public void processPCM(ByteData pcm) {
        try {
            wav.writePCM(pcm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

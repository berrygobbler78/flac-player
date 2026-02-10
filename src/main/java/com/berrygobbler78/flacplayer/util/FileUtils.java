package com.berrygobbler78.flacplayer.util;

import com.berrygobbler78.flacplayer.App;
import com.berrygobbler78.flacplayer.Constants.*;

import com.berrygobbler78.flacplayer.userdata.Playlist;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture;
import org.jaudiotagger.tag.flac.FlacTag;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;
import java.util.logging.Logger;

public class FileUtils {
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    public enum FILE_TYPE{
        SONG,
        ALBUM,
        ARTIST,
        PLAYLIST
    }

    public enum FILTER_TYPE {
        FOLDER,
        FLAC,
        COVER_IMAGE,
        COVER_ICON
    }

    private static final FlacDecoder DECODER = new FlacDecoder();

    public static FileFilter getFileFilter(FILTER_TYPE filterType) throws NullPointerException {
        switch (filterType) {
            case FOLDER -> {return File::isDirectory;}
            case FLAC -> {return f -> f.getAbsolutePath().endsWith(".flac");}
            case COVER_IMAGE -> {return f -> f.getName().endsWith("coverImage.png");}
            case COVER_ICON -> {return f -> f.getName().endsWith("coverIcon.png");}
            default -> throw new NullPointerException("Unknown filter type:"  + filterType);
        }
    }

    public static void refreshCoverArt() {
        for (File artistFolder : Objects.requireNonNull(new File(App.references.getRootDirectoryPath()).listFiles(getFileFilter(FILTER_TYPE.FOLDER)))) {
            for (File albumFolder : Objects.requireNonNull(artistFolder.listFiles(getFileFilter(FILTER_TYPE.FOLDER)))) {
                if (Objects.requireNonNull(albumFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))).length == 0) {
                    try {
                        File coverImage = new File(albumFolder, "coverImage.png");
                        BufferedImage coverBufferedImage =
                                bufferedImageFromSong(Objects.requireNonNull(albumFolder.listFiles())[0].getAbsolutePath(), 600, 600);
                        ImageIO.write(makeRoundedCorner(coverBufferedImage, 50), "png", coverImage);
                    } catch (IOException e) {
                        LOGGER.severe("Failed to create coverImage for: " + albumFolder.getAbsolutePath());
                    }
                }
                if (Objects.requireNonNull(albumFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON))).length == 0) {
                    try {
                        File coverIcon = new File(albumFolder, "coverIcon.png");
                        BufferedImage coverBufferedImage =
                                bufferedImageFromSong(Objects.requireNonNull(albumFolder.listFiles())[0].getAbsolutePath(), 600, 600);
                        ImageIO.write(resizeBufferedImage(coverBufferedImage, 20, 20), "png", coverIcon);
                    } catch (IOException e) {
                        LOGGER.severe("Failed to create coverIcon for: " + albumFolder.getAbsolutePath());
                    }
                }
            }
        }

        if(!new File("src/main/resources/com/berrygobbler78/flacplayer/graphics/playlist-art").exists()) {
            return;
        }

        for(File playlistFolder : Objects.requireNonNull(new File("src/main/resources/com/berrygobbler78/flacplayer/graphics/playlist-art").listFiles(getFileFilter(FILTER_TYPE.FOLDER)))) {
            for(Playlist playlist : App.references.getPlaylists()) {
                String playlistName = playlist.getName().toLowerCase().replace(" ", "-");
                if(!playlistFolder.getName().equals(playlistName)) {
                    LOGGER.warning("Playlist name does not match: " + playlistName);
                    return;
                }

                if (Objects.requireNonNull(playlistFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))).length == 0) {
                    try {
                        File coverImage = new File(playlistFolder, "coverImage.png");
                        BufferedImage coverBufferedImage =
                                bufferedImageFromPath(playlistFolder.getAbsolutePath() + "/"+ playlistName + ".png", 600, 600);
                        ImageIO.write(makeRoundedCorner(coverBufferedImage, 50), "png", coverImage);
                    } catch (IOException e) {
                        System.err.println("Error generating image cache with exception: " + e);
                    }
                }

                if (Objects.requireNonNull(playlistFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON))).length == 0) {
                    try {
                        File coverIcon = new File(playlistFolder, "coverIcon.png");
                        BufferedImage coverBufferedImage =
                                bufferedImageFromPath(playlistFolder.getAbsolutePath() + "/"+ playlistName + ".png", 600, 600);
                        ImageIO.write(resizeBufferedImage(coverBufferedImage, 20, 20), "png", coverIcon);
                    } catch (IOException e) {
                        System.err.println("Error generating image cache with exception: " + e);
                    }
                }
            }
        }
    }

    public static String getSongTitle(String songPath) {
        if(!songPath.contains(".flac")) {
            LOGGER.warning("File does not contain flac extension: " + songPath);
            return "Unknown Title";
        }

        String songName = songPath;

        songName = songPath.replace(".flac", "");
        songName= songName.split("[\\\\/]")[songName.split("[\\\\/]").length - 1];
        songName = songName.substring(songName.indexOf("-") + 1).trim();
//        songName = songName.split("\\.")[0].trim();

        return songName;
    }

    public static String getSongAlbum(String songPath) {
        if(!songPath.contains(".flac")) {
            LOGGER.warning("File does not contain flac extension: " + songPath);
            return "Unknown Album";
        }

        return songPath.split("[\\\\/]")[songPath.split("[\\\\/]").length - 2];
    }

    public static String getSongArtist(String songPath) {
        if(!songPath.contains(".flac")) {
            LOGGER.warning("File does not contain flac extension: " + songPath);
            return "Unknown Artist";
        }

        return songPath.split("[\\\\/]")[songPath.split("[\\\\/]").length - 3];
    }

    // This method is pretty slow so try to use it as little as possible
    public static BufferedImage bufferedImageFromSong(String forSongPath, int w, int h) throws NullPointerException {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(forSongPath));
            FlacTag tag = (FlacTag) audioFile.getTag();
            MetadataBlockDataPicture coverPicture = tag.getImages().getFirst();

            BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(coverPicture.getImageData())));
            bi = resizeBufferedImage(bi, w, h);

            return (bi);
        } catch (Exception e) {
            throw new NullPointerException("Couldn't generate buffered image from file path: " + forSongPath);
        }
    }

    public static BufferedImage bufferedImageFromPath(String path, int w, int h) throws NullPointerException{
        try {
            BufferedImage bi = ImageIO.read(new File(path));
            bi = resizeBufferedImage(bi, w, h);

            return (bi);
        } catch (Exception e) {
            throw new NullPointerException("Couldn't generate buffered image from file path: " + path);
        }
    }

    public static Image getCoverImage(String path, FILE_TYPE type) throws NullPointerException {
        File file = new File(path);
        try {
            switch (type) {
                case SONG -> {
                    file = Objects.requireNonNull(file.getParentFile().listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE)))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ALBUM, PLAYLIST -> {
                    file = Objects.requireNonNull(file.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE)))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ARTIST -> {
                    // TODO: Implement custom artist coverImages
                    return null;
                }
                default -> throw new NullPointerException("Invalid FILE_TYPE " + type.name());
            }
        } catch (Exception e) {
            throw new NullPointerException("Couldn't get cover art from path: " + path);
        }
    }

    public static Image getCoverIcon(String path, FILE_TYPE type) throws IOException {
        File file = new File(path);
        try {
            switch (type) {
                case SONG -> {
                    file = Objects.requireNonNull(file.getParentFile().listFiles(getFileFilter(FILTER_TYPE.COVER_ICON)))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ALBUM, PLAYLIST -> {
                    file = Objects.requireNonNull(file.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON)))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ARTIST -> {
                    // TODO: Implement custom artist coverImages
                    return null;
                }
                default -> throw new NullPointerException("Invalid FILE_TYPE " + type.name());
            }
        } catch (Exception e) {
            throw new NullPointerException("Couldn't get cover icon from path: " + path);
        }
    }

    public static void flacToWav(String fileIn, String fileOut) {
        LOGGER.info("Starting decoding for: " + fileIn);
        DECODER.flacToWav(fileIn, fileOut);
        LOGGER.info("Done!");
    }

    public File fileChooser(Stage stage, String title, String directoryPath, String extensionDesc, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(directoryPath));
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter(extensionDesc, extension));

        return fileChooser.showOpenDialog(stage);
    }

    public static File openDirectoryChooser(Stage stage, String title, String atPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(new File(atPath));

        return directoryChooser.showDialog(stage);
    }

    public static void openFileExplorer(String path) {
        try{
            Runtime.getRuntime().exec(new String[]{"explorer /select, " + path});

        } catch (IOException e){
            LOGGER.severe("Couldn't open file explorer: " + path);
        }
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();

        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }

    public static BufferedImage resizeBufferedImage(BufferedImage bi, int width, int height) {
        java.awt.Image tempImage = bi.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);

        bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = bi.createGraphics();
        g2d.drawImage(tempImage, 0, 0, null);
        g2d.dispose();

        return bi;
    }

    public static Image bufferedImageToFxImage(BufferedImage image) throws NullPointerException {
        if(image == null) throw new NullPointerException("Image is null");

        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());

        PixelWriter pw = wr.getPixelWriter();

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pw.setArgb(x, y, image.getRGB(x, y));
            }
        }

        return new ImageView(wr).getImage();
    }
}

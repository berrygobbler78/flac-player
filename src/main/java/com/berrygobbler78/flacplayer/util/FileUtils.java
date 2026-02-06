package com.berrygobbler78.flacplayer.util;

import com.berrygobbler78.flacplayer.App;
import com.berrygobbler78.flacplayer.Enums.*;

import com.berrygobbler78.flacplayer.userdata.Playlist;
import com.berrygobbler78.flacplayer.userdata.References;
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

public class FileUtils {
    private static final FlacDecoder decoder = new FlacDecoder();

    public static FileFilter getFileFilter(FILTER_TYPE filterType) {
        switch (filterType) {
            case FOLDER -> {return File::isDirectory;}
            case FLAC -> {return f -> f.getName().endsWith(".flac");}
            case COVER_IMAGE -> {return f -> f.getName().endsWith("coverImage.png");}
            case COVER_ICON -> {return f -> f.getName().endsWith("coverIcon.png");}
            default ->  {return null;}
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
                        assert coverBufferedImage != null;
                        ImageIO.write(makeRoundedCorner(coverBufferedImage, 50), "png", coverImage);
                    } catch (IOException e) {
                        System.err.println("Error generating image cache with exception: " + e);
                    }
                }
                if (Objects.requireNonNull(albumFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON))).length == 0) {
                    try {
                        File coverIcon = new File(albumFolder, "coverIcon.png");
                        BufferedImage coverBufferedImage =
                                bufferedImageFromSong(Objects.requireNonNull(albumFolder.listFiles())[0].getAbsolutePath(), 600, 600);
                        assert coverBufferedImage != null;
                        ImageIO.write(resizeBufferedImage(coverBufferedImage, 20, 20), "png", coverIcon);
                    } catch (IOException e) {
                        System.err.println("Error generating image cache with exception: " + e);
                    }
                }
            }
        }

        for(File playListFolder : new File("src/main/resources/com/berrygobbler78/flacplayer/graphics/playlist-art").listFiles(getFileFilter(FILTER_TYPE.FOLDER))) {
            for(Playlist playlist : App.references.getPlaylists()) {
                String playlistName = playlist.getName().toLowerCase().replace(" ", "-");
                if(playListFolder.getName().equals(playlistName)) {
                    if (Objects.requireNonNull(playListFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))).length == 0) {
                        try {
                            File coverImage = new File(playListFolder, "coverImage.png");
                            BufferedImage coverBufferedImage =
                                    bufferedImageFromFile(playListFolder.getAbsolutePath() + "/"+ playlistName + ".png", 600, 600);
                            assert coverBufferedImage != null;
                            ImageIO.write(makeRoundedCorner(coverBufferedImage, 50), "png", coverImage);
                        } catch (IOException e) {
                            System.err.println("Error generating image cache with exception: " + e);
                        }
                    }
                    if (Objects.requireNonNull(playListFolder.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON))).length == 0) {
                        try {
                            File coverIcon = new File(playListFolder, "coverIcon.png");
                            BufferedImage coverBufferedImage =
                                    bufferedImageFromFile(playListFolder.getAbsolutePath() + "/"+ playlistName + ".png", 600, 600);
                            assert coverBufferedImage != null;
                            ImageIO.write(resizeBufferedImage(coverBufferedImage, 20, 20), "png", coverIcon);
                        } catch (IOException e) {
                            System.err.println("Error generating image cache with exception: " + e);
                        }
                    }
                }
            }
        }
    }

    public static String getSongTitle(String filePath) {
        if(!filePath.contains(".flac")) {
            return "Unknown Title";
        }

        String backslash = "[\\\\/]";
        int index = filePath.split(backslash).length - 1;
        filePath = filePath.split(backslash)[index];

        filePath = filePath.substring(filePath.indexOf(".") + 1);

        filePath = filePath.split("-")[0].trim();

        return filePath;
    }

    public static String getSongAlbum(String filePath) {
        if(!filePath.contains(".flac")) {
            return "Unknown Artist";
        }

        String backslash = "[\\\\/]";
        int index = filePath.split(backslash).length - 2;
        filePath = filePath.split(backslash)[index];

        return filePath;
    }

    public static String getSongArtist(String filePath) {
        if(!filePath.contains(".flac")) {
            return "Unknown Artist";
        }

        String backslash = "[\\\\/]";
        int index = filePath.split(backslash).length - 3;
        filePath = filePath.split(backslash)[index];

        return filePath;
    }

    // This method is pretty slow so try to use it as little as possible
    public static BufferedImage bufferedImageFromSong(String forSongPath, int w, int h) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(forSongPath));
            FlacTag tag = (FlacTag) audioFile.getTag();
            MetadataBlockDataPicture coverPicture = tag.getImages().getFirst();

            BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(coverPicture.getImageData())));
            bi = resizeBufferedImage(bi, w, h);

            return (bi);
        } catch (Exception e) {
            System.err.println("Couldn't generate buffered image from file path: " + forSongPath);
            return null;
        }
    }

    public static BufferedImage bufferedImageFromFile(String forFilePath, int w, int h) {
        try {

            BufferedImage bi = ImageIO.read(new File(forFilePath));
            bi = resizeBufferedImage(bi, w, h);

            return (bi);
        } catch (Exception e) {
            System.err.println("Couldn't generate buffered image from file path: " + forFilePath);
            return null;
        }
    }

    public static Image getCoverImage(String forPath, FILE_TYPE type) throws IOException {
        File file = new File(forPath);
        try {
            switch (type) {
                case SONG -> {
                    file = file.getParentFile().listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ALBUM -> {
                    file = file.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                case ARTIST -> {
                    // TODO: Implement custom artist coverImages
                    return null;
                }
                case PLAYLIST ->  {
                    file = file.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE))[0];
                    return bufferedImageToFxImage(ImageIO.read(file));
                }
                default -> {
                    return null;
                }
            }
        } catch (IOException e) {
            System.err.println("Couldn't get cover image from file path: " + forPath);
            return null;
        }

    }

    public static Image getCoverIcon(String forPath, FILE_TYPE type) throws IOException {
        File file = new File(forPath);

        switch (type) {
            case SONG -> {
                file = Objects.requireNonNull(file.getParentFile().listFiles(getFileFilter(FILTER_TYPE.COVER_ICON)))[0];
                return bufferedImageToFxImage(ImageIO.read(file));
            }
            case ALBUM -> {
                file = Objects.requireNonNull(file.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON)))[0];
                return bufferedImageToFxImage(ImageIO.read(file));
            }
            case ARTIST -> {
                //     TODO: Implement custom artist coverImages
                return null;
            }
            case PLAYLIST ->  {
                file = Objects.requireNonNull(file.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON)))[0];
                return bufferedImageToFxImage(ImageIO.read(file));
            }
            default -> {
                return null;
            }
        }
    }

    public static void flacToWav(String fileIn, String fileOut) {
        decoder.flacToWav(fileIn, fileOut);
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

    public static void openFileExplorer(String atPath) {
        try{
            Runtime.getRuntime().exec("explorer /select, " + atPath);

        } catch (IOException e){
            System.err.println("Couldn't open directory explorer: " + e);
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

        // ... then compositing the image on top,
        // using the white shape from above as alpha source
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

    public static Image bufferedImageToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }

        return new ImageView(wr).getImage();
    }

    public static Icon bufferedImageToIcon(BufferedImage image) {
        return new ImageIcon(image);
    }

}

package com.berrygobbler78.flacplayer.util;

import com.berrygobbler78.flacplayer.App;

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
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
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

    private static final String albumArtDir = "src/main/resources/com/berrygobbler78/flacplayer/cache/album-art/";

    private static final FlacDecoder DECODER = new FlacDecoder();

    public static FileFilter getFileFilter(FILTER_TYPE filterType) throws NullPointerException {
        switch (filterType) {
            case FOLDER -> {return File::isDirectory;}
            case FLAC -> {return f -> f.getAbsolutePath().endsWith(".flac");}
            case COVER_IMAGE -> {return f -> f.getAbsolutePath().endsWith("coverImage.png");}
            case COVER_ICON -> {return f -> f.getAbsolutePath().endsWith("coverIcon.png");}
            default -> throw new NullPointerException("Unknown filter type:"  + filterType);
        }
    }

    public static void refreshAllArt() {
        refreshAlbumArt();
        refreshPlaylistArt();
    }

    public static void refreshAlbumArt() {
        LOGGER.info("Refreshing album art...");

        File rootDir = new File(App.getReferences().getRootDirectoryPath());
        if(!rootDir.exists() || !rootDir.isDirectory()) {
            LOGGER.warning("Root directory does not exist or is not a directory. Path:" + rootDir.getAbsolutePath());
        } else {
            File[] artistFolders = rootDir.listFiles(getFileFilter(FILTER_TYPE.FOLDER));
            if(artistFolders == null || artistFolders.length == 0) {
                LOGGER.warning("No artist files found. Path:" + rootDir.getAbsolutePath());
            } else {
                for (File artistFolder : artistFolders) {
                    // Image caching artist directory
                    File localArtistFile = new File(albumArtDir + artistFolder.getName().toLowerCase().replace(' ', '-').trim());
                    if(!localArtistFile.exists()) {
                        if(!localArtistFile.mkdirs()) {
                            LOGGER.warning(String.format("Failed to create local artist file %s", localArtistFile.getAbsolutePath()));
                        }
                    }

                    File[] albumFolders = artistFolder.listFiles(getFileFilter(FILTER_TYPE.FOLDER));
                    if(albumFolders == null || albumFolders.length == 0) {
                        LOGGER.warning("No album files found. Path:" + artistFolder.getAbsolutePath());
                    } else {
                        for (File albumFolder : albumFolders) {
                            // Image caching album directory
                            File localAlbumFile = new File(localArtistFile, albumFolder.getName().toLowerCase().replace(' ', '-').trim());
                            if(!localAlbumFile.exists()) {
                                if(!localAlbumFile.mkdirs()) {
                                    LOGGER.warning(String.format("Failed to create local album file %s", localArtistFile.getAbsolutePath()));
                                } else {
                                    LOGGER.info(String.format("Created local album file %s", localAlbumFile.getAbsolutePath()));
                                }
                            }

                            File[] songFiles = albumFolder.listFiles(getFileFilter(FILTER_TYPE.FLAC));
                            if(songFiles == null || songFiles.length == 0) {
                                LOGGER.warning("Song files not found. Path:" + localArtistFile.getAbsolutePath());
                            } else {
                                File[] coverImages = localAlbumFile.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE));
                                if(coverImages == null || coverImages.length == 0) {
                                    LOGGER.warning("No cover image files found, generating new image. Path:" + albumFolder.getAbsolutePath());
                                    File coverImage = new File(localAlbumFile, "coverImage.png");
                                    try{
                                        BufferedImage coverBufferedImage =
                                                bufferedImageFromSong(songFiles[0].getAbsolutePath(), 600, 600);
                                        ImageIO.write(makeRoundedCorner(coverBufferedImage, 50), "png", coverImage);
                                    } catch (Exception e) {
                                        LOGGER.warning("Error while writing cover image file: " + songFiles[0].getAbsolutePath() + "Error:" + e);
                                    }

                                    LOGGER.info("Generated cover image for " + albumFolder.getName());
                                }

                                File[] coverIcons = localAlbumFile.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON));
                                if(coverIcons == null || coverIcons.length == 0) {
                                    LOGGER.warning("No cover icon files found, generating new icon. Path:" + albumFolder.getAbsolutePath());
                                    File coverImage = new File(localAlbumFile, "coverIcon.png");
                                    try{
                                        BufferedImage coverBufferedImage =
                                                bufferedImageFromSong(songFiles[0].getAbsolutePath(), 20, 20);
                                        ImageIO.write(makeRoundedCorner(coverBufferedImage, 2), "png", coverImage);
                                    } catch (Exception e) {
                                        LOGGER.warning("Error while writing cover icon file: " + songFiles[0].getAbsolutePath() + "Error:" + e);
                                    }

                                    LOGGER.info("Generated cover icon for " + albumFolder.getName());
                                }
                            }
                        }
                    }
                }
            }
        }

        LOGGER.info("Album art has been refreshed");
    }

    public static void refreshPlaylistArt() {
        LOGGER.info("Refreshing playlist art...");

        File playlistDir = new File("src/main/resources/com/berrygobbler78/flacplayer/cache/playlist-art");
        if(!playlistDir.exists() || !playlistDir.isDirectory()) {
            LOGGER.warning("Playlist directory does not exist or is not a directory. Path:" + playlistDir.getAbsolutePath());
            return;
        }

        if(App.getReferences().getPlaylists() == null) {
            LOGGER.info("No playlists to be added");
            return;
        }

        for(Playlist playlist : App.getReferences().getPlaylists()) {
            File playlistFolder = new File(playlistDir, playlist.getName().toLowerCase().replace(' ', '-').trim());
            if(!playlistFolder.exists() || !playlistFolder.isDirectory()) {
                if(playlistFolder.mkdirs()) {
                    LOGGER.info("Created playlist folder: " + playlistFolder.getAbsolutePath());
                } else {
                    LOGGER.warning("Failed to create playlist folder: " + playlistFolder.getAbsolutePath());
                }
            }
        }

        // TODO: Generate image from top 4 songs if no image is provided

        LOGGER.info("Playlist art has been refreshed.");
    }

    public static String getSongTitle(String songPath) {
        // FIXME: user defined song naming convention
        if(!songPath.contains(".flac")) {
            LOGGER.warning("File does not contain flac extension: " + songPath);
            return "Unknown Title";
        }

        String songName = songPath.replace(".flac", "");
        songName= songName.split("[\\\\/]")[songName.split("[\\\\/]").length - 1];
        songName = songName.substring(songName.indexOf("-") + 1).trim();

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
    public static BufferedImage bufferedImageFromSong(String forSongPath, int w, int h) throws Exception {
        AudioFile audioFile = AudioFileIO.read(new File(forSongPath));
        FlacTag tag = (FlacTag) audioFile.getTag();
        MetadataBlockDataPicture coverPicture = tag.getImages().getFirst();

        BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(coverPicture.getImageData())));
        return resizeBufferedImage(bi, w, h);
    }

    public static BufferedImage bufferedImageFromPath(String path, int w, int h) throws Exception{
        try {
            BufferedImage bi = ImageIO.read(new File(path));
            return resizeBufferedImage(bi, w, h);
        } catch (IOException e) {
            LOGGER.warning("Couldn't generate buffered image from file path: " + path);
            return null;
        }
    }

    public static Image getCoverImage(String path, FILE_TYPE type) throws NullPointerException {
        File file = new File(path);

        switch (type) {
            case SONG -> {
                String artistString = getSongArtist(path).replace(" ", "-").toLowerCase();
                String albumString = file.getParentFile().getName().replace(" ", "-").toLowerCase().trim();

                File[] images = new File(albumArtDir + artistString + File.separator + albumString).listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE));

                if(images == null || images.length == 0) {
                    return Constants.IMAGES.WARNING.get();
                }

                try {
                    return bufferedImageToFxImage(ImageIO.read(images[0]));
                } catch (IOException e) {
                    return Constants.IMAGES.WARNING.get();
                }
            }
            case ALBUM, PLAYLIST -> {
                String artistString = file.getParentFile().getName().replace(" ", "-").toLowerCase();
                String albumString = file.getName().replace(" ", "-").toLowerCase().trim();

                File dir =  new File(albumArtDir + artistString + File.separator + albumString);

                File[] images = dir.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE));

                if(images == null || images.length == 0) {
                    return Constants.IMAGES.WARNING.get();
                }

                try {
                    return bufferedImageToFxImage(ImageIO.read(images[0]));
                } catch (IOException e) {
                    return Constants.IMAGES.WARNING.get();
                }
            }
            case ARTIST -> {
                // TODO: Implement custom artist coverImages
                return null;
            }
            default -> throw new NullPointerException("Invalid FILE_TYPE " + type.name());
        }
    }

    public static File getCoverImageFile(String path, FILE_TYPE type) throws NullPointerException {
        File file = new File(path);

        try {
            switch (type) {
                case SONG -> {
                    String artistString = getSongArtist(path).replace(" ", "-").toLowerCase();
                    String albumString = file.getParentFile().getName().replace(" ", "-").toLowerCase().trim();

                    File[] files = new File(albumArtDir + artistString + File.separator + albumString).listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE));
                    if(files == null || files.length == 0) {
                        return null;
                    } else {
                        return files[0];
                    }
                }
                case ALBUM, PLAYLIST -> {
                    String artistString = file.getParentFile().getName().replace(" ", "-").toLowerCase();
                    String albumString = file.getName().replace(" ", "-").toLowerCase().trim();

                    File dir =  new File(albumArtDir + artistString + File.separator + albumString);

                    File[] files = dir.listFiles(getFileFilter(FILTER_TYPE.COVER_IMAGE));
                    if(files == null || files.length == 0) {
                        return null;
                    } else {
                        return files[0];
                    }
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
                    String artistString = getSongArtist(path).replace(" ", "-").toLowerCase();
                    String albumString = file.getParentFile().getName().replace(" ", "-").toLowerCase().trim();

                    File[] icons = new File(albumArtDir + artistString + File.separator + albumString).listFiles(getFileFilter(FILTER_TYPE.COVER_ICON));

                    if(icons == null || icons.length == 0) {
                        return Constants.IMAGES.WARNING.get();
                    }

                    try {
                        return bufferedImageToFxImage(ImageIO.read(icons[0]));
                    } catch (IOException e) {
                        return Constants.IMAGES.WARNING.get();
                    }
                }
                case ALBUM -> {
                    String artistString = file.getParentFile().getName().replace(" ", "-").toLowerCase();
                    String albumString = file.getName().replace(" ", "-").toLowerCase().trim();

                    File dir =  new File(albumArtDir + artistString + File.separator + albumString);

                    File[] icons = dir.listFiles(getFileFilter(FILTER_TYPE.COVER_ICON));

                    if(icons == null || icons.length == 0) {
                        return Constants.IMAGES.WARNING.get();
                    }

                    try {
                        return bufferedImageToFxImage(ImageIO.read(icons[0]));
                    } catch (IOException e) {
                        return Constants.IMAGES.WARNING.get();
                    }
                }
                case PLAYLIST -> {
                    File[] images = file.listFiles(f -> f.getAbsolutePath().endsWith(".png"));
                    if(images == null || images.length == 0) {
                        return Constants.IMAGES.WARNING.get();
                    }
                    for(File image : images) {
                        if(image.getName().equals(file.getName() +  ".png")) {
                            return bufferedImageToFxImage(ImageIO.read(image));
                        } else if(image.getName().equals(file.getName() + "-temp.png")) {
                            return bufferedImageToFxImage(ImageIO.read(image));
                        } else {
                            return Constants.IMAGES.WARNING.get();
                        }
                    }
                }
                case ARTIST -> {
                    // TODO: Implement custom artist coverImages
                    return null;
                }
                default -> {
                    return Constants.IMAGES.WARNING.get();
                }
            }
        } catch (Exception e) {
            return Constants.IMAGES.WARNING.get();
        }

        return Constants.IMAGES.WARNING.get();
    }

    public static File flacToWav(String fileIn) throws IOException {
        LOGGER.info("Starting decoding for: " + fileIn);
        File temp = File.createTempFile("current", ".wav");
        temp.deleteOnExit();

        DECODER.flacToWav(fileIn, temp.getAbsolutePath());
        LOGGER.info("Done!");

        return temp;
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
        try {
            switch (App.getCurrentOS()) {
                case LINUX -> Runtime.getRuntime().exec(new String[]{"xdg-open", path});
                case WINDOWS_11 -> Runtime.getRuntime().exec(new String[]{"cmd", "/c", "explorer/select/" + path});
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to open file explorer for path: " + path);
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

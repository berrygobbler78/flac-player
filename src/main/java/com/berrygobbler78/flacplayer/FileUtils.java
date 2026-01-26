package com.berrygobbler78.flacplayer;

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
import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;

public class FileUtils implements PCMProcessor {

    public String directoryPath = "C:\\Users\\wgari\\Music\\The Archive";

    private WavWriter wav;

    public String getSongTitle(File file) {
        return file.getName().replace(".flac", "").substring(file.getName().indexOf(".")+1, file.getName().replace(".flac", "").lastIndexOf("-")).trim();
    }

    public String getSongAlbumTitle(File file) {
        return new File(file.getParent()).getName();
    }

    public String getSongArtist(File file) {
        return new File(new File(file.getParent()).getParent()).getName();
    }

    public Image iconToImage(Icon icon) {
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        try {
            icon.paintIcon(null, g2d, 0, 0);
        } finally {
            g2d.dispose();
        }

        return convertToFxImage(bi);
    }

    public Image getFileIcon(File file) {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        return iconToImage(fsv.getSystemIcon(file));

    }

    public BufferedImage getAlbumImage(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            FlacTag tag = (FlacTag) audioFile.getTag();
            MetadataBlockDataPicture albumCoverPicture = tag.getImages().getFirst();
            BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(albumCoverPicture.getImageData())));
            bi = resizeBufferedImage(bi, 600, 600);

            return bi;
        } catch (Exception e) {
            System.err.println("Error: " + e);
            return null;
        }
    }

    public BufferedImage getRoundedAlbumImage(File file, int cornerRadius) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            FlacTag tag = (FlacTag) audioFile.getTag();
            MetadataBlockDataPicture albumCoverPicture = tag.getImages().getFirst();
            BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(albumCoverPicture.getImageData())));
            bi = resizeBufferedImage(bi, 600, 600);
            bi = makeRoundedCorner(bi, cornerRadius);

            return bi;
        } catch (Exception e) {
            System.err.println("Error: " + e);
            return null;
        }
    }

    public BufferedImage getAlbumImage(File file, int w, int h) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            FlacTag tag = (FlacTag) audioFile.getTag();
            MetadataBlockDataPicture albumCoverPicture = tag.getImages().getFirst();
            BufferedImage bi = ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(albumCoverPicture.getImageData())));
            bi = resizeBufferedImage(bi, w, h);


            return (bi);
        } catch (Exception e) {
            System.err.println("Error: " + e);
            return null;
        }
    }

    public Image getAlbumFXImage(File file, String albumOrPlaylist) throws IOException {
        File albumArtImage;
        return switch (albumOrPlaylist) {
            case "album" -> {
                albumArtImage = new File(file, "albumArtImage.png");

                yield new Image(albumArtImage.toURI().toString());
            }
            case "playlist" -> {
                albumArtImage = new File(new File(file.getParent()), "albumArtImage.png");
                yield new Image(albumArtImage.toURI().toString());
            }
            default -> {
                System.err.println("Error: Wrong type inserted in getAlbumFXImage");
                yield null;
            }
        };
    }

    public Image getAlbumFXIcon(File file, String albumOrSong) {
        File albumArtIcon;
        return switch (albumOrSong) {
            case "album" -> {
                albumArtIcon = new File(file, "albumArtIcon.png");
                yield new Image(albumArtIcon.toURI().toString());
            }
            case "song" -> {
                albumArtIcon = new File(new File(file.getParent()), "albumArtIcon.png");
                yield new Image(albumArtIcon.toURI().toString());
            }
            default -> null;
        };
    }

    public BufferedImage resizeBufferedImage(BufferedImage bi, int width, int height) {
        java.awt.Image tempImage = bi.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();

        g2d.drawImage(tempImage, 0, 0, null);
        g2d.dispose();
        return bi;
    }

    public void flacToWav(String inFileName, String outFileName) {
        try (FileInputStream is = new FileInputStream(inFileName); FileOutputStream os = new FileOutputStream(outFileName)) {
            wav = new WavWriter(os);
            FLACDecoder flacDecoder = new FLACDecoder(is);
            flacDecoder.addPCMProcessor(this);
            flacDecoder.decode();
        } catch (IOException e) {
            System.err.println("Some");
        }
        // TODO Auto-generated catch block
        // TODO Auto-generated catch block
    }

    @Override
    public void processStreamInfo(StreamInfo info) {
        try {
            wav.writeHeader(info);
        } catch (IOException e) {
            e.printStackTrace();
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

    public File fileChooser(Stage stage, String title, String directoryPath, String extensionDesc, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(directoryPath));
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter(extensionDesc, extension));

        return fileChooser.showOpenDialog(stage);
    }

    public File directoryChooser(Stage stage, String title, String directoryPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(new File(directoryPath));

        return directoryChooser.showDialog(stage);
    }

    public void openDirectoryExplorer() {
        try{
            Runtime.getRuntime().exec("explorer /select, " + directoryPath);

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void changeDirectoryPath(String newPath) {
        this.directoryPath = newPath;
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
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

    public Image convertToFxImage(BufferedImage image) {
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

}

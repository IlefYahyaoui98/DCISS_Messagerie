package fr.uga.miashs.dciss.chatservice.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageTransferTest {

    private ImageTransfer imageTransfer;
    private byte[] sampleImageData;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Créer une image test
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        File tempFile = tempDir.resolve("test.png").toFile();
        ImageIO.write(image, "png", tempFile);

        // Créer un ImageTransfer pour les tests
        imageTransfer = ImageTransfer.fromImage(tempFile);
        sampleImageData = imageTransfer.getImageData();
    }

    @Test
    void testConstructor() {
        ImageTransfer it = new ImageTransfer("test.png", "png", sampleImageData);

        assertEquals("test.png", it.getImageName());
        assertEquals("png", it.getImageFormat());
        assertArrayEquals(sampleImageData, it.getImageData());
    }

    @Test
    void testFromImage() throws IOException {
        // Créer une nouvelle image test
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        File imageFile = tempDir.resolve("fromImage.png").toFile();
        ImageIO.write(image, "png", imageFile);

        ImageTransfer it = ImageTransfer.fromImage(imageFile);

        assertEquals("fromImage.png", it.getImageName());
        assertEquals("png", it.getImageFormat());
        assertNotNull(it.getImageData());
        assertTrue(it.getImageData().length > 0);
    }

    @Test
    void testSerializationDeserialization() throws IOException {
        // Tester la conversion en bytes et retour
        byte[] serialized = ImageTransfer.toBytes(imageTransfer);
        ImageTransfer deserialized = ImageTransfer.fromBytes(serialized);

        assertEquals(imageTransfer.getImageName(), deserialized.getImageName());
        assertEquals(imageTransfer.getImageFormat(), deserialized.getImageFormat());
        assertArrayEquals(imageTransfer.getImageData(), deserialized.getImageData());
    }

    @Test
    void testSaveImage() throws IOException {
        File outputFile = tempDir.resolve("output.png").toFile();
        imageTransfer.saveImage(outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        // Vérifier que l'image peut être relue
        BufferedImage savedImage = ImageIO.read(outputFile);
        assertNotNull(savedImage);
    }

    @Test
    void testInvalidImage() {
        File nonExistentFile = tempDir.resolve("nonexistent.png").toFile();

        assertThrows(IOException.class, () -> {
            ImageTransfer.fromImage(nonExistentFile);
        });
    }

    @Test
    void testUnsupportedFormat() throws IOException {
        // Créer un fichier texte avec extension .txt
        File textFile = tempDir.resolve("test.txt").toFile();
        textFile.createNewFile();

        IOException exception = assertThrows(IOException.class, () -> {
            ImageTransfer.fromImage(textFile);
        });

        // Vérifier le message d'erreur
        assertTrue(exception.getMessage().contains("Format d'image non supporté"));
    }
}
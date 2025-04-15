package fr.uga.miashs.dciss.chatservice.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageTransfer {
    private String imageName;
    private String imageFormat; // "png", "jpg", etc.
    private byte[] imageData;
    
    // Constructeur
    public ImageTransfer(String imageName, String imageFormat, byte[] imageData) {
        this.imageName = imageName;
        this.imageFormat = imageFormat;
        this.imageData = imageData;
    }
    
    // Getters
    public String getImageName() { return imageName; }
    public String getImageFormat() { return imageFormat; }
    public byte[] getImageData() { return imageData; }
    
    // Convertit une image en bytes pour l'envoi
    public static byte[] toBytes(ImageTransfer it) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        
        dos.writeByte(6); // Type 6 = transfert d'image
        dos.writeUTF(it.imageName);
        dos.writeUTF(it.imageFormat);
        dos.writeInt(it.imageData.length);
        dos.write(it.imageData);
        
        return bos.toByteArray();
    }
    
    // Crée un ImageTransfer depuis des bytes reçus
    public static ImageTransfer fromBytes(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        
        dis.skipBytes(1); // Skip type byte
        String imageName = dis.readUTF();
        String imageFormat = dis.readUTF();
        int length = dis.readInt();
        byte[] imageData = new byte[length];
        dis.readFully(imageData);
        
        return new ImageTransfer(imageName, imageFormat, imageData);
    }
    
    // Méthode utilitaire pour convertir une image en bytes
    public static ImageTransfer fromImage(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        String format = getImageFormat(imageFile.getName());
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, format, bos);
        
        return new ImageTransfer(
            imageFile.getName(),
            format,
            bos.toByteArray()
        );
    }
    
    // Méthode utilitaire pour sauvegarder l'image reçue
    public void saveImage(File outputFile) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
        BufferedImage image = ImageIO.read(bis);
        ImageIO.write(image, imageFormat, outputFile);
    }
    
    // Extrait l'extension du fichier image
    private static String getImageFormat(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "png"; // Format par défaut
    }
}
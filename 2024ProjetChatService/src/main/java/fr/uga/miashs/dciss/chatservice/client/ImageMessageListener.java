package fr.uga.miashs.dciss.chatservice.client;

import fr.uga.miashs.dciss.chatservice.common.ImageTransfer;
import fr.uga.miashs.dciss.chatservice.common.Packet;
import java.io.*;

public class ImageMessageListener implements MessageListener {
    private final String imageDir;
    
    public ImageMessageListener(String imageDirectory) {
        this.imageDir = imageDirectory;
        // Créer le répertoire des images s'il n'existe pas
        new File(imageDir).mkdirs();
    }
    
    @Override
    public void messageReceived(Packet p) {
        try {
            // Vérifier si c'est une image (type 6)
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(p.data));
            byte type = dis.readByte();
            
            if (type == 6) { // Type image
                // Reconstruire l'ImageTransfer
                ImageTransfer imageTransfer = ImageTransfer.fromBytes(p.data);
                
                // Créer le fichier de sortie
                File outputFile = new File(imageDir, imageTransfer.getImageName());
                
                // Sauvegarder l'image
                imageTransfer.saveImage(outputFile);
                
                System.out.println("Image reçue: " + imageTransfer.getImageName() + 
                                 " de " + p.srcId + " (format: " + 
                                 imageTransfer.getImageFormat() + ")");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception de l'image: " + e.getMessage());
        }
    }
}
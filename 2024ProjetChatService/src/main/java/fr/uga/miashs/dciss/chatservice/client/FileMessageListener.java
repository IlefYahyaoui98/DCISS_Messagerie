package fr.uga.miashs.dciss.chatservice.client;

import java.io.*;
import fr.uga.miashs.dciss.chatservice.common.Packet;

public class FileMessageListener implements MessageListener {
    private final String downloadDir;

    public FileMessageListener(String downloadDirectory) {
        this.downloadDir = downloadDirectory;
        // Créer le répertoire de téléchargement s'il n'existe pas
        new File(downloadDir).mkdirs();
    }

    @Override
    public void messageReceived(Packet p) {
        try {
            // Vérifier si c'est un fichier (type 5)
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(p.data));
            byte type = dis.readByte();
            
            if (type == 5) { // Type fichier
                // Lire les métadonnées du fichier
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                
                // Créer le fichier dans le répertoire de téléchargement
                File outputFile = new File(downloadDir, fileName);
                
                // Lire le contenu du fichier
                byte[] fileContent = new byte[(int)fileSize];
                dis.readFully(fileContent);
                
                // Écrire le fichier
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(fileContent);
                }
                
                System.out.println("Fichier reçu: " + fileName + " de " + p.srcId + 
                                 " (" + fileSize + " bytes)");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception du fichier: " + e.getMessage());
        }
    }
}
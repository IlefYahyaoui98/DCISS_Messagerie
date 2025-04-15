package fr.uga.miashs.dciss.chatservice.common;

// Importation des classes nécessaires pour les tests unitaires
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

// Classe de test pour la classe FileTransfer
public class FileTransferTest {

    // Test pour vérifier la sérialisation et la désérialisation d'un objet FileTransfer
    @Test
    public void testSerializationDeserialization() throws IOException {
        // Préparer les données de test : nom du fichier et contenu
        String fileName = "test.txt";
        byte[] content = "Hello World".getBytes(); // Convertir une chaîne en tableau d'octets
        
        // Créer un objet FileTransfer avec les données de test
        FileTransfer original = new FileTransfer(fileName, content);
        
        // Convertir l'objet FileTransfer en un tableau d'octets
        byte[] serialized = FileTransfer.toBytes(original);
        
        // Convertir le tableau d'octets en un objet FileTransfer
        FileTransfer deserialized = FileTransfer.fromBytes(serialized);
        
        // Vérifier que le nom du fichier est préservé après désérialisation
        assertEquals("Le nom du fichier doit être préservé", 
                    fileName, deserialized.getFileName());
        // Vérifier que la taille du fichier est préservée après désérialisation
        assertEquals("La taille du fichier doit être préservée", 
                    content.length, deserialized.getFileSize());
        // Vérifier que le contenu du fichier est préservé après désérialisation
        assertArrayEquals("Le contenu du fichier doit être préservé", 
                         content, deserialized.getFileData());
    }

    // Test pour vérifier que la désérialisation échoue avec des données invalides
    @Test(expected = IOException.class)
    public void testDeserializationInvalidData() throws IOException {
        // Préparer des données invalides (qui ne correspondent pas à un objet FileTransfer)
        byte[] invalidData = "Invalid data".getBytes();
        // Tenter de désérialiser les données invalides (doit lever une IOException)
        FileTransfer.fromBytes(invalidData);
    }

    // Test pour vérifier la sérialisation et désérialisation avec un fichier de grande taille
    @Test
    public void testLargeFile() throws IOException {
        // Préparer un fichier de grande taille (1 Mo)
        String fileName = "large.dat";
        byte[] content = new byte[1024 * 1024]; // Tableau d'octets de 1 Mo
        // Remplir le tableau avec des données aléatoires
        new java.util.Random().nextBytes(content);
        
        // Créer un objet FileTransfer avec les données de test
        FileTransfer original = new FileTransfer(fileName, content);
        // Sérialiser l'objet FileTransfer
        byte[] serialized = FileTransfer.toBytes(original);
        // Désérialiser le tableau d'octets pour recréer l'objet FileTransfer
        FileTransfer deserialized = FileTransfer.fromBytes(serialized);
        
        // Vérifier que le nom du fichier est préservé après désérialisation
        assertEquals(fileName, deserialized.getFileName());
        // Vérifier que la taille du fichier est préservée après désérialisation
        assertEquals(content.length, deserialized.getFileSize());
        // Vérifier que le contenu du fichier est préservé après désérialisation
        assertArrayEquals(content, deserialized.getFileData());
    }
}
package fr.uga.miashs.dciss.chatservice.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControleChat {
    public static void SendMessage(ClientMsg client, String destPseudo, String message,
            Map<String, Integer> pseudoToId) {
        Integer destId = pseudoToId.get(destPseudo.toLowerCase());
        if (destId == null) {
            System.out.println("Pseudo inconnu !");
            return;
        }

        client.sendPacket(destId, message.getBytes());
    }

    public static void CreateGroup(ClientMsg client, List<String> pseudos, Map<String, Integer> pseudoToId, String groupName) {
        try {
            List<Integer> membres = new ArrayList<>();
            for (String pseudo : pseudos) {
                Integer id = pseudoToId.get(pseudo.toLowerCase());
                if (id == null) {
                    System.out.println("Pseudo inconnu : " + pseudo);
                    continue;
                }
                membres.add(id);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(1); // type = 1 : création de groupe
            dos.writeUTF(groupName); // ⚠️ Ajouter le nom du groupe ici
            dos.writeInt(membres.size());
            for (int id : membres)
                dos.writeInt(id);
            dos.flush();

            client.sendPacket(0, bos.toByteArray());

        } catch (IOException e) {
            System.out.println("Erreur lors de la création du groupe.");
            e.printStackTrace();
        }
    }

    public static void AddMemberToGroup(ClientMsg client, int groupId, String pseudo,
            Map<String, Integer> pseudoToId) {
        try {
            Integer userId = pseudoToId.get(pseudo.toLowerCase());
            if (userId == null) {
                System.out.println("Pseudo inconnu !");
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(2); // type = 2 : ajout membre
            dos.writeInt(groupId);
            dos.writeInt(userId);
            dos.flush();

            client.sendPacket(0, bos.toByteArray());

        } catch (IOException e) {
            System.out.println("Erreur lors de l'ajout du membre.");
            e.printStackTrace();
        }
    }

    public static void RemoveMemberFromGroup(ClientMsg client, int groupId, String pseudo,
            Map<String, Integer> pseudoToId) {
        try {
            Integer userId = pseudoToId.get(pseudo.toLowerCase());
            if (userId == null) {
                System.out.println("Pseudo inconnu !");
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(3); // type = 3 : suppression membre
            dos.writeInt(groupId);
            dos.writeInt(userId);
            dos.flush();

            client.sendPacket(0, bos.toByteArray());

        } catch (IOException e) {
            System.out.println("Erreur lors de la suppression du membre.");
            e.printStackTrace();
        }
    }

    public static void SendFile(ClientMsg client, String pseudo, String path,
            Map<String, Integer> pseudoToId) {
        try {
            Integer dest = pseudoToId.get(pseudo.toLowerCase());
            if (dest == null) {
                System.out.println("Pseudo inconnu !");
                return;
            }

            File file = new File(path);
            if (!file.exists()) {
                System.out.println("Fichier introuvable !");
                return;
            }

            client.sendFile(dest, file);
            System.out.println("Fichier envoyé !");
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi du fichier.");
        }
    }

    public static void SendImage(ClientMsg client, String pseudo, String path, Map<String, Integer> pseudoToId) {
        try {
            Integer dest = pseudoToId.get(pseudo.toLowerCase());
            if (dest == null) {
                System.out.println("Pseudo inconnu !");
                return;
            }

            File image = new File(path);
            if (!image.exists()) {
                System.out.println("Image introuvable !");
                return;
            }

            client.sendImage(dest, image);
            System.out.println("Image envoyée !");
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi de l'image.");
        }
    }

    public static Integer resolvePseudo(String pseudo, Map<String, Integer> pseudoToId) {
        return pseudoToId.get(pseudo.toLowerCase());
    }

}

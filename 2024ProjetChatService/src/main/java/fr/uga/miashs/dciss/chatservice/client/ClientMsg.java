/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.uga.miashs.dciss.chatservice.common.ImageTransfer;
import fr.uga.miashs.dciss.chatservice.common.Packet;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;

	private List<MessageListener> mListeners;
	private List<ConnectionListener> cListeners;

	/**
	 * Create a client with an existing id, that will connect to the server at the
	 * given address and port
	 * 
	 * @param id      The client id
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(int id, String address, int port) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
	}

	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 * 
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port);
	}

	/**
	 * Register a MessageListener to the client. It will be notified each time a
	 * message is received.
	 * 
	 * @param l
	 */
	public void addMessageListener(MessageListener l) {
		if (l != null)
			mListeners.add(l);
	}

	protected void notifyMessageListeners(Packet p) {
		mListeners.forEach(x -> x.messageReceived(p));
	}

	/**
	 * Register a ConnectionListener to the client. It will be notified if the
	 * connection start or ends.
	 * 
	 * @param l
	 */
	public void addConnectionListener(ConnectionListener l) {
		if (l != null)
			cListeners.add(l);
	}

	protected void notifyConnectionListeners(boolean active) {
		cListeners.forEach(x -> x.connectionEvent(active));
	}

	public int getIdentifier() {
		return identifier;
	}

	/**
	 * Method to be called to establish the connection.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void startSession() throws UnknownHostException {
		if (s == null || s.isClosed()) {
			try {
				s = new Socket(serverAddress, serverPort);
				dos = new DataOutputStream(s.getOutputStream());
				dis = new DataInputStream(s.getInputStream());
				dos.writeInt(identifier);
				dos.flush();
				if (identifier == 0) {
					identifier = dis.readInt();
					// Demander un pseudo à l'utilisateur
					System.out.print("Choisissez votre pseudo : ");
					Scanner sc = new Scanner(System.in);
					String nickname = sc.nextLine();

					// Envoyer le pseudo au serveur (type 6)
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos2 = new DataOutputStream(bos);
					byte[] nicknameBytes = nickname.getBytes();
					dos2.writeByte(7); // type 7
					dos2.write(nicknameBytes); // juste écrire les bytes du pseudo

					dos2.flush();

					// envoyer au serveur
					sendPacket(0, bos.toByteArray());
				}
				// start the receive loop
				new Thread(() -> receiveLoop()).start();
				notifyConnectionListeners(true);
			} catch (IOException e) {
				e.printStackTrace();
				// error, close session
				closeSession();
			}
		}
	}

	/**
	 * Send a packet to the specified destination (etiher a userId or groupId)
	 * 
	 * @param destId the destinatiion id
	 * @param data   the data to be sent
	 */
	public void sendPacket(int destId, byte[] data) {
		try {
			synchronized (dos) {
				dos.writeInt(destId);
				dos.writeInt(data.length);
				dos.write(data);
				dos.flush();
			}
		} catch (IOException e) {
			// error, connection closed
			closeSession();
		}

	}

	/**
	 * Envoie un fichier à un destinataire spécifique
	 * 
	 * @param destId ID du destinataire (utilisateur ou groupe)
	 * @param file   Fichier à envoyer
	 * @throws IOException Si une erreur survient pendant la lecture ou l'envoi du
	 *                     fichier
	 */
	public void sendFile(int destId, File file) throws IOException {
		if (!file.exists() || !file.isFile()) {
			throw new FileNotFoundException("Le fichier n'existe pas ou n'est pas accessible" + file.getAbsolutePath());
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		// Structure du message:
		// [TYPE=5][NOM_FICHIER][TAILLE_FICHIER][CONTENU_FICHIER]
		dos.writeByte(5); // Type 5 pour transfert de fichier
		dos.writeUTF(file.getName()); // Nom du fichier

		// Lecture et envoi du contenu du fichier
		byte[] fileContent = new byte[(int) file.length()];
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(fileContent);
		}

		dos.writeLong(fileContent.length); // Taille du fichier
		dos.write(fileContent); // Contenu du fichier
		dos.flush();

		// Envoi du paquet
		sendPacket(destId, bos.toByteArray());
	}

	// transfer de l'image
	public void sendImage(int destId, File imageFile) throws IOException {
		if (!imageFile.exists() || !imageFile.isFile()) {
			throw new FileNotFoundException("L'image n'existe pas ou n'est pas accessible");
		}

		// Vérifier que c'est bien une image
		String name = imageFile.getName().toLowerCase();
		if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
			throw new IllegalArgumentException("Format d'image non supporté");
		}

		// Convertir l'image en ImageTransfer
		ImageTransfer imageTransfer = ImageTransfer.fromImage(imageFile);

		// Sérialiser et envoyer
		byte[] data = ImageTransfer.toBytes(imageTransfer);
		sendPacket(destId, data);
	}

	/**
	 * Start the receive loop. Has to be called only once.
	 */
	private void receiveLoop() {
		try {
			while (s != null && !s.isClosed()) {

				int sender = dis.readInt();
				int dest = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				dis.readFully(data);
				notifyMessageListeners(new Packet(sender, dest, data));

			}
		} catch (IOException e) {
			// error, connection closed
		}
		closeSession();
	}

	public void closeSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
		}
		s = null;
		notifyConnectionListeners(false);
	}

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		ClientMsg c = new ClientMsg("localhost", 1666);
		Map<String, Integer> pseudoToId = new HashMap<>();
		Map<Integer, String> idToPseudo = new HashMap<>();

		// Listener principal de messages texte
		c.addMessageListener(p -> {
			String msg = new String(p.data).trim();

			// Pseudo annoncé
			if (msg.startsWith("NOUVEAU_PSEUDO:")) {
				String[] parts = msg.split(":", 3);
				if (parts.length == 3) {
					int userId = Integer.parseInt(parts[1]);
					String pseudo = parts[2];

					// Si déjà enregistré, ne rien faire
					if (pseudoToId.containsKey(pseudo.toLowerCase()))
						return;

					// Enregistrer le pseudo et l'ID dans les maps
					pseudoToId.put(pseudo.toLowerCase(), userId);
					idToPseudo.put(userId, pseudo);

					// Afficher le message de connexion d'un nouveau utilisateur
					if (userId != c.getIdentifier()) {
						System.out.println(pseudo + " vient de se connecter !");
					}
				}
				return;
			}

			// Affichage du message "Bienvenue ..."
			if (msg.toLowerCase().startsWith("bienvenue ") && p.srcId == 0) {
				System.out.println(msg);

				// On enregistre son propre pseudo
				String myPseudo = msg.substring(10).split(" ")[0];
				pseudoToId.put(myPseudo.toLowerCase(), c.getIdentifier());
				idToPseudo.put(c.getIdentifier(), myPseudo);

				return;
			}

			// Si Message serveur
			if (p.srcId == 0) {
				System.out.println(msg);
				return;
			}

			// Affichage Message classique
			String from = idToPseudo.getOrDefault(p.srcId, "Utilisateur #" + p.srcId);
			String to = idToPseudo.getOrDefault(p.destId, "Utilisateur #" + p.destId);
			System.out.println(from + " says to " + to + " : " + msg);
		});

		// Listeners pour fichiers et images
		c.addMessageListener(new FileMessageListener("downloads"));
		c.addMessageListener(new ImageMessageListener("images"));

		// Listener de déconnexion
		c.addConnectionListener(active -> {
			if (!active)
				System.exit(0);
		});

		// Connexion
		c.startSession();
		Thread.sleep(500); // Laisser passer l'annonce de bienvenue

		// Interface console
		Scanner sc = new Scanner(System.in);
		String lu = null;

		while (!"\\quit".equalsIgnoreCase(lu)) {
			System.out
					.println("A qui voulez vous écrire ? ou tapez \\add, \\remove, \\create, \\file, \\photo, \\quit");
			lu = sc.nextLine();

			switch (lu) {
				case "\\file":
					try {
						System.out.print("Pseudo du destinataire : ");
						String pseudo = sc.nextLine();
						System.out.print("Chemin du fichier à envoyer : ");
						String path = sc.nextLine();
						ControleChat.SendFile(c, pseudo, path, pseudoToId);
					} catch (Exception e) {
						System.out.println("Erreur lors de l'envoi du fichier.");
					}
					break;

				case "\\photo":
					try {
						System.out.print("Pseudo du destinataire : ");
						String pseudo = sc.nextLine();
						System.out.print("Chemin de l'image à envoyer : ");
						String path = sc.nextLine();
						ControleChat.SendImage(c, pseudo, path, pseudoToId);
					} catch (Exception e) {
						System.out.println("Erreur lors de l'envoi de l'image.");
					}
					break;

				case "\\create":
					try {
						System.out.print("Nombre de membres dans le groupe : ");
						int nb = Integer.parseInt(sc.nextLine());
						List<String> pseudos = new ArrayList<>();
						for (int i = 0; i < nb; i++) {
							System.out.print("Pseudo du membre " + (i + 1) + " : ");
							pseudos.add(sc.nextLine());
						}
						ControleChat.CreateGroup(c, pseudos, pseudoToId);
					} catch (Exception e) {
						System.out.println("Erreur lors de la création du groupe.");
					}
					break;

				case "\\add":
					try {
						System.out.print("ID du groupe à modifier : ");
						int groupId = Integer.parseInt(sc.nextLine());
						System.out.print("Pseudo à ajouter : ");
						String pseudo = sc.nextLine();
						ControleChat.AddMemberToGroup(c, groupId, pseudo, pseudoToId);
					} catch (Exception e) {
						System.out.println("Erreur lors de l'ajout.");
					}
					break;

				case "\\remove":
					try {
						System.out.print("ID du groupe à modifier : ");
						int groupId = Integer.parseInt(sc.nextLine());
						System.out.print("Pseudo à retirer : ");
						String pseudo = sc.nextLine();
						ControleChat.RemoveMemberFromGroup(c, groupId, pseudo, pseudoToId);
					} catch (Exception e) {
						System.out.println("Erreur lors de la suppression.");
					}
					break;

				default:
					// Message texte
					if (!lu.startsWith("\\")) {
						System.out.println("Votre message ? ");
						String message = sc.nextLine();
						ControleChat.SendMessage(c, lu, message, pseudoToId);
					}
					break;
			}
		}

		c.closeSession();
	}

}

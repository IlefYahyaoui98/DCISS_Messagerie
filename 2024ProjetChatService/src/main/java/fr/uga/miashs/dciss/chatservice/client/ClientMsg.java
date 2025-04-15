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
					dos2.writeByte(7); // type 7 : définir le pseudo
					dos2.writeUTF(nickname);
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
			throw new FileNotFoundException("Le fichier n'existe pas ou n'est pas accessible");
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

		// add a dummy listener that print the content of message as a string
		c.addMessageListener(p -> System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data)));

		// ajout d'un écouteur de fichier qui enregistre les fichiers reçus dans le
		c.addMessageListener(new FileMessageListener("downloads"));

		 // Ajouter l'écouteur d'images
		 c.addMessageListener(new ImageMessageListener("images"));

		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active -> {
			if (!active)
				System.exit(0);
		});

		c.startSession();
		//System.out.println("Vous êtes : " + c.getIdentifier());

		// Thread.sleep(5000);
		try {
			Thread.sleep(150); // attendre un petit moment pour laisser passer le message de bienvenue
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// l'utilisateur avec id 4 crée un grp avec 1 et 3 dedans (et lui meme)
		if (c.getIdentifier() == 4) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			// byte 1 : create group on server
			dos.writeByte(1);

			// nb members
			dos.writeInt(2);
			// list members
			dos.writeInt(1);
			dos.writeInt(3);
			dos.flush();

			c.sendPacket(0, bos.toByteArray());

		}

		Scanner sc = new Scanner(System.in);
		String lu = null;
		while (!"\\quit".equals(lu)) {
			System.out.println(
					"A qui voulez vous écrire ? ou tapez \\add pour intégrer un groupe, \\remove pour supprimer un membre, \\create pour créer un groupe");
			lu = sc.nextLine();
			// Si l'utilisateur tape la commande \file, on traite l'envoi de fichier
			if ("\\file".equals(lu)) {
				try {
					System.out.print("ID du destinataire: ");
					int dest = Integer.parseInt(sc.nextLine());

					System.out.print("Chemin du fichier à envoyer: ");
					String path = sc.nextLine();
					File file = new File(path);

					if (file.exists() && file.isFile()) {
						c.sendFile(dest, file);
						System.out.println("Fichier envoyé!");
					} else {
						System.out.println("Fichier introuvable");
					}
				} catch (Exception e) {
					System.out.println("Erreur lors de l'envoi: " + e.getMessage());
				}
				continue;
			}

			 // Ajouter le traitement de la commande \photo
			 if ("\\photo".equals(lu)) {
				try {
					System.out.print("ID du destinataire: ");
					int dest = Integer.parseInt(sc.nextLine());
					
					System.out.print("Chemin de l'image à envoyer: ");
					String path = sc.nextLine();
					File imageFile = new File(path);
					
					c.sendImage(dest, imageFile);
					System.out.println("Image envoyée!");
				} catch (Exception e) {
					System.out.println("Erreur lors de l'envoi: " + e.getMessage());
				}
				continue;
			}
					
			// Créer un groupe
			if ("\\create".equals(lu)) {
				try {
					System.out.print("Veuillez entrer le nombre des membres dans le groupe ? ");
					int nb = Integer.parseInt(sc.nextLine());

					List<Integer> membres = new ArrayList<>();
					for (int i = 0; i < nb; i++) {
						System.out.print("ID du membre " + (i + 1) + " : ");
						membres.add(Integer.parseInt(sc.nextLine()));
					}

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);
					dos.writeByte(1); // type 1 = création de groupe
					dos.writeInt(membres.size());
					for (int id : membres)
						dos.writeInt(id);
					dos.flush();

					c.sendPacket(0, bos.toByteArray());

				} catch (Exception e) {
					System.out.println("Erreur dans la saisie");
				}
				continue;
			}

			// Si l'utilisateur tape la commande \add, on traite l'ajout de membre
			if ("\\add".equals(lu)) {
				try {
					// Demande l'ID du groupe cible
					System.out.print("ID du groupe que vous voulez intégrer : ");
					int groupId = Integer.parseInt(sc.nextLine());
					// Demande l'ID du nouvel utilisateur à ajouter au groupe
					System.out.print("Entrer l'ID du nouvel utilisateur : ");
					int newUserId = Integer.parseInt(sc.nextLine());
					// Construction d'un paquet de données à envoyer au serveur
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);
					dos.writeByte(2); // type = 2 : commande "ajout membre"
					dos.writeInt(groupId); // ID du groupe cible
					dos.writeInt(newUserId); // ID de l'utilisateur à ajouter
					dos.flush();

					// Envoi du paquet vers le serveur (destinataire = 0)
					c.sendPacket(0, bos.toByteArray());

				} catch (Exception e) {
					// Gestion d'une erreur de saisie (ex: non entier)
					System.out.println("Erreur de saisie.");
				}

				// Retour au début de la boucle (évite de demander à qui écrire après)
				continue;
			}
			if ("\\remove".equals(lu)) {
				try {
					// Demande à l'utilisateur l'ID du groupe
					System.out.print("Veuillez entrer l'ID du groupe :");
					int groupId = Integer.parseInt(sc.nextLine());

					// Demande l'ID de l'utilisateur à retirer
					System.out.print("Veuillez entrer l'ID de l'utilisateur à supprimer du groupe : ");
					int memberId = Integer.parseInt(sc.nextLine());

					// Construction du paquet à envoyer au serveur
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(bos);
					dos.writeByte(3); // type = 3 => suppression d’un membre
					dos.writeInt(groupId); // ID du groupe
					dos.writeInt(memberId); // ID de l'utilisateur à retirer
					dos.flush();

					// Envoi du paquet vers le serveur (destinataire = 0)
					c.sendPacket(0, bos.toByteArray());
				} catch (Exception e) {
					System.out.println("Erreur de saisie.");
				}

				// On revient au début de la boucle
				continue;
			}

			// Si ce n'est pas une commande spéciale, on traite comme un envoi de message
			// normal
			try {
				// Demande le destinataire du message (ID utilisateur ou groupe)
				int dest = Integer.parseInt(lu);

				// Demande le message à envoyer
				System.out.println("Votre message ? ");
				lu = sc.nextLine();

				// Envoie du message sous forme de bytes
				c.sendPacket(dest, lu.getBytes());

			} catch (InputMismatchException | NumberFormatException e) {
				// Gestion d'une erreur si l'entrée n'est pas un nombre
				System.out.println("Mauvais format");
			}
			
		}

		/*
		 * int id =1+(c.getIdentifier()-1) % 2; System.out.println("send to "+id);
		 * c.sendPacket(id, "bonjour".getBytes());
		 * 
		 * 
		 * Thread.sleep(10000);
		 */

		c.closeSession();

	}

}

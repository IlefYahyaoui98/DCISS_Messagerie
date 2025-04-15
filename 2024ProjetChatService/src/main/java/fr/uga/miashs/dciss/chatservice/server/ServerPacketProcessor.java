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

package fr.uga.miashs.dciss.chatservice.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.FileTransfer;
import fr.uga.miashs.dciss.chatservice.common.ImageTransfer;
import fr.uga.miashs.dciss.chatservice.common.Packet;

public class ServerPacketProcessor implements PacketProcessor {
	private final static Logger LOG = Logger.getLogger(ServerPacketProcessor.class.getName());
	private ServerMsg server;

	public ServerPacketProcessor(ServerMsg s) {
		this.server = s;
	}

	@Override
	public void process(Packet p) {
		// ByteBufferVersion. On aurait pu utiliser un ByteArrayInputStream +
		// DataInputStream à la place
		ByteBuffer buf = ByteBuffer.wrap(p.data);
		byte type = buf.get();

		if (type == 1) { // cas creation de groupe
			createGroup(p.srcId, buf);

		} else if (type == 2) { // cas ajout d'un utilisateur
			addMemberToGroup(p.srcId, buf);

		} else if (type == 3) {
			removeMemberFromGroup(p.srcId, buf);
		} else if (type == 5) {
			handleFileTransfer(p); // Traite le transfert de fichier
		} else if (type == 6) {
			handleImageTransfer(p); // Traite le transfert d'image
		} else {
			LOG.warning("Server message of type=" + type + " not handled by procesor");
		}
	}

	public void createGroup(int ownerId, ByteBuffer data) {
		int nb = data.getInt();
		GroupMsg g = server.createGroup(ownerId);

		for (int i = 0; i < nb; i++) {
			int memberId = data.getInt();
			UserMsg member = server.getUser(memberId);
			g.addMember(member);

			// ✅ Envoi d'un message de notification au membre
			if (member != null) {
				String notif = "Vous avez été ajouté au groupe " + g.getId();
				member.process(new Packet(0, memberId, notif.getBytes()));
			}
		}

		// ✅ Envoi d'une confirmation au créateur du groupe
		UserMsg owner = server.getUser(ownerId);
		if (owner != null) {
			String confirm = "Vous avez créé le groupe " + g.getId();
			owner.process(new Packet(0, ownerId, confirm.getBytes()));
		}
	}

	public void addMemberToGroup(int requesterId, ByteBuffer data) {
		int groupId = data.getInt();
		int newMemberId = data.getInt();

		GroupMsg group = server.getGroup(groupId);
		if (group == null) {
			LOG.warning("Groupe introuvable : " + groupId);
			return;
		}

		if (group.getOwner().getId() != requesterId) {
			LOG.warning("Seul le propriétaire peut ajouter des membres !");
			return;
		}

		UserMsg newMember = server.getUser(newMemberId);
		if (newMember == null) {
			LOG.warning("Utilisateur " + newMemberId + " inconnu");
			return;
		}

		// Ajoute un utilisateur au groupe
		group.addMember(newMember);
		LOG.info("Utilisateur " + newMemberId + " ajouté au groupe " + groupId);

		// ✅ Envoi d'un message de confirmation au membre ajouté
		String messageToMember = "Vous avez été ajouté au groupe " + groupId;
		newMember.process(new Packet(0, newMemberId, messageToMember.getBytes()));

		// ✅ Envoi d'un message de confirmation au propriétaire
		UserMsg owner = server.getUser(requesterId);
		if (owner != null) {
			String messageToOwner = "L'utilisateur " + newMemberId + " a été ajouté au groupe " + groupId;
			owner.process(new Packet(0, requesterId, messageToOwner.getBytes()));
		}
	}

	// Retire un utilisateur du groupe
	// On suppose que le propriétaire du groupe est celui qui a envoyé la requête
	public void removeMemberFromGroup(int requesterId, ByteBuffer data) {
		int groupId = data.getInt();
		int memberId = data.getInt();

		GroupMsg group = server.getGroup(groupId);
		if (group == null || group.getOwner().getId() != requesterId)
			return;

		UserMsg member = server.getUser(memberId);
		if (member == null)
			return;

		if (group.removeMember(member)) {
			// 🔔 Notifie l'utilisateur retiré
			String msg = "Vous avez été retiré du groupe " + groupId;
			member.process(new Packet(0, memberId, msg.getBytes()));

			// 🔔 Confirme au propriétaire
			String confirm = "L'utilisateur " + memberId + " a été retiré du groupe " + groupId;
			server.getUser(requesterId).process(new Packet(0, requesterId, confirm.getBytes()));
		}
	}

	// Traite un transfert de fichier
	private void handleFileTransfer(Packet p) {
		try {
			// Reconstruction du FileTransfer
			FileTransfer transfer = FileTransfer.fromBytes(p.data);

			// Log du transfert
			LOG.info("File transfer from " + p.srcId + " to " + p.destId +
					": " + transfer.getFileName());

			// Transmission au destinataire
			if (p.destId < 0) { // Groupe
				GroupMsg group = server.getGroup(p.destId);
				if (group != null) {
					group.process(p);
				}
			} else { // Utilisateur direct
				UserMsg dest = server.getUser(p.destId);
				if (dest != null) {
					dest.process(p);
				}
			}
		} catch (IOException e) {
			LOG.warning("Error processing file transfer: " + e.getMessage());
		}
	}

	// Traite un transfert d'image
private void handleImageTransfer(Packet p) {
    try {
        // Reconstruction de l'ImageTransfer
        ImageTransfer transfer = ImageTransfer.fromBytes(p.data);

        // Log du transfert
        LOG.info("Image transfer from " + p.srcId + " to " + p.destId +
                ": " + transfer.getImageName() + " (" + transfer.getImageFormat() + ")");

        // Transmission au destinataire
        if (p.destId < 0) { // Groupe
            GroupMsg group = server.getGroup(p.destId);
            if (group != null) {
                group.process(p);
            } else {
                LOG.warning("Group " + p.destId + " not found for image transfer");
            }
        } else { // Utilisateur direct
            UserMsg dest = server.getUser(p.destId);
            if (dest != null) {
                dest.process(p);
            } else {
                LOG.warning("User " + p.destId + " not found for image transfer");
            }
        }


    } catch (IOException e) {
        LOG.warning("Error processing image transfer: " + e.getMessage());
       
    }
}

}

package fr.uga.miashs.dciss.chatservice.server;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

public class ServerMsgTest {

    private ServerMsg server;
    private static final int TEST_PORT = 1667;

    @Before
    public void setUp() throws Exception {
        server = new ServerMsg(TEST_PORT);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testCreateGroup() throws Exception {
        // Préparer les données
        int userId = 1;
        UserMsg owner = new UserMsg(userId, server);
        server.addUser(owner); // Utiliser la nouvelle méthode

        // Exécuter le test
        GroupMsg group = server.createGroup(userId);

        // Vérifier les résultats
        assertNotNull("Le groupe créé ne devrait pas être null", group);
        assertTrue("L'ID du groupe devrait être négatif", group.getId() < 0);
        assertSame("Le propriétaire du groupe devrait être l'utilisateur créé", owner, group.getOwner());
        assertSame("Le groupe devrait être stocké dans le serveur", group, server.getGroup(group.getId()));
    }

    @Test(expected = ServerException.class)
    public void testCreateGroupWithInvalidOwner() {
        // Tenter de créer un groupe avec un ID utilisateur invalide
        server.createGroup(999);
    }

    @Test

    public void testRemoveGroup() {
        // Configuration initiale
        int userId = 1;
        UserMsg owner = new UserMsg(userId, server);
        server.addUser(owner);

        // Créer un groupe à supprimer
        GroupMsg group = server.createGroup(userId);
        int groupId = group.getId();

        // Vérifier que le groupe existe initialement
        assertNotNull("Le groupe devrait exister avant la suppression",
                server.getGroup(groupId));

        // Test 1: Suppression d'un groupe existant
        boolean result = server.removeGroup(groupId);
        assertTrue("La suppression devrait réussir", result);
        assertNull("Le groupe ne devrait plus exister après suppression",
                server.getGroup(groupId));

        // Test 2: Suppression d'un groupe déjà supprimé
        boolean secondResult = server.removeGroup(groupId);
        assertFalse("La suppression d'un groupe déjà supprimé devrait échouer",
                secondResult);

        // Test 3: Suppression d'un groupe inexistant
        boolean nonExistentResult = server.removeGroup(-999);
        assertFalse("La suppression d'un groupe inexistant devrait échouer",
                nonExistentResult);
    }

    @Test

    public void testGetGroup() {
        // Configuration initiale
        int userId = 1;
        UserMsg owner = new UserMsg(userId, server);
        server.addUser(owner);

        // Test 1: Récupérer un groupe qui n'existe pas
        GroupMsg nonExistentGroup = server.getGroup(-999);
        assertNull("Un groupe inexistant devrait retourner null", nonExistentGroup);

        // Test 2: Créer et récupérer un groupe
        GroupMsg createdGroup = server.createGroup(userId);
        int groupId = createdGroup.getId();

        GroupMsg retrievedGroup = server.getGroup(groupId);
        assertNotNull("Le groupe existant devrait être trouvé", retrievedGroup);
        assertEquals("L'ID du groupe récupéré devrait correspondre",
                groupId, retrievedGroup.getId());
        assertSame("Le groupe récupéré devrait être le même objet que celui créé",
                createdGroup, retrievedGroup);
    }
}
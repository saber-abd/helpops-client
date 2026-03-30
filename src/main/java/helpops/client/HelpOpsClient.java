package helpops.client;

import helpops.interfaces.RMIAuthService;
import helpops.interfaces.RMIHelpOps;
import helpops.model.Incident;
import helpops.model.Statistiques;
import helpops.model.Token;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class HelpOpsClient {
    private RMIAuthService auth;
    private RMIHelpOps service;
    private Scanner scanner;
    private Token token;// token de session obtenu apres connexion

    public HelpOpsClient(String authHost, String serverHost) {
        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("console.encoding", "UTF-8");

            Registry authRegistry = LocateRegistry.getRegistry(authHost, 1099); // connexion serveur Auth
            auth = (RMIAuthService) authRegistry.lookup("AuthService");
            System.out.println("[CLIENT] Serveur Auth : " + auth.ping());

            Registry serverRegistry = LocateRegistry.getRegistry(serverHost, 1099); // connexion serveur de gestion des incident
            service = (RMIHelpOps) serverRegistry.lookup("HelpOps");
            System.out.println("[CLIENT] Serveur HelpOps : " + service.ping());
            System.out.println();

            scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[ERREUR] Impossible de se connecter aux serveurs.");
            System.err.println("Verifiez que les serveurs Auth et HelpOps sont demarres.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void demarrer() {
        System.out.println("=== HELP'OPS ===");
        while (true) {
            while (token == null) {
                System.out.println("\n1. Se connecter");
                System.out.println("2. Creer un compte");
                System.out.println("3. Quitter");
                System.out.print("Choix : ");
                String choix = scanner.nextLine().trim();
                if ("1".equals(choix)) {
                    seConnecter();
                } else if ("2".equals(choix)) {
                    creerCompte();
                } else if ("3".equals(choix)) {
                    System.out.println("Fermeture.");
                    return;
                } else {
                    System.out.println("Choix invalide.");
                }
            }
            menuPrincipal();
            System.out.println("Retour au menu de connexion...");
        }
    }

    //  authentification
    private boolean seConnecter() {
        System.out.println("=== CONNEXION ===");
        int maxTentatives = 3;
        for (int i = 1; i <= maxTentatives; i++) {
            try {
                System.out.print("Login : ");
                String login = scanner.nextLine().trim();
                String mdp = lireMotDePasse();
                token = auth.connecter(login, hacher(mdp));
                if (token != null) {
                    System.out.println("Bienvenue " + token.getLogin() + " !");
                    return true;
                } else {
                    int restants = maxTentatives - i;
                    if (restants > 0) {
                        System.out.println("Identifiants incorrects. Il vous reste " + restants + " tentative(s).");
                    } else {
                        System.out.println("Trop d'echecs. Retour au menu.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERREUR] " + e.getMessage());
            }
        }
        return false;
    }

    private boolean creerCompte() {
        System.out.println("=== CREATION DE COMPTE ===");
        System.out.println("[TEST UNIQUEMENT] Choisissez le role du compte a creer.");
        System.out.println("  Cette option sera retiree en production.");
        System.out.println("  1. UTILISATEUR (defaut)");
        System.out.println("  2. AGENT");
        System.out.print("Role (Entree = UTILISATEUR) : ");
        String choixRole = scanner.nextLine().trim();
        String role = "2".equals(choixRole) ? "AGENT" : "UTILISATEUR";
        try {
            System.out.print("Login : ");
            String login = scanner.nextLine().trim();
            String mdp = lireMotDePasse();
            boolean ok = auth.inscrireAvecRole(login, hacher(mdp), role);
            if (ok) {
                System.out.println("Compte " + role + " cree ! Connexion automatique...\n");
                token = auth.connecter(login, hacher(mdp));
                return token != null;
            } else {
                System.out.println("Ce login est deja utilise. Choisissez-en un autre.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
            return false;
        }
    }

    //  Menus
    private void menuPrincipal() {
        while (token != null) {
            if (token.estAgent()) {
                menuAgent();
            } else {
                menuUtilisateur();
            }
        }
    }

    private void menuUtilisateur() {
        System.out.println("=== MENU UTILISATEUR ===");
        System.out.println("1. Signaler un incident");
        System.out.println("2. Mes incidents");
        System.out.println("3. Detail d'un incident");
        System.out.println("4. Se deconnecter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();
        try {
            switch (choix) {
                case "1" -> signalerIncident();
                case "2" -> voirMesIncidents();
                case "3" -> voirDetail();
                case "4" -> { token = null; return; }
                default  -> System.out.println("Choix invalide.");
            }
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
        }
    }

    private void menuAgent() {
        System.out.println("=== MENU AGENT ===");
        System.out.println("1. Prendre en charge un incident");
        System.out.println("2. Tous les incidents");
        System.out.println("3. Voir mes assignations");
        System.out.println("4. Resoudre un ticket");
        System.out.println("5. Creer un ticket pour un utilisateur");
        System.out.println("6. Statistiques");
        System.out.println("7. Detail d'un incident");
        System.out.println("8. Se deconnecter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();
        try {
            switch (choix) {
                case "1" -> voirEtPrendreEnCharge();
                case "2" -> voirTousLesIncidents();
                case "3" -> voirMesAssignations();
                case "4" -> resoudreTicket();
                case "5" -> creerTicketPourUser();
                case "6" -> voirStatistiques();
                case "7" -> voirDetail();
                case "8" -> { token = null; return; }
                default  -> System.out.println("Choix invalide.");
            }
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
        }
    }

    //  actions UTILISATEUR
    private void signalerIncident() throws Exception {
        System.out.println("--- Nouveau signalement ---");
        System.out.print("Categorie : ");
        String cat = scanner.nextLine().trim();
        System.out.print("Titre : ");
        String titre = scanner.nextLine().trim();
        System.out.print("Description : ");
        String desc = scanner.nextLine().trim();
        if (cat.isEmpty() || titre.isEmpty()) {
            System.out.println("Erreur : Le titre et la categorie sont obligatoires.");
            return;
        }
        Incident i = service.signalerIncident(token.getValeur(), cat, titre, desc);
        if (i != null) {
            System.out.println("[SUCCES] Incident cree avec l'ID #" + i.getId());
        } else {
            System.out.println("[ERREUR] Echec de la creation.");
        }
    }

    private void voirMesIncidents() throws Exception {
        System.out.println("--- Mes incidents signales ---");
        List<Incident> liste = service.listerMesIncidents(token.getValeur());
        if (liste == null || liste.isEmpty()) {
            System.out.println("Vous n'avez aucun incident enregistre.");
        } else {
            for (Incident i : liste) System.out.println(i);
        }
    }

    private void voirDetail() throws Exception {
        System.out.print("ID de l'incident : ");
        String idStr = scanner.nextLine().trim();
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { System.out.println("ID invalide."); return; }
        Incident i = service.consulterIncident(token.getValeur(), id); // via le token et l'id de l'incident
        if (i == null) { System.out.println("Incident #" + id + " introuvable ou acces refuse."); return; }
        System.out.println("--- Detail incident #" + id + " ---");
        System.out.println("Titre       : " + i.getTitre());
        System.out.println("Categorie   : " + i.getCategorie());
        System.out.println("Description : " + i.getDescription());
        System.out.println("Statut      : " + i.getStatut());
        System.out.println("Auteur      : " + i.getUserUuid());
        System.out.println("Cree le     : " + i.getDateCreation());
        if (i.getAgentUuid() != null) {
            System.out.println("Agent       : " + i.getAgentUuid());
            System.out.println("Assigne le  : " + i.getDateAssignation());
        }
        if (i.getDateResolution() != null) {
            System.out.println("Resolu le   : " + i.getDateResolution());
            System.out.println("Resolution  : " + i.getMessageResolution());
        }
    }

    //  actions AGENT
    private void voirEtPrendreEnCharge() throws Exception {
        System.out.println("--- Incidents en attente ---");
        List<Incident> ouverts = service.listerIncidentsOuverts(token.getValeur());
        if (ouverts == null || ouverts.isEmpty()) {
            System.out.println("Aucun incident ouvert.");
            return;
        }
        for (Incident i : ouverts) System.out.println(i);
        System.out.print("\nID de l'incident a prendre en charge (ou Entree pour annuler) : ");
        String idStr = scanner.nextLine().trim();
        if (idStr.isEmpty()) return;
        int id = Integer.parseInt(idStr);
        boolean ok = service.prendreEnChargeIncident(token.getValeur(), id);
        if (ok) System.out.println("[SUCCES] Vous avez pris en charge l'incident #" + id);
    }

    private void voirTousLesIncidents() throws Exception {
        System.out.println("--- Liste de tous les incidents ---");
        List<Incident> tous = service.listerTousLesIncidents(token.getValeur());
        if (tous == null || tous.isEmpty()) {
            System.out.println("Aucun incident dans le systeme.");
        } else {
            for (Incident i : tous) System.out.println(i);
        }
    }

    private void voirMesAssignations() throws Exception {
        System.out.println("--- Mes assignations ---");
        List<Incident> liste = service.listerMesAssignations(token.getValeur());
        if (liste == null || liste.isEmpty()) {
            System.out.println("Vous n'avez aucun incident assigne.");
        } else {
            for (Incident i : liste) System.out.println(i);
        }
    }

    // v3 resolution ticket assigne a l'agent connecte
    private void resoudreTicket() throws Exception {
        System.out.println("--- Resoudre un ticket ---");
        List<Incident> assignations = service.listerMesAssignations(token.getValeur());
        List<Incident> aResoudre = assignations == null ? List.of() :
                assignations.stream()
                        .filter(i -> "ASSIGNED".equalsIgnoreCase(i.getStatut()))
                        .toList();
        if (aResoudre.isEmpty()) {
            System.out.println("Aucun ticket ASSIGNED a resoudre.");
            return;
        }
        for (Incident i : aResoudre) System.out.println(i);
        System.out.print("\nID du ticket a resoudre (ou Entree pour annuler) : ");
        String idStr = scanner.nextLine().trim();
        if (idStr.isEmpty()) return;
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { System.out.println("ID invalide."); return; }
        System.out.print("Message de resolution : ");
        String message = scanner.nextLine().trim();
        if (message.isEmpty()) {
            System.out.println("Le message de resolution est obligatoire.");
            return;
        }
        boolean ok = service.resoudreTicket(token.getValeur(), id, message);
        if (ok) System.out.println("[SUCCES] Ticket #" + id + " marque comme RESOLVED.");
    }

    // v3 agent cree un ticket pour un utilisateur
    private void creerTicketPourUser() throws Exception {
        System.out.println("--- Creer un ticket pour un utilisateur ---");
        System.out.print("Login de l'utilisateur cible : ");
        String loginCible = scanner.nextLine().trim();
        System.out.print("Categorie : ");
        String cat = scanner.nextLine().trim();
        System.out.print("Titre : ");
        String titre = scanner.nextLine().trim();
        System.out.print("Description : ");
        String desc = scanner.nextLine().trim();
        if (cat.isEmpty() || titre.isEmpty()) {
            System.out.println("Erreur : Le titre et la categorie sont obligatoires.");
            return;
        }
        Incident i = service.creerTicketPourUtilisateur(token.getValeur(), loginCible, cat, titre, desc);
        if (i != null) {
            System.out.println("[SUCCES] Ticket #" + i.getId() + " cree pour l'utilisateur '" + loginCible + "'.");
        } else {
            System.out.println("[ERREUR] Echec de la creation.");
        }
    }

    // v3 affichage statistiques
    private void voirStatistiques() throws Exception {
        System.out.println("--- Statistiques ---");
        Statistiques stats = service.getStatistiques(token.getValeur());
        System.out.println(stats);
    }

    //  utilitaires
    private String hacher(String motDePasse) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage", e);
        }
    }

    private String lireMotDePasse() {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("Mot de passe : ");
            return new String(passwordChars);
        } else {
            System.out.print("Mot de passe : ");
            return scanner.nextLine().trim();
        }
    }

    public static void main(String[] args) {
        String authHost   = (args.length > 0) ? args[0] : "localhost";
        String serverHost = (args.length > 1) ? args[1] : "localhost";
        new HelpOpsClient(authHost, serverHost).demarrer();
    }
}

package helpops.client;

import helpops.interfaces.RMIAuthService;
import helpops.interfaces.RMIHelpOps;
import helpops.model.Incident;
import helpops.model.Token;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import static helpops.client.AgentAction.*;
import static helpops.utils.ConsoleUtils.hacher;
import static helpops.utils.ConsoleUtils.lireMotDePasse;

public class HelpOpsClient {
    private RMIAuthService auth;
    private RMIHelpOps service;
    private Scanner scanner;
    private Token token;// token de session obtenu apres connexion
    private boolean supervisionActive = false;
    private SupervisionUDP threadUDP;

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
                String mdp = lireMotDePasse(scanner);
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
            String mdp = lireMotDePasse(scanner);
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
        System.out.println("8. Supervision ");
        System.out.println("9. Deleguer un ticket");
        System.out.println("10. Se deconnecter");

        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();
        try {
            switch (choix) {
                case "1" -> voirEtPrendreEnCharge(service,token,scanner);
                case "2" -> voirTousLesIncidents(service,token);
                case "3" -> voirMesAssignations(service,token);
                case "4" -> resoudreTicket(service,token,scanner);
                case "5" -> creerTicketPourUser(service,token,scanner);
                case "6" -> voirStatistiques(service,token);
                case "7" -> voirDetail();
                case "10" -> {if (supervisionActive) supervision();token = null;}
                case "9" -> deleguerTicket(service,token,scanner);
                case "8" -> supervision();
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

    //  actions AGENT=> focntion deporter dans AgentAction

    private void supervision() {
        if (!supervisionActive) {
            threadUDP = new SupervisionUDP(token);
            threadUDP.start();
            supervisionActive = true;
            System.out.println("[INFO] Supervision UDP activee.");
        } else {
            if (threadUDP != null) threadUDP.arreter();
            supervisionActive = false;
            System.out.println("[INFO] Supervision désactivee.");
        }
    }

    public static void main(String[] args) {
        String authHost   = (args.length > 0) ? args[0] : "localhost";
        String serverHost = (args.length > 1) ? args[1] : "localhost";
        new HelpOpsClient(authHost, serverHost).demarrer();
    }
}
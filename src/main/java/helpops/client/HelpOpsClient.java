package helpops.client;

import helpops.interfaces.RMIAuthService;
import helpops.interfaces.RMIHelpOps;
import helpops.model.Incident;
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
    private Token token;    // token de session obtenu apres connexion

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
        System.out.println("=== HELP'OPS  ===");

        while (true) {
            // Phase 1 : Connexion / Inscription
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
                    return; // Sortie définitive du programme
                } else {
                    System.out.println("Choix invalide.");
                }
            }

            // Phase 2 :Lancemwent du menu
            menuPrincipal();

            //Phase 3: deconnection
            System.out.println("Retour au menu de connexion...");
        }
    }

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
                        System.out.println("Compte bloqué temporairement ou trop d'échecs.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERREUR] " + e.getMessage());
            }
        }
        return false; // Échec après 3 tentatives
    }

    private boolean creerCompte() {
        System.out.println("=== CREATION DE COMPTE ===");
        try {
            System.out.print("Login : ");
            String login = scanner.nextLine().trim();
            String mdp   = lireMotDePasse();
            boolean ok = auth.inscrire(login, hacher(mdp));
            if (ok) {
                System.out.println("Compte cree avec succes ! Connexion automatique...\n");
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

    private void menuPrincipal() {
        while (token != null) {
            if (token.estAgent()) {
                menuAgent();
            } else {
                menuUtilisateur();
            }
        }
    }

    private void menuAgent() {
        System.out.println("=== MENU AGENT ===");
        System.out.println("1. Prise en charge d'un incident");
        System.out.println("2. Tous les incidents "); // Nouvelle option
        System.out.println("3. Voir mes assignations");
        System.out.println("4. Detail d'un incident");
        System.out.println("5. Quitter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();

        try {
            switch (choix) {
                case "1" -> voirEtPrendreEnCharge();
                case "2" -> voirTousLesIncidents(); // Nouvelle méthode
                case "3" -> voirMesAssignations();
                case "4" -> voirDetail();
                case "5" -> {
                    token=null;
                    return;
                }
                default  -> System.out.println("Choix invalide.");
            }
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
        }
    }

    private void voirTousLesIncidents() throws Exception {
        System.out.println("--- Liste d'incidents ---");
        List<Incident> tous = service.listerTousLesIncidents(token.getValeur());
        if (tous.isEmpty()) {
            System.out.println("Aucun incident dans le système.");
        } else {
            for (Incident i : tous) {
                System.out.println(i);
            }
        }
    }

    private void voirEtPrendreEnCharge() throws Exception {
        System.out.println("--- Incidents en attente ---");
        List<Incident> ouverts = service.listerIncidentsOuverts(token.getValeur());

        if (ouverts.isEmpty()) {
            System.out.println("Aucun incident ouvert.");
            return;
        }

        for (Incident i : ouverts) {
            System.out.println(i);
        }

        System.out.print("\nID de l'incident à prendre en charge (ou Entrée pour annuler) : ");
        String idStr = scanner.nextLine().trim();
        if (idStr.isEmpty()) return;

        int id = Integer.parseInt(idStr);
        boolean ok = service.prendreEnChargeIncident(token.getValeur(), id);

        if (ok) {
            System.out.println("[SUCCÈS] Vous avez pris en charge l'incident #" + id);
        }
    }

    private void voirMesAssignations() throws Exception {
        System.out.println("--- Mes assignations ---");
        List<Incident> liste = service.listerMesAssignations(token.getValeur());
        if (liste.isEmpty()) {
            System.out.println("Vous n'avez aucun incident en cours.");
        } else {
            for (Incident i : liste) System.out.println(i);
        }
    }

    private void menuUtilisateur() {
        System.out.println("=== MENU UTILISATEUR ===");
        System.out.println("1. Signaler un incident");
        System.out.println("2. Mes incidents");
        System.out.println("3. Detail d'un incident");
        System.out.println("4. Quitter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();

        try {
            switch (choix) {
                case "1" -> signalerIncident();
                case "2" -> voirMesIncidents();
                case "3" -> voirDetail();
                case "4" -> {
                        token=null;
                        return;
                }
                default  -> System.out.println("Choix invalide.");
            }
        } catch (Exception e) {
            System.err.println("[ERREUR] " + e.getMessage());
        }
    }

    private void signalerIncident() throws Exception {
        System.out.println("--- Nouveau signalement ---");
        System.out.print("Categorie : ");
        String cat = scanner.nextLine().trim();
        System.out.print("Titre : ");
        String titre = scanner.nextLine().trim();
        System.out.print("Description : ");
        String desc = scanner.nextLine().trim();

        if (cat.isEmpty() || titre.isEmpty()) {
            System.out.println("Erreur : Le titre et la catégorie sont obligatoires.");
            return;
        }

        Incident i = service.signalerIncident(token.getValeur(), cat, titre, desc);
        if (i != null) {
            System.out.println("[SUCCÈS] Incident créé avec l'ID #" + i.getId());
        } else {
            System.out.println("[ERREUR] Échec de la création.");
        }
    }

    private void voirMesIncidents() throws Exception {
        System.out.println("--- Mes incidents signalés ---");
        List<Incident> liste = service.listerMesIncidents(token.getValeur());
        if (liste.isEmpty()) {
            System.out.println("Vous n'avez aucun incident enregistré.");
        } else {
            for (Incident i : liste) {
                System.out.println(i);
            }
        }
    }

    private void voirDetail() throws Exception {
        System.out.print("ID de l'incident : ");
        String idStr = scanner.nextLine().trim();
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { System.out.println("ID invalide."); return; }
        Incident i = service.consulterIncident(token.getValeur(), id); // via le token et l'id de l'incident
        if (i == null) { System.out.println("Incident #" + id + " introuvable."); return; }
        System.out.println("--- Detail incident #" + id + " ---");
        System.out.println("Titre       : " + i.getTitre());
        System.out.println("Categorie   : " + i.getCategorie());
        System.out.println("Description : " + i.getDescription());
        System.out.println("Statut      : " + i.getStatut());
        System.out.println("Auteur  : " + i.getUserUuid());
        System.out.println("Date        : " + i.getDateCreation());
    }

    private String hacher(String motDePasse) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
            // La méthode readPassword masque la saisie par défaut
            char[] passwordChars = console.readPassword("Mot de passe : ");
            return new String(passwordChars);
        } else {
            // Solution de secours pour les IDE (IntelliJ), le texte sera visible
            System.out.print("Mot de passe : ");
            return scanner.nextLine().trim();
        }
    }

    public static void main(String[] args) {
        String authHost   = (args.length > 0) ? args[0] : "localhost";  // args[0] = adresse du serveur Auth
        String serverHost = (args.length > 1) ? args[1] : "localhost";  // args[1] = serveur HelpOps
        new HelpOpsClient(authHost, serverHost).demarrer();
    }
}

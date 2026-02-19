package helpops.client;

import helpops.interfaces.IAuthService;
import helpops.interfaces.IHelpOps;
import helpops.model.Incident;
import helpops.model.Token;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class HelpOpsClient {

    private IAuthService auth;
    private IHelpOps     service;
    private Scanner      scanner;

    private Token  token;    // token de session obtenu apres connexion

    // Constructeur : connexion aux deux serveurs RMI

    public HelpOpsClient(String authHost, String serverHost) {
        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("console.encoding", "UTF-8");

            // Connexion au serveur Auth
            Registry authRegistry = LocateRegistry.getRegistry(authHost, 2000);
            auth = (IAuthService) authRegistry.lookup("AuthService");
            System.out.println("[CLIENT] Serveur Auth : " + auth.ping());

            // Connexion au serveur principal
            Registry serverRegistry = LocateRegistry.getRegistry(serverHost, 1099);
            service = (IHelpOps) serverRegistry.lookup("HelpOps");
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
        System.out.println("1. Se connecter");
        System.out.println("2. Creer un compte");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        System.out.println();

        boolean ok = false;
        if ("1".equals(choix)) {
            ok = seConnecter();
        } else if ("2".equals(choix)) {
            ok = creerCompte();
        } else {
            System.out.println("Choix invalide.");
        }

        if (!ok) {
            System.out.println("Fermeture.");
            scanner.close();
            return;
        }
        menuPrincipal();
        scanner.close();
    }

    private boolean seConnecter() {
        System.out.println("=== CONNEXION ===");
        for (int tentative = 1; tentative <= 3; tentative++) {
            try {
                System.out.print("Login : ");
                String login = scanner.nextLine().trim();
                String mdp = lireMotDePasse();

                token = auth.connecter(login, mdp);
                if (token != null) {
                    System.out.println("Bienvenue " + token.getLogin() + " !");
                    System.out.println();
                    return true;
                }

                int restant = 3 - tentative;
                if (restant > 0) System.out.println("Login ou mot de passe incorrect. " + restant + " tentative(s) restante(s).\n");
                else             System.out.println("Nombre maximum de tentatives atteint.");

            } catch (Exception e) {
                System.err.println("[ERREUR] " + e.getMessage());
            }
        }
        return false;
    }

    private boolean creerCompte() {
        System.out.println("=== CREATION DE COMPTE ===");
        try {
            System.out.print("Login souhaite : ");
            String login = scanner.nextLine().trim();
            String mdp   = lireMotDePasse();

            boolean ok = auth.inscrire(login, mdp);
            if (ok) {
                System.out.println("Compte cree ! Connexion automatique...\n");
                token = auth.connecter(login, mdp);
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
        boolean continuer = true;
        while (continuer) {
            System.out.println("=== MENU ===");
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
                    case "4" -> { continuer = false; System.out.println("Deconnexion."); }
                    default  -> System.out.println("Choix invalide (1-4).");
                }
            } catch (Exception e) {
                System.err.println("[ERREUR] " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void signalerIncident() throws Exception {
        System.out.println("--- Signaler un incident ---");
        System.out.print("Categorie : ");
        String categorie  = scanner.nextLine().trim();
        System.out.print("Titre : ");
        String titre      = scanner.nextLine().trim();
        System.out.print("Description : ");
        String description = scanner.nextLine().trim();

        if (categorie.isBlank() || titre.isBlank() || description.isBlank()) {
            System.out.println("Tous les champs sont obligatoires.");
            return;
        }

        Incident incident = service.signalerIncident(token.getValeur(), categorie, titre, description);
        if (incident != null) System.out.println("Incident cree : " + incident);
        else                  System.out.println("Erreur lors de la creation.");
    }

    private void voirMesIncidents() throws Exception {
        System.out.println("--- Mes incidents ---");
        List<Incident> liste = service.listerMesIncidents(token.getValeur());
        if (liste == null || liste.isEmpty()) {
            System.out.println("Aucun incident.");
            return;
        }
        for (Incident i : liste) System.out.println(i);
        System.out.println("Total : " + liste.size() + " incident(s)");
    }

    private void voirDetail() throws Exception {
        System.out.print("ID de l'incident : ");
        String idStr = scanner.nextLine().trim();
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { System.out.println("ID invalide."); return; }

        Incident i = service.consulterIncident(token.getValeur(), id);
        if (i == null) { System.out.println("Incident #" + id + " introuvable."); return; }

        System.out.println("--- Detail incident #" + id + " ---");
        System.out.println("Titre       : " + i.getTitre());
        System.out.println("Categorie   : " + i.getCategorie());
        System.out.println("Description : " + i.getDescription());
        System.out.println("Statut      : " + i.getStatut());
        System.out.println("Login       : " + i.getLogin());
        System.out.println("Date        : " + i.getDateCreation());
    }

    private String lireMotDePasse() {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword("Mot de passe : ");
            return new String(chars);
        }
        System.out.print("Mot de passe : ");
        return scanner.nextLine().trim();
    }

    public static void main(String[] args) {
        // Par defaut : tous les serveurs sur localhost
        // args[0] = adresse du serveur Auth, args[1] = adresse du serveur HelpOps
        String authHost   = (args.length > 0) ? args[0] : "localhost";
        String serverHost = (args.length > 1) ? args[1] : "localhost";

        new HelpOpsClient(authHost, serverHost).demarrer();
    }
}

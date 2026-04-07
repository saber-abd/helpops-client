package helpops.client;

import helpops.interfaces.RMIHelpOps;
import helpops.model.Incident;
import helpops.model.Token;
import java.rmi.RemoteException;
import java.util.List;

public class AgentAction {

    public static void voirStatistiques(RMIHelpOps service, Token token) throws Exception {
        System.out.println("--- Statistiques ---");
        System.out.println(service.getStatistiques(token.getValeur()));
    }

    public static void voirEtPrendreEnCharge(RMIHelpOps service, Token token,java.util.Scanner scanner) throws Exception {
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
        try {
            boolean ok = service.prendreEnChargeIncident(token.getValeur(), id);
            if (ok) System.out.println("[SUCCES] Vous avez pris en charge l'incident #" + id);
        } catch (RemoteException e) {
            String message = e.getMessage();
            if (message.contains("nested exception is:")) {
                message = message.substring(message.lastIndexOf(":") + 1).trim();
            }
            System.out.println("\n[INFO] " + message);
        } catch (Exception e) {
            System.out.println("[ERREUR TECHNIQUE] " + e.getMessage());
        }
    }

    public static void voirTousLesIncidents(RMIHelpOps service,Token token) throws Exception {
        System.out.println("--- Liste de tous les incidents ---");
        List<Incident> tous = service.listerTousLesIncidents(token.getValeur());
        if (tous == null || tous.isEmpty()) {
            System.out.println("Aucun incident dans le systeme.");
        } else {
            for (Incident i : tous) System.out.println(i);
        }
    }

    public static void voirMesAssignations(RMIHelpOps service,Token token) throws Exception {
        System.out.println("--- Mes assignations ---");
        List<Incident> liste = service.listerMesAssignations(token.getValeur());
        if (liste == null || liste.isEmpty()) {
            System.out.println("Vous n'avez aucun incident assigne.");
        } else {
            for (Incident i : liste) System.out.println(i);
        }
    }

    // v3 resolution ticket assigne a l'agent connecte
    public static void resoudreTicket(RMIHelpOps service,Token token,java.util.Scanner scanner) throws Exception {
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
    public static void creerTicketPourUser(RMIHelpOps service,Token token, java.util.Scanner scanner) throws Exception {
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

    // Action permettant à un agent de transférer un ticket dont il a la charge
    public static void deleguerTicket(RMIHelpOps service, Token token,java.util.Scanner scanner) throws Exception {
        System.out.println("--- Deleguer un ticket ---");

        List<Incident> mesTickets = service.listerMesAssignations(token.getValeur());
        if (mesTickets == null || mesTickets.isEmpty()) {
            System.out.println("Vous n'avez aucun ticket à déléguer.");
            return;
        }

        for (Incident i : mesTickets) {
            if ("ASSIGNED".equalsIgnoreCase(i.getStatut())) {
                System.out.println(i);
            }
        }

        System.out.print("\nID du ticket a deleguer : ");
        String idStr = scanner.nextLine().trim();
        if (idStr.isEmpty()) return;
        int id = Integer.parseInt(idStr);

        System.out.print("Login de l'agent a qui vous voulez transmettre le ticket : ");
        String loginCible = scanner.nextLine().trim();

        if (loginCible.isEmpty()) return;

        boolean ok = service.deleguerTicket(token.getValeur(), id, loginCible);
        if (ok) {
            System.out.println("[SUCCES] Le ticket #" + id + " a été delegue a " + loginCible);
        } else {
            System.out.println("[ECHEC] Impossible de deleguer ce ticket (Verifiez l'id et/ou vos droits).");
        }
    }

}
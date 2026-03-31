package helpops.client;

import helpops.interfaces.RMISupervisionClient;
import helpops.model.Evenement;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SupervisionHandler extends UnicastRemoteObject implements RMISupervisionClient {

    protected SupervisionHandler() throws RemoteException {
        super();
    }
    @Override
    public void notifier(Evenement e) throws RemoteException {
        System.out.println("\n>>> [ALERTE] " + e.toString());
    }
}
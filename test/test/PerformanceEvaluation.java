package test;

import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

public class PerformanceEvaluation {

	private static ResourceManager rm;
	
	public static void main(String[] args) {
		String server = "localhost";
		int port = 1099;
		if (args.length == 1)
			server = args[0];
		if (args.length == 2) {
			server = args[0];
			port = Integer.parseInt(args[1]);
		} else if (args.length != 0 && args.length != 2) {
			System.out.println("Usage: java client [rmihost [rmiport]]");
			System.exit(1);
		}

		try {
			// get a reference to the rmiregistry
			Registry registry = LocateRegistry.getRegistry(server, port);
			// get the proxy and the remote reference by rmiregistry lookup
			rm = (ResourceManager) registry.lookup("Group1ResourceManager");
			if (rm != null) {
				System.out.println("Successful");
				System.out.println("Connected to RM");
			} else {
				System.out.println("Unsuccessful");
			}
			// make call on remote method
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
	}

}

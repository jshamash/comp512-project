import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

public class Middleware {

	static ResourceManager flightRM = null;
	static ResourceManager carRM = null;
	static ResourceManager roomRM = null;

	public static void main(String args[]) {
		String flightServer, carServer, roomServer;
		int flightPort, carPort, roomPort;

		if (args.length != 6) {
			System.err.println("Wrong usage");
			System.out
					.println("Usage: java ResImpl.ResourceManagerImpl [flightServer] [flightPort] "
							+ "[carServer] [carPort] [roomServer] [roomPort]");
			System.exit(1);
		}

		flightServer = args[0];
		flightPort = Integer.parseInt(args[1]);
		carServer = args[2];
		carPort = Integer.parseInt(args[3]);
		roomServer = args[4];
		roomPort = Integer.parseInt(args[5]);

		try {
			// get a reference to the flight rmiregistry
			Registry registry = LocateRegistry.getRegistry(flightServer, flightPort);
			// get the proxy and the remote reference by rmiregistry lookup
			flightRM = (ResourceManager) registry
					.lookup("Group1ResourceManager");
			if (flightRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Flight RM");
			} else {
				System.out.println("Unsuccessful");
			}

			// get a reference to the car rmiregistry
			registry = LocateRegistry.getRegistry(carServer, carPort);
			// get the proxy and the remote reference by rmiregistry lookup
			carRM = (ResourceManager) registry.lookup("Group1ResourceManager");
			if (carRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Car RM");
			} else {
				System.out.println("Unsuccessful");
			}
			
			// get a reference to the room rmiregistry
			registry = LocateRegistry.getRegistry(roomServer, roomPort);
			// get the proxy and the remote reference by rmiregistry lookup
			roomRM = (ResourceManager) registry.lookup("Group1ResourceManager");
			if (roomRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Room RM");
			} else {
				System.out.println("Unsuccessful");
			}
		} catch (Exception e) {
			System.err.println("Middleware exception: " + e.toString());
			e.printStackTrace();
		}
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
	}
}

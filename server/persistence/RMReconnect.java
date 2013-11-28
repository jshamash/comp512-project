package persistence;

import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

/**
 * Tries to reconnect to the specified RM. On success, updates the middleware and transaction manager.
 * @author Jake
 *
 */
public abstract class RMReconnect implements Runnable {

	private static final int RETRY_INTERVAL = 5000;
	
	private String hostname;
	private int port;
	private ResourceManager crashedRM = null;

	public RMReconnect(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		
	}
	
	public ResourceManager getRM() {
		return crashedRM;
	}
	
	/**
	 * The actions to be taken when a connection is established.
	 */
	public abstract void onComplete();

	@Override
	public void run() {
		while (crashedRM == null) {
			try {
				// get a reference to the flight rmiregistry
				Registry registry = LocateRegistry.getRegistry(hostname, port);
				// get the proxy and the remote reference by rmiregistry lookup
				crashedRM = (ResourceManager) registry
						.lookup("Group1ResourceManager");
				Thread.sleep(RETRY_INTERVAL);
			} catch (Exception e) {
				System.err.println("RMReconnect exception: " + e.toString());
				e.printStackTrace();
			}
		}
		
		System.out.println("Connected!");
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		
		this.onComplete();
	}

}

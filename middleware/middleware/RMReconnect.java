package middleware;

import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

/**
 * Tries to reconnect to the specified RM. On success, updates the middleware and transaction manager.
 * @author Jake
 *
 */
public abstract class RMReconnect extends Thread {

	private static final int RETRY_INTERVAL = 500;
	
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
		System.out.println("Trying to reconnect to an RM...");
		while (crashedRM == null) {
			// Try to connect
			try {
				// get a reference to the rmiregistry
				Registry registry = LocateRegistry.getRegistry(hostname, port);
				// get the proxy and the remote reference by rmiregistry lookup
				crashedRM = (ResourceManager) registry.lookup("Group1ResourceManager");
			} catch (Exception e) {
				// Can't connect yet, keep trying
			}
			
			// Sleep
			try {
				Thread.sleep(RETRY_INTERVAL);
			} catch (InterruptedException e) {
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

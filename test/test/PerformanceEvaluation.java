package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;

import LockManager.DeadlockException;
import ResInterface.ResourceManager;

public class PerformanceEvaluation {

	private static ResourceManager rm;

	public static void main(String[] args) {
		String server = "localhost";
		int port = 9090;
		int iterations = 0;
		long before;
		long after;
		String filename = "results.txt";
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			if (args.length != 3) {
				System.out
						.println("Usage: java client <rmihost> <rmiport> <iterations>");
				System.exit(1);
			} else {
				server = args[0];
				port = Integer.parseInt(args[1]);
				iterations = Integer.parseInt(args[2]);
			}
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

			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}

			before = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				sendTransactionOneRM();
			}
			after = System.currentTimeMillis();
			writer.write("One RM: " + (after - before) + "\n");

			before = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				sendTransactionAllRMs();
			}
			after = System.currentTimeMillis();
			writer.write("All RMs: " + (after - before) + "\n");
			writer.close();
			rm.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendTransactionOneRM() {
		try {
			int xid = rm.start();

			rm.addCars(xid, "montreal", 100, 100);
			rm.queryCars(xid, "montreal");
			rm.deleteCars(xid, "montreal");

			rm.addCars(xid, "montreal", 100, 100);
			rm.queryCars(xid, "montreal");
			rm.deleteCars(xid, "montreal");

			rm.addCars(xid, "montreal", 100, 100);
			rm.queryCars(xid, "montreal");
			rm.deleteCars(xid, "montreal");

			rm.addCars(xid, "montreal", 100, 100);
			rm.queryCars(xid, "montreal");
			rm.deleteCars(xid, "montreal");

			rm.commit(xid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendTransactionAllRMs() {
		try {
			int xid = rm.start();

			rm.addCars(xid, "montreal", 100, 100);
			rm.queryCars(xid, "montreal");
			rm.deleteCars(xid, "montreal");

			rm.addFlight(xid, 10, 100, 100);
			rm.queryFlight(xid, 10);
			rm.deleteFlight(xid, 10);

			rm.addRooms(xid, "montreal", 100, 100);
			rm.queryRooms(xid, "montreal");
			rm.deleteRooms(xid, "montreal");

			int cid = (new Random()).nextInt(500);
			rm.newCustomer(xid, cid);
			rm.queryCustomerInfo(xid, cid);
			rm.deleteCustomer(xid, cid);

			rm.commit(xid);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

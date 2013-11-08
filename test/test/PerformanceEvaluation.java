package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

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

			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}

			System.out.print("Warming up...");
			warmup();
			System.out.println("Done.");

			System.out.print("Testing one RM...");
			before = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				sendTransactionOneRM();
			}
			after = System.currentTimeMillis();
			writer.write("One RM: " + (after - before) + "\n");
			System.out.println("Done.");

			System.out.print("Testing all RMs...");
			before = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				sendTransactionAllRMs();
			}
			after = System.currentTimeMillis();
			writer.write("All RMs: " + (after - before) + "\n");
			System.out.println("Done.");

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

	private static void warmup() {
		try {
			int xid, customerId;
			for (int i = 0; i < 50; i++) {
				xid = rm.start();
				customerId = rm.newCustomer(xid);
				rm.deleteCustomer(xid, customerId);
				rm.addCars(xid, "montreal", 50, 50000);
				rm.deleteCars(xid, "montreal");
				rm.addRooms(xid, "montreal", 50, 500);
				rm.deleteRooms(xid, "montreal");
				rm.addFlight(xid, 10, 10, 10);
				rm.deleteFlight(xid, 10);
				rm.commit(xid);

				Thread.sleep(500);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

}

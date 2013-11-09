package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.Random;

import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;

import LockManager.DeadlockException;
import ResInterface.ResourceManager;

public class DistributedResponseTimeEvaluation {

	private static ResourceManager rm;

	// Lists for transactions
	public LinkedList<Integer> c_ids = new LinkedList<Integer>();
	public LinkedList<Integer> f_ids = new LinkedList<Integer>();
	public LinkedList<String> c_locations = new LinkedList<String>();
	public LinkedList<String> c_locations_unused = new LinkedList<String>();
	public LinkedList<String> r_locations = new LinkedList<String>();
	public LinkedList<String> r_locations_unused = new LinkedList<String>();

	// ms of deviation from required time/transaction.
	private final static int DEVIATION = 100;

	private final static int TRANSACTIONS = 1000;

	public static void main(String[] args) {
		String server = "localhost";
		int port = 9090;
		int txnsPerSec = 0;
		long seconds = 0;
		int id = 0;
		if (args.length != 5) {
			System.out
					.println("Usage: java client <rmihost> <rmiport> <txnsPerSec> <seconds> <client-id>");
		} else {
			server = args[0];
			port = Integer.parseInt(args[1]);
			txnsPerSec = Integer.parseInt(args[2]);
			seconds = Integer.parseInt(args[3]);
			id = Integer.parseInt(args[4]);
		}
		String filename = "client-" + id + ".txt";
		System.out.println(txnsPerSec + " transactions per second");
		System.out.println(seconds + " seconds");

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

		// Now create transactions and run transactions
		
		System.out.println("Creating "+TRANSACTIONS+" for client: "+id);
		LinkedList<Transaction> transactions = createTransactions(TRANSACTIONS, id);

		int ms = (int) ((1.0 / txnsPerSec) * 1000);
		long responseTimeSum = 0;
		long iterations = 0;
		long before;
		long after;
		int min = ms - DEVIATION;
		int max = ms + DEVIATION;

		long stopTime = System.currentTimeMillis() + seconds * 1000;

		warmup(id);

		while (System.currentTimeMillis() < stopTime) {
			for (Transaction t : transactions) {
				
				if (System.currentTimeMillis() >= stopTime)
					break;

				// Generates one transaction per time in interval [ms-DEVIATION,
				// ms+DEVIATION]
				long timePeriod = min + (int) (Math.random() * (max - min) + 1);
				before = System.currentTimeMillis();
				t.run();
				after = System.currentTimeMillis();
				System.out.println("Txn took " + (after-before) + " ms");
				responseTimeSum += (after - before);
				iterations++;
				try {
					long sleepTime = timePeriod
							- (System.currentTimeMillis() - before);
					if (sleepTime > 0)
						Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Spent " + (System.currentTimeMillis() - before) + " on this txn");
			}
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write("Average response time: " + (double) responseTimeSum
					/ iterations + "\n");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Creates numbTransactions transactions and returns a linkedList containing
	// all of these transactions
	private static LinkedList<Transaction> createTransactions(int numbTransactions, int clientId) {
		LinkedList<Transaction> transactions = new LinkedList<Transaction>();

		TransactionSharedInformation tsi = new TransactionSharedInformation(clientId);

		for (int i = 0; i < numbTransactions; i++) {
			transactions.addLast(new Transaction(i, tsi, rm));
		}

		return transactions;
	}
	
	private static void warmup(int id) {
		try {
			int xid = rm.start();
			rm.addCars(xid, "montreal-"+id, 10, 10);
			rm.deleteCars(xid, "montreal-"+id);
			rm.addFlight(xid, id, 10, 10);
			rm.deleteFlight(xid, id);
			rm.newCustomer(xid, id);
			rm.deleteCustomer(xid, id);
			rm.addRooms(xid, "montreal-"+id, 10, 10);
			rm.deleteRooms(xid, "montreal-"+id);
			rm.commit(xid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

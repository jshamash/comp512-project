package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;

import ResInterface.ResourceManager;

import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;

import LockManager.DeadlockException;
import ResInterface.ResourceManager;

public class DistributedResponseTimeEvaluation {
	
	private static ResourceManager rm;
	
	//Lists for transactions
	public LinkedList<Integer> c_ids = new LinkedList<Integer>();
	public LinkedList<Integer> f_ids = new LinkedList<Integer>();
	public LinkedList<String> c_locations = new LinkedList<String>();
	public LinkedList<String> c_locations_unused = new LinkedList<String>();
	public LinkedList<String> r_locations = new LinkedList<String>();
	public LinkedList<String> r_locations_unused = new LinkedList<String>();
	
	
	public static void main(String[] args) {
		String server = "localhost";
		int port = 9090;
		int numTransactions = 50;
		long before;
		long after;
		String filename = "results.txt";
		/*BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
		} catch (IOException e1) {
			System.err.println("Couldn't open file " + filename + " for writing");
			System.exit(1);
		}
		if (args.length != 3) {
			System.out.println("Usage: java client <rmihost> <rmiport> <iterations>");
			System.exit(1);
		}
		else {
			server = args[0];
			port = Integer.parseInt(args[1]);
			numTransactions = Integer.parseInt(args[2]);
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
		}*/
		
		
		//Now create transactions and run transactions
		LinkedList<Transaction> transactions = createTransactions(numTransactions);
		
		System.out.println("Number of transaction created: " + transactions.size());
		
		//for (Transaction t : transactions) t.run();
		
		/*try {
			rm.shutdown();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	//Creates numbTransactions transactions and returns a linkedList containing all of these transactions
	private static LinkedList<Transaction> createTransactions(int numbTransactions){
		LinkedList<Transaction> transactions = new LinkedList<Transaction>();
		
		TransactionSharedInformation tsi = new TransactionSharedInformation();
		
		for(int i = 0;i<numbTransactions;i++){
			transactions.addLast(new Transaction(i, tsi, rm));
		}
		
		return transactions;
	}
}

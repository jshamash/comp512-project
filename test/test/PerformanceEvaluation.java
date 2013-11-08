package test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
			iterations = Integer.parseInt(args[2]);
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
		
		before = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			sendTransactionOneRM();
		}
		after = System.currentTimeMillis();
		try {
			writer.write("One RM: " + (after - before) + "\n");
		} catch (IOException e) {
			System.err.println("Couldn't write to file!");
		}
		
		before = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			sendTransactionAllRMs();
		}
		after = System.currentTimeMillis();
		try {
			writer.write("All RMs: " + (after - before) + "\n");
		} catch (IOException e) {
			System.err.println("Couldn't write to file!");
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			rm.shutdown();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
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
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeadlockException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransactionAbortedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidTransactionException e) {
			// TODO Auto-generated catch block
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
			
			int cid = rm.newCustomer(xid);
			rm.queryCustomerInfo(xid, cid);
			rm.deleteCustomer(xid, cid);
			
			rm.commit(xid);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeadlockException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransactionAbortedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidTransactionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void warmupServers(){
		try{
			int xid, customerId;
			for(int i = 0;i<250;i++){
				xid = rm.start();
				customerId = rm.newCustomer(xid);
				rm.commit(xid);
				
				xid = rm.start();
				rm.deleteCustomer(xid, customerId);
				rm.commit(xid);
			}

			for(int i = 0;i<250;i++){
				xid = rm.start();
				rm.addCars(xid, "montreal", 50,50000);
				rm.commit(xid);
				
				xid = rm.start();
				rm.deleteCars(xid, "montreal");
				rm.commit(xid);
			}

			for(int i = 0;i<250;i++){
				xid = rm.start();
				rm.addRooms(xid, "montreal", 50, 500);
				rm.commit(xid);
				
				xid = rm.start();
				rm.addRooms(xid, "montreal", 50, 500);
				rm.commit(xid);
			}
			
			for(int i = 0;i<250;i++){
				xid = rm.start();
				rm.addFlight(xid, 10, 10, 10);
				rm.commit(xid);
				
				xid = rm.start();
				rm.addFlight(xid, 10, 10, 10);
				rm.commit(xid);
			}
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}

}

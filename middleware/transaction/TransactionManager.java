package transaction;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import middleware.Middleware;
import middleware.RMReconnect;
import persistence.TMLogger;
import tools.Constants;
import tools.Constants.RMType;
import tools.Constants.TransactionStatus;
import tools.Serializer;
import ResInterface.ResourceManager;

public class TransactionManager {
	
	private int tid_counter;
	private Middleware customerRM;
	public static ResourceManager carRM, roomRM, flightRM;
	protected HashMap<Integer,LinkedList<RMType>> rm_records = new HashMap<Integer,LinkedList<RMType>>();
	private TransactionMonitor t_monitor;
	
	private static LinkedList<RMType> crashedRMs = new LinkedList<RMType>();
	
	//2PC related variables
	private static HashMap<Integer, TransactionStatus> t_status = new HashMap<Integer, TransactionStatus>();
	
	
	//Setting Transaction Manager
	public TransactionManager(Middleware cust_rm, ResourceManager car_rm, ResourceManager room_rm, ResourceManager flight_rm){
		carRM = car_rm;
		roomRM = room_rm;
		flightRM = flight_rm;
		customerRM = cust_rm;
		t_monitor = new TransactionMonitor(customerRM);
		t_monitor.start();
		
		tid_counter = 0;
	}
	
	//Method that creates a new Transaction id
	private int getNewTransactionId(){
		tid_counter++;
		return tid_counter;
	}
	
	//Check if the transaction has started an the RM specified with the rm_id
	//If not started, do nothing and return
	//If not started, start rm with xid and insert rm_id into list
	public void enlist(int xid, RMType rm_id) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		t_monitor.refresh(xid);
		
		LinkedList<RMType> rm_list = rm_records.get(xid);
		
		//If the rm is already in the list, return true, otherwise add to rm_records list
		if(rm_list.contains(rm_id))
			return;
		
		//Start specified RM + add RM to the linkedList
		switch(rm_id){
			case CUSTOMER:
				try{
					customerRM.start(xid);
					rm_list.add(RMType.CUSTOMER);
				}catch(Exception e){}
				
				break;
			case CAR:
				try{
					carRM.start(xid);
					rm_list.add(RMType.CAR);
				}catch(Exception e){}
				
				break;
			case ROOM:
				try{
					roomRM.start(xid);
					rm_list.add(RMType.ROOM);
				}catch(Exception e){}
				
				break;
			case FLIGHT:
				try{
					flightRM.start(xid);
					rm_list.add(RMType.FLIGHT);
				}catch(Exception e){}
				
				break;
		}
		
		serialize();
	}
	
	//Start called. We create a new xid for the new transaction, 
	//then we create an entry in our hashtable to store which RM's we are
	//connected to. Initially, we are not connected to any RMs
	public int start() {
		int new_xid = getNewTransactionId();
		
		synchronized(rm_records){
			rm_records.put(new_xid, new LinkedList<RMType>());
		}	
		t_status.put(new_xid, TransactionStatus.ACTIVE);
		
		t_monitor.create(new_xid);
		
		
		//TODO we may not even need to do this since so far no RMs are implicated in the txn
		serialize();
		
		return new_xid;
	}
	
	//Simple 2PC algorithm divided in different classes for cleaner vision of protocol
	public boolean twoPhaseCommit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		assert t_status.get(xid) == TransactionStatus.ACTIVE;
		t_monitor.unwatch(xid);
		
		//Call prepare function on each of the RMs
		if(prepare(xid)){
			System.out.println("Everyone prepared");
			assert t_status.get(xid) == TransactionStatus.UNCERTAIN;
			if (commit(xid)) {
				// Everyone committed
				return true;
			}
			else {
				// Some guy couldn't commit
				System.err.println("Somebody couldn't commit!!!!!");
				return true;
			}
			
		}else{
			abort(xid);
			throw new TransactionAbortedException(xid, "Transaction was aborted");
		}
	}
	
	//Prepare functions that takes care of executing the first phase of 2PC, i.e. test if any of the servers have crashed
	public boolean prepare(int xid){
		//Start by setting log information to Start2PC
		t_status.put(xid, TransactionStatus.UNCERTAIN);
		
		// Log start of 2PC
		serialize();
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<RMType> rm_list = rm_records.get(xid);
		
		//Calls prepared function on all RMs related to this transaction
		for(RMType i : rm_list){
			switch(i){
			case CUSTOMER:
				try{
					//Since we are in the same process this message should not fail so we don't create
					//a new thread and immediately return false if the RM returns a NO VOTE
					if(!customerRM.prepare(xid)) return false;
				}catch(RemoteException e){
					// The customer RM is on the same machine so we don't have to worry about crashes.
					return false;
				}
				break;
			case CAR:
				try{
					if(!carRM.prepare(xid)) return false;
				}catch(RemoteException e){
					// carRM crashed -- try to reconnect
					reconnect(RMType.CAR);
					return false;
				} catch (TransactionAbortedException | InvalidTransactionException e) {
					e.printStackTrace();
					return false;
				}
				break;
			case ROOM:
				try {
					if(!roomRM.prepare(xid)) return false;
				} catch (RemoteException e1) {
					// roomRM crashed -- try to reconnect
					reconnect(RMType.ROOM);
					return false;
				} catch (TransactionAbortedException  | InvalidTransactionException e1) {
					return false;
				}
				break;
			case FLIGHT:
				try{
					if(!flightRM.prepare(xid)) return false;
				}catch(RemoteException e){
					// flightRM crashed -- try to reconnect
					reconnect(RMType.FLIGHT);
					return false;
				} catch (TransactionAbortedException |InvalidTransactionException e) {
					return false;
				}
				break;
			}
		}
		
		return true;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public boolean commit(int xid) throws InvalidTransactionException, TransactionAbortedException{
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<RMType> rm_list = rm_records.remove(xid);
		
		t_monitor.unwatch(xid);
		
		t_status.put(xid, TransactionStatus.COMMIT);
		
		// Log commit record
		serialize();
		
		boolean allCommitAcks = true;
		
		for(RMType i : rm_list){
			switch(i){
				case CUSTOMER:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from customer RM.");
						allCommitAcks = allCommitAcks && customerRM.middlewareCommit(xid);
						System.out.println("Successfully committed transaction "+xid+" from customer RM.");
					} catch(RemoteException e) {//We should not get any error here
						System.out.println("ERROR: Remote exception on customer RM commit");
					}
					break;
					
				case CAR:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from car RM.");
						allCommitAcks = allCommitAcks && carRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from car RM.");
					} catch(RemoteException e) {
						allCommitAcks =  false;
						reconnect(RMType.CAR);
					}
					break;
					
				case ROOM:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from room RM.");
						allCommitAcks = allCommitAcks && roomRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from room RM.");
					} catch(RemoteException e) {
						allCommitAcks = false;
						reconnect(RMType.ROOM);
					}
					break;
				case FLIGHT:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from flight RM.");
						allCommitAcks = allCommitAcks && flightRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from flight RM.");
					} catch(RemoteException e) {
						allCommitAcks = false;
						reconnect(RMType.FLIGHT);
					}
					break;
			}
		}
		
		// If we get in here, everyone committed OK. We can forget about this txn now.
		if (allCommitAcks) {
			t_status.remove(xid);
			serialize();
		}
		
		return allCommitAcks;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public void abort(int xid) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		t_monitor.unwatch(xid);
		System.out.println("Aborting transaction "+xid);
		t_status.put(xid, TransactionStatus.ABORT);
		serialize();
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<RMType> rm_list = rm_records.remove(xid);
		
		boolean allAbortAcks = true;
		
		for(RMType i : rm_list){
			switch(i){
				case CUSTOMER:
					System.out.println("Attempting to abort transaction "+ xid+ " from car RM.");
					try {
						customerRM.middlewareAbort(xid);
					} catch (RemoteException e1) {
						allAbortAcks = false;
					}
					System.out.println("Successfully aborted transaction "+xid+" from customer RM.");
					break;
					
				case CAR:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from car RM.");
						carRM.abort(xid);
						System.out.println("Successfully aborted transaction "+xid+" from car RM.");
					}catch(Exception e){
						allAbortAcks = false;
					}
					break;
					
				case ROOM:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from room RM.");
						roomRM.abort(xid);
						System.out.println("Successfully aborted transaction "+xid+" from room RM.");
					}catch(Exception e){
						allAbortAcks = false;
					}
					break;
				case FLIGHT:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from flight RM.");
						flightRM.abort(xid);
						System.out.println("Successfully aborted transaction "+xid+" from flight RM.");
					}catch(Exception e){
						allAbortAcks = false;
					}
					break;
			}
		}
		
		if (allAbortAcks) {
			// Everyone aborted this txn so we can forget about it
			t_status.remove(xid);
			serialize();
		}
	}
	
	public void recover(TMLogger tm) {
		System.out.println("Recovering TM...");
		this.rm_records = tm.getRm_records();
		TransactionManager.t_status = tm.getT_status();
		this.tid_counter = tm.getTid_counter();
		
		System.out.println("TID counter is " + tid_counter);
		
		for (Integer xid : new HashSet<Integer>(t_status.keySet())) {
			System.out.println("Found outstanding transaction xid " + xid + ", status is " + t_status.get(xid));
			switch (t_status.get(xid)) {
			case ACTIVE:
				try {
					abort(xid);
				} catch (InvalidTransactionException e) {
					// Impossible that this would happen
					e.printStackTrace();
				}
				break;
			case ABORT:
				try {
					abort(xid);
				} catch (InvalidTransactionException e) {
					// Impossible that this would happen
					e.printStackTrace();
				}
				break;
			case COMMIT:
				try {
					commit(xid);
				} catch (InvalidTransactionException
						| TransactionAbortedException e) {
					// This can't happen
					e.printStackTrace();
				}
				break;
			case UNCERTAIN:
				try {
					abort(xid);
				} catch (InvalidTransactionException e) {
					// Impossible that this would happen
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
		System.out.println("Done recovering");
		
		customerRM.recover(t_status);
	}
	
	public static void reconnect(RMType type) {
		if (crashedRMs.contains(type)) return;
		
		crashedRMs.add(type);
		
		System.out.println("Reconnecting to " + type);
		switch (type) {
		case FLIGHT:
			/* restart flight */
			new RMReconnect(Middleware.flightServer, Middleware.flightPort) {
				@Override
				public void onComplete() {
					Middleware.flightRM = this.getRM();
					TransactionManager.flightRM = this.getRM();
					crashedRMs.remove(RMType.FLIGHT);
					try {
						flightRM.initialize(Constants.FLIGHT_FILE_PTR);
						flightRM.recover(t_status);
					} catch (RemoteException e) {
						// Yo dawg, i heard you like reconnects....
						reconnect(RMType.FLIGHT);
					}
				}
			}.start();
			break;
		case CAR:
			/* restart car */
			new RMReconnect(Middleware.carServer, Middleware.carPort) {
				@Override
				public void onComplete() {
					Middleware.carRM = this.getRM();
					TransactionManager.carRM = this.getRM();
					crashedRMs.remove(RMType.CAR);
					try {
						carRM.initialize(Constants.CAR_FILE_PTR);
						carRM.recover(t_status);
					} catch (RemoteException e) {
						reconnect(RMType.CAR);
					}
				}
			}.start();
			break;
		case ROOM:
			/* Restart room */						
			new RMReconnect(Middleware.roomServer, Middleware.roomPort) {
				@Override
				public void onComplete() {
					Middleware.roomRM = this.getRM();
					TransactionManager.roomRM = this.getRM();
					crashedRMs.remove(RMType.ROOM);
					try {
						roomRM.initialize(Constants.ROOM_FILE_PTR);
						roomRM.recover(t_status);
					} catch (RemoteException e) {
						reconnect(RMType.ROOM);
					}
				}
			}.start();
			break;
		default:
			break;
		}
	}
	
	private void serialize() {
		TMLogger log = new TMLogger(tid_counter, rm_records, t_status);
		try {
			Serializer.serialize(log, Constants.TRANSACTION_MANAGER_FILE);
		} catch (Exception e) {
			System.out.println("Couldn't serialize TM!");
			e.printStackTrace();
		}
	}
}

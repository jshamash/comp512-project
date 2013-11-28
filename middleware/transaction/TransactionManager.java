package transaction;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.LinkedList;

import middleware.Middleware;
import ResInterface.ResourceManager;


public class TransactionManager implements Serializable {
	
	private static final long serialVersionUID = -7778289931614182560L;
	
	//Enums to easily find each RM
	public final static int CUSTOMER = 	0;
	public final static int CAR = 		1;
	public final static int ROOM = 		3;
	public final static int FLIGHT = 	4;
	
	
	private int tid_counter;
	private Middleware customerRM;
	private ResourceManager carRM, roomRM, flightRM;
	protected Hashtable<Integer,LinkedList<Integer>> rm_records = new Hashtable<Integer,LinkedList<Integer>>();
	private TransactionMonitor t_monitor;
	
	
	//Setting Transaction Manager
	public TransactionManager(Middleware cust_rm, ResourceManager car_rm, ResourceManager room_rm, ResourceManager flight_rm){
		carRM = car_rm;
		roomRM = room_rm;
		flightRM = flight_rm;
		customerRM = cust_rm;
		t_monitor = new TransactionMonitor(this);
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
	public void enlist(int xid, int rm_id) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		t_monitor.refresh(xid);
		
		LinkedList<Integer> rm_list = rm_records.get(xid);
		
		//If the rm is already in the list, return true, otherwise add to rm_records list
		if(rm_list.contains(rm_id))
			return;
		
		//Start specified RM + add RM to the linkedList
		switch(rm_id){
			case CUSTOMER:
				try{
					customerRM.start(xid);
					rm_list.add(CUSTOMER);
				}catch(Exception e){}
				
				break;
			case CAR:
				try{
					carRM.start(xid);
					rm_list.add(CAR);
				}catch(Exception e){}
				
				break;
			case ROOM:
				try{
					roomRM.start(xid);
					rm_list.add(ROOM);
				}catch(Exception e){}
				
				break;
			case FLIGHT:
				try{
					flightRM.start(xid);
					rm_list.add(FLIGHT);
				}catch(Exception e){}
				
				break;
		}
	}
	
	//Start called. We create a new xid for the new transaction, 
	//then we create an entry in our hashtable to store which RM's we are
	//connected to. Initially, we are not connected to any RMs
	public int start() {
		int new_xid = getNewTransactionId();
		
		synchronized(rm_records){
			rm_records.put(new_xid, new LinkedList<Integer>());
		}
		
		t_monitor.create(new_xid);
		
		return new_xid;
	}
	
	//Prepare functions that takes care of executing the first phase of 2PC, i.e. test if any of the servers have crashed
	public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<Integer> rm_list = rm_records.get(xid);
		
		for(Integer i : rm_list){
			switch(i){
			case CUSTOMER:
				try{
					if(!customerRM.firstPhaseACK(xid)) return false;
				}catch(Exception e){
					return false;
				}
				break;
			case CAR:
				try{
					if(!carRM.firstPhaseACK(xid)) return false;
				}catch(Exception e){
					return false;
				}
				break;
			case ROOM:
				try{
					if(!roomRM.firstPhaseACK(xid)) return false;
				}catch(Exception e){
					return false;
				}
				break;
			case FLIGHT:
				try{
					if(!flightRM.firstPhaseACK(xid)) return false;
				}catch(Exception e){
					return false;
				}
				break;
			}
			
		}
		
		
		return true;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public boolean commit(int xid) throws InvalidTransactionException{
		
		// THIS IS THE FIRST STEP IN 2PC
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		t_monitor.unwatch(xid);
		
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<Integer> rm_list = rm_records.remove(xid);
		
		for(Integer i : rm_list){
			switch(i){
				case CUSTOMER:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from customer RM.");
						if(!customerRM.middlewareCommit(xid)) return false;
						System.out.println("Successfully committed transaction "+xid+" from customer RM.");
					}catch(Exception e){return false;}				
					break;
					
				case CAR:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from car RM.");
						if(!carRM.commit(xid)) return false;
						System.out.println("Successfully committed transaction "+xid+" from car RM.");
					}catch(Exception e){return false;}
					break;
					
				case ROOM:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from room RM.");
						if(!roomRM.commit(xid)) return false;
						System.out.println("Successfully committed transaction "+xid+" from room RM.");
					}catch(Exception e){return false;}
					break;
				case FLIGHT:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from flight RM.");
						if(!flightRM.commit(xid))return false;
						System.out.println("Successfully committed transaction "+xid+" from flight RM.");
					}catch(Exception e){return false;}
					break;
			}
		}
		
		return true;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public void abort(int xid) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		t_monitor.unwatch(xid);
		
		System.out.println("Aborting transaction "+xid);
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<Integer> rm_list = rm_records.remove(xid);
		
		for(Integer i : rm_list){
			switch(i){
				case CUSTOMER:
					System.out.println("Attempting to abort transaction "+ xid+ " from car RM.");
					try {
						customerRM.middlewareAbort(xid);
					} catch (RemoteException e1) {}
					System.out.println("Successfully aborted aborted transaction "+xid+" from car RM.");
					break;
					
				case CAR:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from car RM.");
						carRM.abort(xid);
						System.out.println("Successfully aborted aborted transaction "+xid+" from car RM.");
					}catch(Exception e){}
					break;
					
				case ROOM:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from room RM.");
						roomRM.abort(xid);
						System.out.println("Successfully aborted aborted transaction "+xid+" from car RM.");
					}catch(Exception e){}
					break;
				case FLIGHT:
					try{
						System.out.println("Attempting to abort transaction "+ xid+ " from flight RM.");
						flightRM.abort(xid);
						System.out.println("Successfully aborted aborted transaction "+xid+" from car RM.");
					}catch(Exception e){}
					break;
			}
		}
	}
}

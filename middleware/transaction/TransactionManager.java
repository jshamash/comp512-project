package transaction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import ResInterface.ResourceManager;


public class TransactionManager {
	
	//Enums to easily find each RM
	public final static int CUSTOMER = 	0;
	public final static int CAR = 		1;
	public final static int ROOM = 		3;
	public final static int FLIGHT = 	4;
	
	
	private int tid_counter;
	private ResourceManager customerRM, carRM, roomRM, flightRM;
	protected Hashtable<Integer,LinkedList<Integer>> rm_records = new Hashtable<Integer,LinkedList<Integer>>();
	
	
	//Setting Transaction Manager
	public TransactionManager(ResourceManager cust_rm, ResourceManager car_rm, ResourceManager room_rm, ResourceManager flight_rm){
		carRM = car_rm;
		roomRM = room_rm;
		flightRM = flight_rm;
		customerRM = cust_rm;
		
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
		
		return new_xid;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public boolean commit(int xid) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<Integer> rm_list = rm_records.remove(xid);
		boolean excCustCommit= false;
		
		for(Integer i : rm_list){
			switch(i){
				case CUSTOMER:
					excCustCommit = true;					
					break;
					
				case CAR:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from car RM.");
						carRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from car RM.");
					}catch(Exception e){}
					break;
					
				case ROOM:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from room RM.");
						roomRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from room RM.");
					}catch(Exception e){}
					break;
				case FLIGHT:
					try{
						System.out.println("Attempting to commit transaction "+ xid+ " from flight RM.");
						flightRM.commit(xid);
						System.out.println("Successfully committed transaction "+xid+" from flight RM.");
					}catch(Exception e){}
					break;
			}
		}
		
		return excCustCommit;
	}
	
	//Commit function that checks which RM to call to delete hash table entries from corresponding RMs
	//This function ensures that we do not have to call abort on all of the RMs
	public boolean abort(int xid) throws InvalidTransactionException{
		if(rm_records.get(xid)==null) throw new InvalidTransactionException(xid, "Transaction "+ xid+ " does not exist in the Transaction Manager.");
		
		System.out.println("Aborting transaction "+xid);
		
		//Both get the correct Hashtable and removes it from the rm_records hashTable --> 2 in 1 baby
		LinkedList<Integer> rm_list = rm_records.remove(xid);
		boolean excCustAbort= false;
		
		for(Integer i : rm_list){
			switch(i){
				case CUSTOMER:
					excCustAbort = true;
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
		
		return excCustAbort;
	}
}

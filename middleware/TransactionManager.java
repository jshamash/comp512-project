import java.util.Hashtable;
import java.util.LinkedList;

import LockManager.DeadlockException;
import LockManager.LockManager;


public class TransactionManager {

	private int tid_counter;
	
	
	//WHAT DOES THIS DO???
	protected Hashtable<Integer,Hashtable<String,Integer>> m_transactionManager = new Hashtable<Integer,Hashtable<String,Integer>>();
	
	//Setting Transaction Manager
	TransactionManager(){
		tid_counter = 1;
		//transactions = new LinkedList<Transaction>()	
	}
	
	//Method that creates a new Transaction id
	public int getNewTransactionId(){
		int newTID = tid_counter;
		tid_counter++;
		return newTID;
	}
	
	public int start(){
		int newTID = getNewTransactionId();
		
		//Need to store new transaction into the some type of memory (HASH table?)
		
		
		return newTID;
	}
	
	public void commit(int p_tip){
		//Find Object transaction in TID and delete
	}
}

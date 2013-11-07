package transaction;
import java.util.Hashtable;
import java.util.LinkedList;

import LockManager.DeadlockException;
import LockManager.LockManager;


public class TransactionManager {

	private int tid_counter;
	
	//Setting Transaction Manager
	public TransactionManager(){
		tid_counter = 0;
		//transactions = new LinkedList<Transaction>()	
	}
	
	//Method that creates a new Transaction id
	public int getNewTransactionId(){
		tid_counter++;
		return tid_counter;
	}
}

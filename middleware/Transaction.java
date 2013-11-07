
public class Transaction {
	private int id;
	private Thread thread;
	//We also need a list of reverse operations in the transaction
	
	//Creates transaction
	Transaction(int pId, Thread pThread){
		id = pId;
		thread = pThread;
	}
	
	public Thread getThread(){
		return thread;
	}
	
	public int getId(){
		return id;
	}
}

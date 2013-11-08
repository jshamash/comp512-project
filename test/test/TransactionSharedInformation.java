package test;

import java.util.LinkedList;

public class TransactionSharedInformation {

	private int clientId;
	
	public int customerIdCounter;
	public int flightIdCounter;
	
	//Lists for transactions
	public LinkedList<Integer> c_ids = new LinkedList<Integer>();
	public LinkedList<Integer> f_ids = new LinkedList<Integer>();
	public LinkedList<String> c_locations = new LinkedList<String>();
	public LinkedList<String> c_locations_unused = new LinkedList<String>();
	public LinkedList<String> r_locations = new LinkedList<String>();
	public LinkedList<String> r_locations_unused = new LinkedList<String>();
	
	
	public TransactionSharedInformation(int clientId){
		this.clientId = clientId;
		fillInLocationLinkedList(clientId);
		customerIdCounter = 0;
		flightIdCounter = 0;
	}
	
	public int getNewCustomerId(){
		String id = ""+(++customerIdCounter)+""+clientId+""+clientId+""+clientId+""+clientId+""+clientId;
		return Integer.parseInt(id);
	}

	public int getNewFlightId(){
		String id = ""+(++flightIdCounter)+""+clientId+""+clientId+""+clientId+""+clientId+""+clientId;
		return Integer.parseInt(id);
	}
	
	//Transaction helper functions
	private void fillInLocationLinkedList(int clientId){
		c_locations_unused.add("montreal"+clientId);
		c_locations_unused.add("toronto"+clientId);
		c_locations_unused.add("winnipeg"+clientId);
		c_locations_unused.add("calgary"+clientId);
		c_locations_unused.add("boston"+clientId);
		c_locations_unused.add("philadelphy"+clientId);
		c_locations_unused.add("newyork"+clientId);
		c_locations_unused.add("ottawa"+clientId);
		c_locations_unused.add("edmonton"+clientId);
		c_locations_unused.add("vancouver"+clientId);
		c_locations_unused.add("quebec"+clientId);
		c_locations_unused.add("seattle"+clientId);
		c_locations_unused.add("florida"+clientId);
		c_locations_unused.add("carolina"+clientId);
		c_locations_unused.add("pittsburgh"+clientId);
		c_locations_unused.add("washington"+clientId);
		c_locations_unused.add("dallas"+clientId);
		c_locations_unused.add("losangeles"+clientId);
		c_locations_unused.add("lasvegas"+clientId);
		c_locations_unused.add("phoenix"+clientId);
		c_locations_unused.add("tampabay"+clientId);
		c_locations_unused.add("buffalo"+clientId);
		c_locations_unused.add("detroit"+clientId);
		c_locations_unused.add("chicago"+clientId);
		c_locations_unused.add("anaheim"+clientId);
		c_locations_unused.add("sanjose"+clientId);
		c_locations_unused.add("colorado"+clientId);
		c_locations_unused.add("saintlouis"+clientId);
		c_locations_unused.add("columbus"+clientId);
		c_locations_unused.add("minnesota"+clientId);
		c_locations_unused.add("paris"+clientId);
		c_locations_unused.add("london"+clientId);
		c_locations_unused.add("mexico"+clientId);
		c_locations_unused.add("brasil"+clientId);
		c_locations_unused.add("moscow"+clientId);
		c_locations_unused.add("hongkong"+clientId);
		r_locations_unused = (LinkedList<String>) c_locations_unused.clone();
	}
	
}

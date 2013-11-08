package test;

import java.util.LinkedList;

public class TransactionSharedInformation {

	public int customerIdCounter;
	public int flightIdCounter;
	
	//Lists for transactions
	public LinkedList<Integer> c_ids = new LinkedList<Integer>();
	public LinkedList<Integer> f_ids = new LinkedList<Integer>();
	public LinkedList<String> c_locations = new LinkedList<String>();
	public LinkedList<String> c_locations_unused = new LinkedList<String>();
	public LinkedList<String> r_locations = new LinkedList<String>();
	public LinkedList<String> r_locations_unused = new LinkedList<String>();
	
	
	public TransactionSharedInformation(){
		fillInLocationLinkedList();
		customerIdCounter = 0;
		flightIdCounter = 0;
	}
	
	public int getNewCustomerId(){
		return ++customerIdCounter;
	}

	public int getNewFlightId(){
		return ++flightIdCounter;
	}
	
	//Transaction helper functions
	private void fillInLocationLinkedList(){
		c_locations_unused.add("montreal");
		c_locations_unused.add("toronto");
		c_locations_unused.add("winnipeg");
		c_locations_unused.add("calgary");
		c_locations_unused.add("boston");
		c_locations_unused.add("philadelphy");
		c_locations_unused.add("newyork");
		c_locations_unused.add("ottawa");
		c_locations_unused.add("edmonton");
		c_locations_unused.add("vancouver");
		c_locations_unused.add("quebec");
		c_locations_unused.add("seattle");
		c_locations_unused.add("florida");
		c_locations_unused.add("carolina");
		c_locations_unused.add("pittsburgh");
		c_locations_unused.add("washington");
		c_locations_unused.add("dallas");
		c_locations_unused.add("losangeles");
		c_locations_unused.add("lasvegas");
		c_locations_unused.add("phoenix");
		c_locations_unused.add("tampabay");
		c_locations_unused.add("buffalo");
		c_locations_unused.add("detroit");
		c_locations_unused.add("chicago");
		c_locations_unused.add("anaheim");
		c_locations_unused.add("sanjose");
		c_locations_unused.add("colorado");
		c_locations_unused.add("saintlouis");
		c_locations_unused.add("columbus");
		c_locations_unused.add("minnesota");
		c_locations_unused.add("paris");
		c_locations_unused.add("london");
		c_locations_unused.add("mexico");
		c_locations_unused.add("brasil");
		c_locations_unused.add("moscow");
		c_locations_unused.add("hongkong");
		r_locations_unused = (LinkedList<String>) c_locations_unused.clone();
	}
	
}

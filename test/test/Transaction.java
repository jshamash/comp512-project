package test;

import java.util.LinkedList;
import java.util.Random;

import ResInterface.ResourceManager;

public class Transaction {
	private final int NUMBTRANS = 5;
	
	private int t_id;

	private int customerId;
	private int flight_id;
	private int location;
	
	//Lists for transactions
	private TransactionSharedInformation tsi;
	private ResourceManager rm;
	
	private Random randomiser;
	
	Transaction(int i, TransactionSharedInformation tsi, ResourceManager rm){
		this.tsi = tsi;
		this.rm = rm;
		
		//Get a t_id
		randomiser = new Random(i);
		
		t_id = (int)(randomiser.nextDouble()*NUMBTRANS);
		
		setup();
	}
	
	private void setup(){
		switch(t_id){
			case 0://Simply creates a customer
				
				break;
			case 1:
				
				break;
			case 2:
				
				break;
			case 3:
				
				break;
			case 4:
				
				break;
			case 5:
				
				break;
		}
	}
	
	
	
	public void run(){
		//Find correct transaction function to execute based on the transaction special TID
		switch(t_id){
			case 0://Simply creates a customer
				
				break;
			case 1:
				
				break;
			case 2:
				
				break;
			case 3:
				
				break;
			case 4:
				
				break;
			case 5:
				
				break;
		}
	}
	
	
	/*private void Trans1(){
		int xid, customerId;
		try{
			xid = rm.start();
			customerId = rm.newCustomer(xid);
			rm.queryCustomerInfo(xid, customerId);
			rm.commit(xid);
			c_ids.add(customerId);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	private void Trans2(){
		int xid, locationId;
		try{
			xid = rm.start();
			randomiser.setSeed(xid);
			String location = c_locations_unused.get((int)(randomiser.nextDouble()*c_locations_unused.size()));
			if(location != null){
				rm.addCars(xid, location, 10,10);
				//rm.queryCustomerInfo(xid, customerId);
				rm.commit(xid);
				//c_ids.add(customerId);
			}
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}

	private void Trans3(){
		int xid, customerId;
		try{
			xid = rm.start();
			customerId = rm.newCustomer(xid);
			rm.queryCustomerInfo(xid, customerId);
			rm.commit(xid);
			c_ids.add(customerId);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	private void Trans4(){
		int xid, customerId;
		try{
			xid = rm.start();
			customerId = rm.newCustomer(xid);
			rm.queryCustomerInfo(xid, customerId);
			rm.commit(xid);
			c_ids.add(customerId);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	private void Trans5(){
		int xid, customerId;
		try{
			xid = rm.start();
			customerId = rm.newCustomer(xid);
			rm.queryCustomerInfo(xid, customerId);
			rm.commit(xid);
			c_ids.add(customerId);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}*/
	
}

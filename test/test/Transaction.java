package test;

import java.util.LinkedList;
import java.util.Random;

import ResInterface.ResourceManager;

public class Transaction {
	private final int NUMBTRANS = 5;
	
	private int t_id;

	private int customerId;
	private int flight_id;
	private String location;
	
	//Lists for transactions
	private TransactionSharedInformation tsi;
	private ResourceManager rm;
	
	private Random randomiser;
	
	Transaction(int i, TransactionSharedInformation tsi, ResourceManager rm){
		this.tsi = tsi;
		this.rm = rm;
		
		//Get a t_id
		randomiser = new Random(i);
		randomiser.setSeed(i);
		
		t_id = (int)(randomiser.nextInt(NUMBTRANS+1));
		
		System.out.println("Trying to create trans #: " + t_id);
		
		setup();
		
		System.out.println("Creating transaction "+i+":\n\tRandomTransactionId: "
		+t_id+"\n\t\tCustomerId: "+customerId+"\n\t\tFlight Id: "+flight_id+"\n\t\tLocation: "+location);
	}
	
	private void setup(){
		switch(t_id){
			case 0://Simply creates a customer
				customerId = tsi.getNewCustomerId();
				tsi.c_ids.add(customerId);
				break;
			case 1:
				//Requires car location and customer ID
				if(tsi.c_locations.size()==0){
					if(t_id == NUMBTRANS)
						t_id = 0;
					else
						t_id++;
					
					setup();
					break;
				}
				//Obtain random location from car and random customer id from customer
				location = tsi.c_locations.get((int)(randomiser.nextInt(tsi.c_locations.size())));
				customerId = tsi.c_ids.get((int)(randomiser.nextInt(tsi.c_ids.size())));
				break;
			case 2:
				//Requires car location and customer ID
				if(tsi.c_locations_unused.size()==0){
					if(t_id == NUMBTRANS)
						t_id = 0;
					else
						t_id++;
					
					setup();
					break;
				}
				
				location = tsi.c_locations_unused.remove((int)(randomiser.nextInt(tsi.c_locations_unused.size())));
				tsi.c_locations.add(location);
				break;
			case 3://Requires unused room location
				if(tsi.r_locations_unused.size()==0){
					if(t_id == NUMBTRANS)
						t_id = 0;
					else
						t_id++;
					
					setup();
					break;
				}
				
				location = tsi.r_locations_unused.remove((int)(randomiser.nextInt(tsi.r_locations_unused.size())));
				tsi.r_locations.add(location);
				break;
			case 4://Requires a new flight id
				flight_id = tsi.getNewFlightId();
				tsi.f_ids.add(flight_id);
				break;
			case 5://Requires a new customer ID, an existing flight ID, and an existing car location
				//Requires car location and customer ID
				if(tsi.c_locations.size()==0){
					if(t_id == NUMBTRANS)
						t_id = 0;
					else
						t_id++;
					
					setup();
					break;
				}
				
				if(tsi.f_ids.size()==0){
					if(t_id == NUMBTRANS)
						t_id = 0;
					else
						t_id++;
					
					setup();
					break;
				}
				//Car Location
				location = tsi.c_locations.get((int)(randomiser.nextInt(tsi.c_locations.size())));
				
				//Flight id
				flight_id = tsi.f_ids.get((int)(randomiser.nextInt(tsi.f_ids.size())));
				
				//Get new customer
				customerId = tsi.getNewCustomerId();
				tsi.c_ids.add(customerId);
				break;
		}
	}
	
	
	
	public void run(){
		//Find correct transaction function to execute based on the transaction special TID
		switch(t_id){
			case 0://Simply creates a customer
				Trans0();
				break;
			case 1:
				Trans1();
				break;
			case 2:
				Trans2();
				break;
			case 3:
				Trans3();
				break;
			case 4:
				Trans4();
				break;
			case 5:
				Trans5();
				break;
		}
	}
	
	//Adds customer, does not need anything in particular
	private void Trans0(){
		int xid;;
		try{
			xid = rm.start();
			rm.newCustomer(xid, customerId);
			tsi.c_ids.add(customerId);
			rm.queryCustomerInfo(xid, customerId);
			rm.commit(xid);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Query car number car, reserve car. 
	//Requires location and customer ID
	private void Trans1(){
		int xid;
		try{
			xid = rm.start();
			if(rm.queryCars(xid, location)>0)
				rm.reserveCar(xid, customerId, location);
			rm.commit(xid);
			//c_ids.add(customerId);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Adds cars
	//Requires unused car location
	private void Trans2(){
		int xid;
		try{
			xid = rm.start();
			rm.addCars(xid, location, 100,100);
			rm.commit(xid);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Adds rooms
	//Requires unused room location
	private void Trans3(){
		int xid;
		try{
			xid = rm.start();
			rm.addRooms(xid, location, 100,100);
			rm.commit(xid);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Adds flights
	//Requires a new flight id
	private void Trans4(){
		int xid;
		try{
			xid = rm.start();
			rm.addFlight(xid, flight_id, 100,100);
			rm.commit(xid);
		}catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Create a customer and create a flight and a car
	//Requires a new customer ID, an existing flight ID, and an existing car location
	private void Trans5(){
		int xid;
		try{
			xid = rm.start();
			rm.newCustomer(xid, customerId);
			if(rm.queryFlight(xid, flight_id) > 0){
				rm.reserveFlight(xid, customerId, flight_id);
			}
			if(rm.queryCars(xid, location) > 0){
				rm.reserveCar(xid, customerId, location);
			}
			rm.commit(xid);
		}catch(Exception e){
			System.out.println(e.toString());
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

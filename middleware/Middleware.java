import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;
import transaction.TransactionManager;
import ResImpl.Car;
import ResImpl.Customer;
import ResImpl.Flight;
import ResImpl.Hotel;
import ResImpl.RMHashtable;
import ResImpl.RMItem;
import ResImpl.ReservedItem;
import ResImpl.Trace;
import ResInterface.ResourceManager;

public class Middleware implements ResourceManager {

	static ResourceManager flightRM = null;
	static ResourceManager carRM = null;
	static ResourceManager roomRM = null;

	protected RMHashtable m_itemHT = new RMHashtable();
	
	//Create a transaction hashtable to store transaction data --> this will be used fho the real abort shizzle
	protected Hashtable<Integer,HashMap<String,RMItem>> t_records = new Hashtable<Integer,HashMap<String,RMItem>>();
	
	//Our transaction manager
	TransactionManager t_manager = new TransactionManager();

	public Middleware() throws RemoteException {
	}

	public static void main(String args[]) {
		String flightServer, carServer, roomServer;
		int flightPort, carPort, roomPort, rmiPort;

		if (args.length != 7) {
			System.err.println("Wrong usage");
			System.out
					.println("Usage: java Middleware [flightServer] [flightPort] "
							+ "[carServer] [carPort] [roomServer] [roomPort] [RMI port]");
			System.exit(1);
		}

		flightServer = args[0];
		flightPort = Integer.parseInt(args[1]);
		carServer = args[2];
		carPort = Integer.parseInt(args[3]);
		roomServer = args[4];
		roomPort = Integer.parseInt(args[5]);
		rmiPort = Integer.parseInt(args[6]);

		try {
			// get a reference to the flight rmiregistry
			Registry registry = LocateRegistry.getRegistry(flightServer,
					flightPort);
			// get the proxy and the remote reference by rmiregistry lookup
			flightRM = (ResourceManager) registry
					.lookup("Group1ResourceManager");
			if (flightRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Flight RM");
			} else {
				System.out.println("Unsuccessful");
			}

			// get a reference to the car rmiregistry
			registry = LocateRegistry.getRegistry(carServer, carPort);
			// get the proxy and the remote reference by rmiregistry lookup
			carRM = (ResourceManager) registry.lookup("Group1ResourceManager");
			if (carRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Car RM");
			} else {
				System.out.println("Unsuccessful");
			}

			// get a reference to the room rmiregistry
			registry = LocateRegistry.getRegistry(roomServer, roomPort);
			// get the proxy and the remote reference by rmiregistry lookup
			roomRM = (ResourceManager) registry.lookup("Group1ResourceManager");
			if (roomRM != null) {
				System.out.println("Successful");
				System.out.println("Connected to Room RM");
			} else {
				System.out.println("Unsuccessful");
			}

			// At this point we are a client to the three RM servers.
			// Now, establish server connection to serve client(s).
			Middleware obj = new Middleware();
			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject
					.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			registry = LocateRegistry.getRegistry(rmiPort);
			registry.rebind("Group1ResourceManager", rm);

			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("Middleware exception: " + e.toString());
			e.printStackTrace();
		}

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
	}	
	
	// Reads a data item
	private RMItem readData(int id, String key) {
		synchronized (m_itemHT) {
			return (RMItem) m_itemHT.get(key);
		}
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value) {
		synchronized (m_itemHT) {
			m_itemHT.put(key, value);
		}
	}

	// Remove the item out of storage
	protected RMItem removeData(int id, String key) {
		synchronized (m_itemHT) {
			return (RMItem) m_itemHT.remove(key);
		}
	}

	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws RemoteException {
		synchronized (flightRM) {
			return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
		}
	}

	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException {
		synchronized (carRM) {
			return carRM.addCars(id, location, numCars, price);
		}
	}

	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException {
		synchronized (roomRM) {
			return roomRM.addRooms(id, location, numRooms, price);
		}
	}

	public int newCustomer(int id) throws RemoteException {
		Trace.info("INFO: RM::newCustomer(" + id + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(id)
				+ String.valueOf(Calendar.getInstance().get(
						Calendar.MILLISECOND))
				+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer cust = new Customer(cid);
		writeData(id, cust.getKey(), cust);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int id, int customerID) throws RemoteException {
		Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
				+ ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			cust = new Customer(customerID);
			writeData(id, cust.getKey(), cust);
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
					+ ") created a new customer");
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
					+ ") failed--customer already exists");
			return false;
		} // else
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		synchronized (flightRM) {
			return flightRM.deleteFlight(id, flightNum);
		}
	}

	public boolean deleteCars(int id, String location) throws RemoteException {
		synchronized (carRM) {
			return carRM.deleteCars(id, location);
		}
	}

	public boolean deleteRooms(int id, String location) throws RemoteException {
		synchronized (roomRM) {
			return roomRM.deleteRooms(id, location);
		}
	}

	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException {
		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::deleteCustomer(" + id + ", " + customerID
					+ ") failed--customer doesn't exist");
			return false;
		} else {
			// Increase the reserved numbers of all reservable items which the
			// customer reserved.
			RMHashtable reservationHT = cust.getReservations();
			for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {
				String reservedkey = (String) (e.nextElement());
				ReservedItem reserveditem = cust.getReservedItem(reservedkey);
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID
						+ ") has reserved " + reserveditem.getKey() + " "
						+ reserveditem.getCount() + " times");

				String key = reserveditem.getKey();
				String type = key.split("-")[0];
				System.out.println(type);
				
				if(type.equals("car")){
					carRM.removeReservations(id, key, reserveditem.getCount());
				}else if(type.equals("room")){
					roomRM.removeReservations(id, key, reserveditem.getCount());
				}else if(type.equals("flight")){
					flightRM.removeReservations(id, key, reserveditem.getCount());
				}else{
					carRM.removeReservations(id, key, reserveditem.getCount());
					roomRM.removeReservations(id, key, reserveditem.getCount());
					flightRM.removeReservations(id, key, reserveditem.getCount());
				}
			}

			// remove the customer from the storage
			removeData(id, cust.getKey());

			Trace.info("RM::deleteCustomer(" + id + ", " + customerID
					+ ") succeeded");
			return true;
		} // if
	}

	public int queryFlight(int id, int flightNumber) throws RemoteException {
		synchronized (flightRM) {
			return flightRM.queryFlight(id, flightNumber);
		}
	}

	public int queryCars(int id, String location) throws RemoteException {
		synchronized (carRM) {
			return carRM.queryCars(id, location);
		}
	}

	public int queryRooms(int id, String location) throws RemoteException {
		synchronized (roomRM) {
			return roomRM.queryRooms(id, location);
		}
	}

	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException {
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
				+ ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID
					+ ") failed--customer doesn't exist");
			return ""; // NOTE: don't change this--WC counts on this value
						// indicating a customer does not exist...
		} else {
			String s = cust.printBill();
			Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
					+ "), bill follows...");
			System.out.println(s);
			return s;
		}
	}

	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException {
		synchronized (flightRM) {
			return flightRM.queryFlightPrice(id, flightNumber);
		}
	}

	public int queryCarsPrice(int id, String location) throws RemoteException {
		synchronized (carRM) {
			return carRM.queryCarsPrice(id, location);
		}
	}

	public int queryRoomsPrice(int id, String location) throws RemoteException {
		synchronized (roomRM) {
			return roomRM.queryRoomsPrice(id, location);
		}
	}

	public boolean reserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException {
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::reserveFlight( " + id + ", " + customerID + ", "
					+ flightNumber + ")  failed--customer doesn't exist");
			return false;
		}
		synchronized (flightRM) {
			int price = flightRM.queryFlightPrice(id, flightNumber);
			if (flightRM.reserveFlight(id, customerID, flightNumber)) {
				// Item was successfully marked reserved by RM
				cust.reserve(Flight.getKey(flightNumber),
						String.valueOf(flightNumber), price);
				writeData(id, cust.getKey(), cust);
				return true;
			} // else
			Trace.warn("MW::reserveFlight( " + id + ", " + customerID + ", "
					+ flightNumber + ")  failed--RM returned false");
			return false;
		}
	}

	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException {
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", "
					+ location + ")  failed--customer doesn't exist");
			return false;
		}
		synchronized (carRM) {
			int price = carRM.queryCarsPrice(id, location);
			if (carRM.reserveCar(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				cust.reserve(Car.getKey(location), location, price);
				writeData(id, cust.getKey(), cust);
				return true;
			} // else
			Trace.warn("MW::reserveCar( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		}
	}

	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException {
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null) {
			Trace.warn("RM::reserveRoom( " + id + ", " + customerID + ", "
					+ location + ")  failed--customer doesn't exist");
			return false;
		}
		synchronized (roomRM) {
			int price = roomRM.queryRoomsPrice(id, location);
			if (roomRM.reserveRoom(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				cust.reserve(Hotel.getKey(location), location, price);
				writeData(id, cust.getKey(), cust);
				return true;
			} // else
			Trace.warn("MW::reserveRoom( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		}
	}

	/**
	 * Note: items are reserved in this order: flight1, ..., flight n, car,
	 * room. If any one of these fails, all previous reservations along the
	 * chain are left as is. I.e. nothing is undone upon failure.
	 * 
	 * @see ResInterface.ResourceManager#reserveItinerary(int, int,
	 *      java.util.Vector, java.lang.String, boolean, boolean)
	 */
	public boolean reserveItinerary(int id, int customer, Vector flightNumbers,
			String location, boolean car, boolean room) throws RemoteException {

		Trace.info("MW::reserveItinerary(" + id + ", " + customer + ", "
				+ flightNumbers + ", " + location + ", " + car + ", " + room
				+ ") was called");

		// Book the flights.
		for (Object flightNumber : flightNumbers) {
			// Ugly cast... not *my* decision to design things this way, lol
			if (!reserveFlight(id, customer,
					Integer.parseInt((String) flightNumber))) {
				Trace.warn("MW::reserveItinerary failed, could not reserve flight "
						+ flightNumber);
				return false; // Flight couldn't be reserved
			} else {
				Trace.info("MW::reserveItinerary:: reserved flight "
						+ flightNumber);
			}
		}

		if (car) {
			// Try to reserve the car
			if (!reserveCar(id, customer, location)) {
				Trace.warn("MW::reserveItinerary failed, could not reserve car at location "
						+ location);
				return false;
			} else {
				Trace.info("MW::reserveItinerary:: reserved car at location "
						+ location);
			}
		}

		if (room) {
			// Try to reserve the room
			if (!reserveRoom(id, customer, location)) {
				Trace.warn("MW::reserveItinerary failed, could not reserve room at location "
						+ location);
				return false;
			} else {
				Trace.info("MW::reserveItinerary:: reserved room at location "
						+ location);
			}
		}

		// Everything worked!
		return true;
	}

	public boolean removeReservations(int id, String key, int count)
			throws RemoteException {
		// We never have to do this!
		return false;
	}
	
	//Creating new Start, commit and abort methods here
	//Start 
	public int start(){
		int newTID = -1;
		
		synchronized (t_manager){
			//Get a brand new fresh id for this newly created transaction
			newTID = t_manager.getNewTransactionId();
		}
		
		//Now create an entry in this transaction records hash table
		synchronized (t_records){
			t_records.put(newTID, new HashMap<String, RMItem>());
		}
		
		System.out.println("Created new transaction with Transaction ID = "+newTID+".");
		
		//Now call the start function on each of the rms to initialize they transaction records
		try{
			roomRM.start(newTID);
			carRM.start(newTID);
			flightRM.start(newTID);
		}catch(RemoteException e){
			//Shouldnt happen ... why is prof asking us to throw this?
			System.out.println(e.toString());
		}
		
		return newTID;//Give the customer its requestion xid
	}

	/* (non-Javadoc)
	 * @see ResInterface.ResourceManager#commit(int)
	 */
	@Override
	public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		//Start by removing the hashtable entry for this transaction ID inside the transaction record
		Object removedObject = t_records.remove(xid);
		if(removedObject==null) return false;//if there was no hash table fho dis transaction ID
		
		
		//Now unlock all locks related to the xid
		//TODO: something like: lockManager.unlockAll(xid); //--> does not need to be synchronized, since unlockAll method takes care of that
					

		try{
			if(!carRM.commit(xid) || !roomRM.commit(xid) || !flightRM.commit(xid))
				return false;
		}catch(Exception e){
			//Should not go there, but just in case
			System.out.println("One RM sent an exception when trying to commit.");
			return false;
		}
		
		
		return true;
	}

	/* (non-Javadoc)
	 * @see ResInterface.ResourceManager#abort(int)
	 */
	public void abort(int xid) throws RemoteException, InvalidTransactionException {
		//Start by removing the hashtable entry for this transaction ID inside the transaction records table
		HashMap<String, RMItem> r_table = t_records.remove(xid);
		
		if(r_table != null)
		{
			Set<String> keys = r_table.keySet();
			RMItem tmp_item;
			
			//Loop through all elements in the removed hash table and reset their original value inside the RM's hash table
			for (String key : keys){
				tmp_item = (RMItem)r_table.get(key);
				
				if(tmp_item==null)
				{
					//We need to remove this item from the RM's hash table
					m_itemHT.remove(key);
				}else{
					//We need to add this item to this RM's hash table
					m_itemHT.put(key, (RMItem)tmp_item);
				}
			}
		}
		
		
		//Now unlock all locks related to the xid
		//TODO: something like: lockManager.unlockAll(xid); //--> does not need to be synchronized, since unlockAll method takes care of that
		
		
		//Now call abort on each of the RMs
		try{
			
		}catch(Exception e){//What do we do
		}
	}

	/* (non-Javadoc)
	 * @see ResInterface.ResourceManager#shutdown()
	 */
	public boolean shutdown() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	
	//USELESS METHODS
	public void start(int xid) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

}

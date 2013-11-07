import java.rmi.NotBoundException;
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
import LockManager.DeadlockException;
import LockManager.LockManager;
import ResImpl.Car;
import ResImpl.Customer;
import ResImpl.DeepCopy;
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

	// Create a transaction hashtable to store transaction data --> this will be
	// xid -> (key -> old value)
	protected Hashtable<Integer, HashMap<String, RMItem>> t_records = new Hashtable<Integer, HashMap<String, RMItem>>();	
	
	//Our transaction manager
	TransactionManager t_manager;


	LockManager lockManager = new LockManager();

	static String flightServer, carServer, roomServer;
	static int flightPort, carPort, roomPort, rmiPort;

	public Middleware() throws RemoteException {
		t_manager = new TransactionManager(this, carRM, roomRM, flightRM);
		System.out.println("Created tm");
	}

	public static void main(String args[]) {

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

	private void record(int xid, String key, RMItem newItem) {
		// Get the record for this txn
		HashMap<String, RMItem> record;
		synchronized(t_records) {
			record = t_records.get(xid);
		}
		if (record == null) {
			// We are trying to update the record of a nonexistent txn!
			System.err.println("No record for this transaction " + xid);
			return;
		}
		if (record.containsKey(key)) {
			System.err.println("Already have a record for this operation "
					+ key);
			return;
		}
		// Haven't recorded this operation yet
		RMItem pastItem = (RMItem) m_itemHT.get(key);
		if (pastItem == null && newItem == null) {
			// We're trying to read or delete an item that's not even there
			return;
		}
		if (pastItem == null) {
			// We're trying to write a new item
			record.put(key, null);
		} else {
			// We're reading, deleting, or overwriting an item
			RMItem copy = (RMItem) DeepCopy.copy(pastItem);
			if (copy == null) {
				System.err.println("Couldn't copy this item -- not serializable");
				return;
			}
			System.out.println("Recording past item " + copy + " - "
					+ copy.hashCode());
			System.out.println("The new item is ging to be " + newItem + " - "
					+ newItem.hashCode());
			record.put(key, copy);
		}
	}

	// Reads a data item
	private RMItem readData(int id, String key) throws DeadlockException {
		boolean lock = false;
		RMItem item = null;

		// Request a write lock
		synchronized (lockManager) {
			lock = lockManager.Lock(id, key, LockManager.READ);
		}

		if (lock) {
			System.out.println("Got a read lock for txn id " + id);
			//this.record(id, key, null);
			item = (RMItem) m_itemHT.get(key);
		}

		// TODO what happens if we don't get a lock?
		return item;
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value)
			throws DeadlockException {
		boolean lock = false;

		// Request a write lock
		synchronized (lockManager) {
			lock = lockManager.Lock(id, key, LockManager.WRITE);
		}
		if (lock) {
			System.out.println("Got a write lock for txn id " + id);
			this.record(id, key, value);
			synchronized (m_itemHT) {
				m_itemHT.put(key, value);
			}
		}
	}

	// Remove the item out of storage
	protected RMItem removeData(int id, String key) throws DeadlockException {
		boolean lock = false;
		RMItem item = null;

		// Request a write lock
		synchronized (lockManager) {
			lock = lockManager.Lock(id, key, LockManager.WRITE);
		}
		if (lock) {
			System.out.println("Got a write lock for txn id " + id);
			this.record(id, key, null);
			synchronized (m_itemHT) {
				item = (RMItem) m_itemHT.remove(key);
			}
		}
		// TODO what happens if we don't get a lock?
		return item;
	}

	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.addCars(id, location, numCars, price);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.addRooms(id, location, numRooms, price);
		} catch (DeadlockException e) {
			// TODO abort
		}
		return false;
	}

	public int newCustomer(int id) throws RemoteException {
		Trace.info("INFO: RM::newCustomer(" + id + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(id)
				+ String.valueOf(Calendar.getInstance().get(
						Calendar.MILLISECOND))
				+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer cust = new Customer(cid);
		try {
			t_manager.enlist(id, TransactionManager.CUSTOMER);
			writeData(id, cust.getKey(), cust);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int id, int customerID) throws RemoteException {
		Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
				+ ") called");
		try {
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				cust = new Customer(customerID);
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				writeData(id, cust.getKey(), cust);
				Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
						+ ") created a new customer");
				return true;
			} else {
				Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
						+ ") failed--customer already exists");
				return false;
			}
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.deleteFlight(id, flightNum);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean deleteCars(int id, String location) throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.deleteCars(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean deleteRooms(int id, String location) throws RemoteException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.deleteRooms(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException {
		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
		try {
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::deleteCustomer(" + id + ", " + customerID
						+ ") failed--customer doesn't exist");
				return false;
			} else {
				// Increase the reserved numbers of all reservable items which
				// the
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
					if (type.equals("car")) {
						t_manager.enlist(id, TransactionManager.CAR);
						carRM.removeReservations(id, key,
								reserveditem.getCount());
					} else if (type.equals("room")) {
						t_manager.enlist(id, TransactionManager.ROOM);
						roomRM.removeReservations(id, key,
								reserveditem.getCount());
					} else if (type.equals("flight")) {
						t_manager.enlist(id, TransactionManager.FLIGHT);
						flightRM.removeReservations(id, key,
								reserveditem.getCount());
					} else {
						t_manager.enlist(id, TransactionManager.CAR);
						carRM.removeReservations(id, key,
								reserveditem.getCount());
						t_manager.enlist(id, TransactionManager.ROOM);
						roomRM.removeReservations(id, key,
								reserveditem.getCount());
						t_manager.enlist(id, TransactionManager.FLIGHT);
						flightRM.removeReservations(id, key,
								reserveditem.getCount());
					}

				}

				// remove the customer from the storage
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				removeData(id, cust.getKey());

				Trace.info("RM::deleteCustomer(" + id + ", " + customerID
						+ ") succeeded");
				return true;
			}
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public int queryFlight(int id, int flightNumber) throws RemoteException {
		try {
			return flightRM.queryFlight(id, flightNumber);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public int queryCars(int id, String location) throws RemoteException {
		try {
			return carRM.queryCars(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public int queryRooms(int id, String location) throws RemoteException {
		try {
			return roomRM.queryRooms(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException {
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
				+ ") called");
		try {
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
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return "";
	}

	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException {
		try {
			return flightRM.queryFlightPrice(id, flightNumber);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public int queryCarsPrice(int id, String location) throws RemoteException {
		try {
			return carRM.queryCarsPrice(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public int queryRoomsPrice(int id, String location) throws RemoteException {
		try {
			return roomRM.queryRoomsPrice(id, location);
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return 0;
	}

	public boolean reserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException {
		try {
			// Read customer object if it exists (and read lock it)
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveFlight( " + id + ", " + customerID
						+ ", " + flightNumber
						+ ")  failed--customer doesn't exist");
				return false;
			}
			int price = flightRM.queryFlightPrice(id, flightNumber);
			t_manager.enlist(id, TransactionManager.FLIGHT);
			if (flightRM.reserveFlight(id, customerID, flightNumber)) {
				// Item was successfully marked reserved by RM
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				cust.reserve(Flight.getKey(flightNumber),
						String.valueOf(flightNumber), price);
				writeData(id, cust.getKey(), cust);
				return true;
			}

			Trace.warn("MW::reserveFlight( " + id + ", " + customerID + ", "
					+ flightNumber + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException {
		try {
			// Read customer object if it exists (and read lock it)
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", "
						+ location + ")  failed--customer doesn't exist");
				return false;
			}
			int price = carRM.queryCarsPrice(id, location);
			t_manager.enlist(id, TransactionManager.CAR);
			if (carRM.reserveCar(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				cust.reserve(Car.getKey(location), location, price);
				writeData(id, cust.getKey(), cust);
				return true;
			}

			Trace.warn("MW::reserveCar( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
	}

	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException {
		try {
			// Read customer object if it exists (and read lock it)
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveRoom( " + id + ", " + customerID + ", "
						+ location + ")  failed--customer doesn't exist");
				return false;
			}
			int price = roomRM.queryRoomsPrice(id, location);
			t_manager.enlist(id, TransactionManager.ROOM);
			if (roomRM.reserveRoom(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				cust.reserve(Hotel.getKey(location), location, price);
				writeData(id, cust.getKey(), cust);
				return true;
			}

			Trace.warn("MW::reserveRoom( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			// TODO Abort
		}
		return false;
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

	// Creating new Start, commit and abort methods here
	// Start
	public int start() {
		int newTID = -1;
		
		synchronized (t_manager){
			//Get a brand new fresh id for this newly created transaction
			newTID = t_manager.start();
		}
		
		return newTID;//Give the customer its requestion xid
	}
	
	//If the TM tells us to add the transaction to
	public void start(int xid) throws RemoteException {
		//First test if an entry in the Hash table already exists:
		if(t_records.get(xid) != null){
			System.out.println("A hash table record already exists in customer RM for transaction ID: "+xid);
			return;
		}
		
		//Now create an entry in this transaction records hash table
		synchronized (t_records){
			t_records.put(xid, new HashMap<String, RMItem>());
			System.out.println("Customer RM has successfully create a hash map entry for TID: "+xid);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ResInterface.ResourceManager#commit(int)
	 */
	@Override
	public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		//Start by calling the commit function in the Transaction Manager
		//This tells us whether we have to execute commit on the customer hash table
		try{
			if(t_manager.commit(xid)){
			
				Object removedObject = t_records.remove(xid);
				if(removedObject==null){
					System.out.println("TID: "+ xid + " has no hashtable entry in customer RM.");
					return false;//if there was no hash table fho dis transaction ID
				}
				
				System.out.println("Successfully found and removed hash table TID: "+ xid + " from customer RM.");
				
				//Now unlock all locks related to the xid
				//TODO: something like: lockManager.unlockAll(xid); //--> does not need to be synchronized, since unlockAll method takes care of that
			}
		}catch(InvalidTransactionException e){
			System.out.println(e.toString());
			return false;
		}
		System.out.println("Successfully found and removed hash table TID: "+ xid + " from room/flight/car RMs.");
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ResInterface.ResourceManager#abort(int)
	 */
	public void abort(int xid) throws RemoteException, InvalidTransactionException {
		//Call TM abort to abort rms, and to tell this function if customer also has to abort
		try{
			if(t_manager.abort(xid)){
				
				System.out.println("Attempting to abort transaction "+ xid+ " from customer RM.");
				
				HashMap<String, RMItem> r_table = t_records.remove(xid);
				
				if(r_table == null)	{
					System.out.println("TID: "+ xid + " has no hashtable entry in RM.");
				}else{
					System.out.println(r_table.size() + " entries have been found in the hash table");
					
					Set<String> keys = r_table.keySet();
					RMItem tmp_item;
					
					synchronized(m_itemHT){
						//Loop through all elements in the removed hash table and reset their original value inside the RM's hash table
						for (String key : keys){
							tmp_item = (RMItem)r_table.get(key);
							
							if(tmp_item==null)
							{
								System.out.println("We need to remove  element-key: "+key+" item from the RM's hash table.");
								//We need to remove this item from the RM's hash table
								m_itemHT.remove(key);
							}else{
								System.out.println("We need to add  element-key: "+key+" item to the RM's hash table.");
								//We need to add this item to this RM's hash table
								m_itemHT.put(key, (RMItem)tmp_item);
							}
						}
					}
					
					//Now unlock all locks related to the xid
					//TODO: something like: lockManager.unlockAll(xid); 
					//--> does not need to be synchronized, since unlockAll method takes care of that
					
				}
				
				System.out.println("Successfully aborted aborted transaction "+xid+" from customer RM.");
	
			}
		}catch(InvalidTransactionException e){
			System.out.println(e.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ResInterface.ResourceManager#shutdown()
	 */
	public boolean shutdown() throws RemoteException {
		boolean success = false;
		success = flightRM.shutdown() && roomRM.shutdown() && carRM.shutdown();

		UnicastRemoteObject.unexportObject(this, true);
		// Unbind self from the registry
		Registry registry = LocateRegistry.getRegistry(rmiPort);
		try {
			registry.unbind("Group1ResourceManager");
		} catch (NotBoundException e) {
			System.out.println("Couldn't unbind self from registry");
			return false;
		}

		// Kill process in separate thread in order to let method return
		new Thread() {
			@Override
			public void run() {
				System.out.print("Shutting down...");
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					// I don't care
				}
				System.out.println("done");
				System.exit(0);
			}
		}.start();

		return success;
	}
}

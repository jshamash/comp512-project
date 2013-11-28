package middleware;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import tools.Constants;
import tools.DeepCopy;
import tools.Serializer;
import transaction.InvalidTransactionException;
import transaction.TransactionAbortedException;
import transaction.TransactionManager;
import transaction.TransactionMonitor;
import LockManager.DeadlockException;
import LockManager.LockManager;
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

	// Create a transaction hashtable to store transaction data --> this will be
	// xid -> (key -> old value)
	protected Hashtable<Integer, HashMap<String, RMItem>> t_records = new Hashtable<Integer, HashMap<String, RMItem>>();

	TransactionManager t_manager;
	LockManager lockManager = new LockManager();
	
	private String ptr_filename;
	private String ser_master;

	static String flightServer, carServer, roomServer;
	static int flightPort, carPort, roomPort, rmiPort;

	public Middleware() throws RemoteException {
		try {
			// Read in serialized txn monitor
			t_manager = (TransactionManager) Serializer.deserialize(Constants.TRANSACTION_MANAGER_FILE);
		} catch (FileNotFoundException e) {
			// No txn manager has been serialized yet so create a new one.
			t_manager = new TransactionManager(this, carRM, roomRM, flightRM);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			
			// Initialize the RMs
			//obj.initialize(Constants.CUSTOMER_FILE_PTR);
			
			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

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
		synchronized (t_records) {
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
				System.err
						.println("Couldn't copy this item -- not serializable");
				return;
			}
			System.out.println("Recording past item " + copy + " - "
					+ copy.hashCode());
			if (newItem != null) {
				System.out.println("The new item is going to be " + newItem
						+ " - " + newItem.hashCode());
			}
			record.put(key, copy);
		}
	}

	// Reads a data item
	private RMItem readData(int id, String key) throws DeadlockException {
		boolean lock = false;
		RMItem item = null;

		// Request a write lock
		//TODO May need to replace synchronized block
		lock = lockManager.Lock(id, key, LockManager.READ);

		if (lock) {
			System.out.println("Got a read lock for txn id " + id);
			// this.record(id, key, null);
			item = (RMItem) m_itemHT.get(key);
		}

		// TODO what happens if we don't get a lock?
		return item;
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value)
			throws DeadlockException {
		boolean lock = false;

		//TODO May need to replace synchronized block
		lock = lockManager.Lock(id, key, LockManager.WRITE);

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
		//TODO May need to replace synchronized block
		lock = lockManager.Lock(id, key, LockManager.WRITE);
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
			int flightPrice) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.addCars(id, location, numCars, price);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.addRooms(id, location, numRooms, price);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public int newCustomer(int id) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
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
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int id, int customerID) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
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
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.deleteFlight(id, flightNum);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean deleteCars(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.deleteCars(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean deleteRooms(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.deleteRooms(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
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
					ReservedItem reserveditem = cust
							.getReservedItem(reservedkey);
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
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return false;
	}

	public int queryFlight(int id, int flightNumber) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.queryFlight(id, flightNumber);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public int queryCars(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.queryCars(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public int queryRooms(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.queryRooms(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
				+ ") called");
		try {
			t_manager.enlist(id, TransactionManager.CUSTOMER);
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
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return "";
	}

	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.FLIGHT);
			return flightRM.queryFlightPrice(id, flightNumber);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public int queryCarsPrice(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CAR);
			return carRM.queryCarsPrice(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public int queryRoomsPrice(int id, String location) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.ROOM);
			return roomRM.queryRoomsPrice(id, location);
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
			}
		}
		return 0;
	}

	public boolean reserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionException {
		try {
			// Read customer object if it exists (and read lock it)
			t_manager.enlist(id, TransactionManager.CUSTOMER);
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveFlight( " + id + ", " + customerID
						+ ", " + flightNumber
						+ ")  failed--customer doesn't exist");
				return false;
			}
			t_manager.enlist(id, TransactionManager.FLIGHT);
			int price = flightRM.queryFlightPrice(id, flightNumber);
			if (flightRM.reserveFlight(id, customerID, flightNumber)) {
				// Item was successfully marked reserved by RM
				t_manager.enlist(id, TransactionManager.CUSTOMER);
				Customer updatedCust = (Customer) DeepCopy.copy(cust);
				updatedCust.reserve(Flight.getKey(flightNumber),
						String.valueOf(flightNumber), price);
				writeData(id, cust.getKey(), updatedCust);
				return true;
			}

			Trace.warn("MW::reserveFlight( " + id + ", " + customerID + ", "
					+ flightNumber + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
				throw new InvalidTransactionException(id, e1.getMessage());
			}
		}
	}

	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CUSTOMER);
			// Read customer object if it exists (and read lock it)
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", "
						+ location + ")  failed--customer doesn't exist");
				return false;
			}
			t_manager.enlist(id, TransactionManager.CAR);
			int price = carRM.queryCarsPrice(id, location);
			if (carRM.reserveCar(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				Customer updatedCust = (Customer) DeepCopy.copy(cust);
				updatedCust.reserve(Car.getKey(location), location, price);
				writeData(id, cust.getKey(), updatedCust);
				return true;
			}

			Trace.warn("MW::reserveCar( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
				throw new InvalidTransactionException(id, e1.getMessage());
			}
		}
	}

	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException, TransactionAbortedException,
			InvalidTransactionException {
		try {
			t_manager.enlist(id, TransactionManager.CUSTOMER);
			// Read customer object if it exists (and read lock it)
			Customer cust = (Customer) readData(id, Customer.getKey(customerID));
			if (cust == null) {
				Trace.warn("RM::reserveRoom( " + id + ", " + customerID + ", "
						+ location + ")  failed--customer doesn't exist");
				return false;
			}
			t_manager.enlist(id, TransactionManager.ROOM);
			int price = roomRM.queryRoomsPrice(id, location);
			if (roomRM.reserveRoom(id, customerID, location)) {
				// Item was successfully marked reserved by RM
				Customer updatedCust = (Customer) DeepCopy.copy(cust);
				updatedCust.reserve(Hotel.getKey(location), location, price);
				writeData(id, cust.getKey(), updatedCust);
				return true;
			}

			Trace.warn("MW::reserveRoom( " + id + ", " + customerID + ", "
					+ location + ")  failed--RM returned false");
			return false;
		} catch (DeadlockException e) {
			try {
				this.abort(id);
				throw new TransactionAbortedException(id,
						"Deadlock - the transaction was aborted");
			} catch (InvalidTransactionException e1) {
				System.err.println("Invalid transaction: " + id);
				throw new InvalidTransactionException(id, e1.getMessage());
			}
		}
	}

	/**
	 * Note: items are reserved in this order: flight1, ..., flight n, car,
	 * room. If any one of these fails, all previous reservations along the
	 * chain are left as is. I.e. nothing is undone upon failure.
	 * 
	 * @throws TransactionAbortedException
	 * @throws NumberFormatException
	 * @throws InvalidTransactionException
	 * 
	 * @see ResInterface.ResourceManager#reserveItinerary(int, int,
	 *      java.util.Vector, java.lang.String, boolean, boolean)
	 */
	public boolean reserveItinerary(int id, int customer, Vector flightNumbers,
			String location, boolean car, boolean room) throws RemoteException,
			NumberFormatException, TransactionAbortedException,
			InvalidTransactionException {

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
	public int start() throws RemoteException{
		int newTID = -1;

		synchronized (t_manager) {
			// Get a brand new fresh id for this newly created transaction
			newTID = t_manager.start();
		}

		return newTID;// Give the customer its requestion xid
	}

	// If the TM tells us to add the transaction to
	public void start(int xid) throws RemoteException {
		// First test if an entry in the Hash table already exists:
		if (t_records.get(xid) != null) {
			System.out
					.println("A hash table record already exists in customer RM for transaction ID: "
							+ xid);
			return;
		}

		// Now create an entry in this transaction records hash table
		synchronized (t_records) {
			t_records.put(xid, new HashMap<String, RMItem>());
			System.out
					.println("Customer RM has successfully create a hash map entry for TID: "
							+ xid);
		}
	}
	
	public boolean prepare(int transactionID) {
		//TODO throw the exceptions
		
		//TODO: timeout should be irrelevant here? Since we are in the same process so messages will get through for sure?
		
		// Begin by storing all committed data into a file
		// Write to the non-master file
		String writeFile = Constants.getInverse(ser_master);
		try {
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(writeFile)));
			out.writeObject(m_itemHT);
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			// This should never happen
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ResInterface.ResourceManager#commit(int)
	 */
	@Override
	public boolean commit(int xid) throws RemoteException,
			InvalidTransactionException {
		
		return t_manager.commit(xid);
		
//		//First phase of 2PC
//		if(!t_manager.prepare(xid)){
//			abort(xid);
//			return false;
//		}
//		
//		//Try to commit using 2PC
//		if(t_manager.commit(xid)){
//			System.out.println("Successfully committed TID: "+ xid);
//			return true;
//		}
//		
//		System.out.println("Unsuccessfully committed TID: "+ xid);
//		return false;
	}
	
	//Special function specific to the middleware to handle customer commit
	public boolean middlewareCommit(int xid) throws RemoteException, InvalidTransactionException {

		Object removedObject = t_records.remove(xid);
		if (removedObject == null) {
			System.out.println("TID: " + xid
					+ " has no hashtable entry in customer RM.");
			return false;// if there was no hash table fho dis transaction
							// ID
		}
	
		System.out.println("Successfully found and removed hash table TID: " + xid + " from customer RM.");
	
		// Now unlock all locks related to the xid
		lockManager.UnlockAll(xid); // --> does not need to be synchronized,
									// since unlockAll method takes care of
									// that
		
		System.out.println("Successfully found and removed hash table TID: "
				+ xid + " from customer RM.");
		
		// Do the ol' switcheroo, indicating which persistent copy is up-to-date
		String newMaster = Constants.getInverse(ser_master);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(ptr_filename));
			out.write(newMaster);
			//TODO do we write the txn id too?
			out.close();
			ser_master = newMaster;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ResInterface.ResourceManager#abort(int)
	 */
	public void abort(int xid) throws RemoteException,
			InvalidTransactionException {		
		// Tell TM to attempt abort
		t_manager.abort(xid);
	}
	
	//Special function specific to the middleware to handle customer abort
	public void middlewareAbort(int xid) throws RemoteException,
	InvalidTransactionException {
		// Call TM abort to abort rms, and to tell this function if customer
		// also has to abort
		System.out.println("Attempting to abort transaction " + xid
				+ " from customer RM.");

		HashMap<String, RMItem> r_table = t_records.remove(xid);

		if (r_table == null) {
			System.out.println("TID: " + xid
					+ " has no hashtable entry in RM.");
		} else {
			System.out.println(r_table.size()
					+ " entries have been found in the hash table");

			Set<String> keys = r_table.keySet();
			RMItem tmp_item;

			synchronized (m_itemHT) {
				// Loop through all elements in the removed hash table
				// and reset their original value inside the RM's hash
				// table
				for (String key : keys) {
					tmp_item = (RMItem) r_table.get(key);

					if (tmp_item == null) {
						System.out
								.println("We need to remove  element-key: "
										+ key
										+ " item from the RM's hash table.");
						// We need to remove this item from the RM's
						// hash table
						m_itemHT.remove(key);
					} else {
						System.out
								.println("We need to add  element-key: "
										+ key
										+ " item to the RM's hash table.");
						// We need to add this item to this RM's hash
						// table
						m_itemHT.put(key, (RMItem) tmp_item);
					}
				}
			}

			// Now unlock all locks related to the xid
			lockManager.UnlockAll(xid);

		}

		System.out.println("Successfully aborted aborted transaction "
				+ xid + " from customer RM.");
	}

	public boolean firstPhaseACK(int xid) throws RemoteException{
		//TODO: Start Timer --> use of thread to start timer and give 100 seconds to receive commit
		
		//For now we will only assume that this firstTimeACK always returns true
		
		return true;
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
	
	//This method is much more complex than it actually looks like. Before the beginning of times servers have never existed.
	//Now this new terror arises and a hero can call this function to undo the evils done by these malicious servers. From the 
	//age of magic to the age of robotics, we have yet to find the very answer for completely destroying and preventing servers
	//from terrorising little children....Until now, a hero has arised who built this very function, permitting any client from
	//crashing these evil creatures in a powerful manner. But such power might be too much to handle for the human race, and
	//there is only one thing we can do... : "Never gonna give you up, never gonna ..."
	public boolean crash(String which) throws RemoteException
	{
		//Tells which RM crashes
		if(which.equals("customer")){
			//Tricky piece of shizzle mah dizzling broatha!
			//Yezzzzz :D!
			
			shutdown();
		}else if(which.equals("flight")){
			if(!flightRM.shutdown()) return false;
		}else if(which.equals("car")){
			if(!carRM.shutdown()) return false;
		}else if(which.equals("room"))
		{
			if(!roomRM.shutdown()) return false;
		}
		
		return true;
	}

	public void dump() throws RemoteException {
		m_itemHT.dump();
		carRM.dump();
		roomRM.dump();
		flightRM.dump();
	}

	@Override
	public void initialize(String ptr_filename) throws RemoteException {
		
		this.ptr_filename = ptr_filename;
		
		// Initialize RMs
		flightRM.initialize(Constants.FLIGHT_FILE_PTR);
		carRM.initialize(Constants.CAR_FILE_PTR);
		roomRM.initialize(Constants.ROOM_FILE_PTR);

		try {
			// Get location of master file
			BufferedReader in = new BufferedReader(new FileReader(ptr_filename));
			ser_master = in.readLine();
			System.out.println("Serialized master is " + ser_master);
			in.close();
		} catch (FileNotFoundException e1) {
			// No pointer file yet, so create one that points to <customers file 1>.
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(ptr_filename));
				out.write(Constants.CUSTOMER_FILE_1);
				out.close();
				System.out.println("Created pointer file to point to " + Constants.CUSTOMER_FILE_1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			// Initialize the hashtable with the contents of ser_master
			ObjectInputStream inObj = new ObjectInputStream(new BufferedInputStream(new FileInputStream(ser_master)));
			m_itemHT = (RMHashtable) inObj.readObject();
			inObj.close();
		} catch (FileNotFoundException e) {
			// hashtable has never been serialized... so it will be initialized as empty.
			System.out.println("No existing hashtable, initializing empty.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}

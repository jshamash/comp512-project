import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;

import ResImpl.Car;
import ResImpl.Customer;
import ResImpl.Flight;
import ResImpl.Hotel;
import ResImpl.RMHashtable;
import ResImpl.RMItem;
import ResImpl.ReservableItem;
import ResImpl.ReservedItem;
import ResImpl.ResourceManagerImpl;
import ResImpl.Trace;
import ResInterface.ResourceManager;

public class Middleware implements ResourceManager {

	static ResourceManager flightRM = null;
	static ResourceManager carRM = null;
	static ResourceManager roomRM = null;

	protected RMHashtable m_itemHT = new RMHashtable();

	public Middleware() throws RemoteException {
	}

	public static void main(String args[]) {
		String flightServer, carServer, roomServer;
		int flightPort, carPort, roomPort, rmiPort;

		if (args.length != 7) {
			System.err.println("Wrong usage");
			System.out
					.println("Usage: java ResImpl.ResourceManagerImpl [flightServer] [flightPort] "
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
				ReservableItem item = (ReservableItem) readData(id,
						reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID
						+ ") has reserved " + reserveditem.getKey()
						+ "which is reserved" + item.getReserved()
						+ " times and is still available " + item.getCount()
						+ " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
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

	public boolean reserveItinerary(int id, int customer, Vector flightNumbers,
			String location, boolean car, boolean room) throws RemoteException {

		Trace.info("MW::reserveItinerary(" + id + ", " + customer + ", "
				+ flightNumbers + ", " + location + ", " + car + ", " + room
				+ ") was called");
		
		// Book the flights.
		for (Object flightNumber : flightNumbers) {
			// Must cast to integer because someone felt it would be a good idea
			// not to parameterize Vector
			if (!reserveFlight(id, customer, (Integer) flightNumber))
				return false; // Flight couldn't be reserved
		}
		
		if (car) {
			// Try to reserve the car
			if(!reserveCar(id, customer, location)) {
				return false;
			}
		}
		
		if (room) {
			// Try to reserve the room
			if (!reserveRoom(id, customer, location))
				return false;
		}
		
		// Everything worked!
		return true;
	}

}

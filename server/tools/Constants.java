package tools;

public class Constants {
	public final static String CAR_FILE_PTR = "../resources/cars/cars.ptr";
	public final static String CAR_FILE_1 = "../resources/cars/cars_1.ser";
	public final static String CAR_FILE_2 = "../resources/cars/cars_2.ser";
	
	public final static String ROOM_FILE_PTR = "../resources/rooms/rooms.ptr";
	public final static String ROOM_FILE_1 = "../resources/cars/rooms_1.ser";
	public final static String ROOM_FILE_2 = "../resources/cars/rooms_2.ser";
	
	public final static String FLIGHT_FILE_PTR = "../resources/flights/flights.ptr";
	public final static String FLIGHT_FILE_1 = "../resources/cars/flights_1.ser";
	public final static String FLIGHT_FILE_2 = "../resources/cars/flights_2.ser";
	
	public final static String CUSTOMER_FILE_PTR = "../resources/customers/customers.ptr";
	public final static String CUSTOMER_FILE_1 = "../resources/cars/customers_1.ser";
	public final static String CUSTOMER_FILE_2 = "../resources/cars/customers_2.ser";
	
	public final static String TRANSACTION_MANAGER_FILE = "../resources/txn-manager/txn-manager.ser";
	
	/**
	 * Given the master's file location, returns the non-master's location.
	 * @param master
	 * @return
	 */
	public static String getInverse(String master) {
		switch (master) {
		case CAR_FILE_1:		return CAR_FILE_2;
		case CAR_FILE_2:		return CAR_FILE_1;
		case ROOM_FILE_1:		return ROOM_FILE_2;
		case ROOM_FILE_2:		return ROOM_FILE_1;
		case FLIGHT_FILE_1:		return FLIGHT_FILE_2;
		case FLIGHT_FILE_2:		return FLIGHT_FILE_1;
		case CUSTOMER_FILE_1:	return CUSTOMER_FILE_2;
		case CUSTOMER_FILE_2:	return CUSTOMER_FILE_1;
		default:				return "";
		}
	}
}

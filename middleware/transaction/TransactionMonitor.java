package transaction;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;

import ResInterface.ResourceManager;

public class TransactionMonitor extends Thread {

	private final long ttl = 30000;
	private Hashtable<Integer, Long> ttl_records = new Hashtable<Integer, Long>();

	private ResourceManager rm;

	public TransactionMonitor(ResourceManager rm) {
		this.rm = rm;
	}

	public void refresh(int xid) {
		ttl_records.put(xid, System.currentTimeMillis());
	}

	public void unwatch(int xid) {
		ttl_records.remove(xid);
	}

	@Override
	public synchronized void run() {
		// TODO Auto-generated method stud
		super.run();

		while (true) {
			Enumeration<Integer> xids = ttl_records.keys();
			while (xids.hasMoreElements()) {
				Integer xid = xids.nextElement();
				long now = System.currentTimeMillis();
				Long previous = ttl_records.get(xid);
				if (previous != null) {
					if (now - previous < ttl) {
						try {
							rm.abort(xid);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidTransactionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}

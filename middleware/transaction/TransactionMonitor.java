package transaction;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;

import tools.Constants;

import ResInterface.ResourceManager;

public class TransactionMonitor extends Thread {

	private final long tm_ttl = Constants.COORDINATOR_TIMEOUT_MILLIS;
	private final long prepare_ttl = Constants.PARTICIPANT_TIMEOUT_MILLIS;
	private long ttl = tm_ttl;
	private Hashtable<Integer, Long> ttl_records = new Hashtable<Integer, Long>();
	private ResourceManager rm;

	public TransactionMonitor(ResourceManager rm) {
		this.rm = rm;
	}

	public TransactionMonitor(ResourceManager rm, boolean usePrepareTtl) {
		this.rm = rm;
		if (usePrepareTtl)
			ttl = prepare_ttl;
	}

	public void create(int xid) {
		ttl_records.put(xid, System.currentTimeMillis());
	}

	public void refresh(int xid) {
		if (ttl_records.containsKey(xid))
			ttl_records.put(xid, System.currentTimeMillis());
	}

	public void unwatch(int xid) {
		ttl_records.remove(xid);
	}

	@Override
	public synchronized void run() {
		super.run();

		while (true) {
			Enumeration<Integer> xids = ttl_records.keys();
			while (xids.hasMoreElements()) {
				Integer xid = xids.nextElement();
				long now = System.currentTimeMillis();
				Long previous = ttl_records.get(xid);
				if (previous != null) {
					if (now - previous > ttl) {
						try {
							System.out.println("Timeout on transaction id "
									+ xid + ", aborting transaction.");
							rm.abort(xid);
						} catch (RemoteException | InvalidTransactionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}

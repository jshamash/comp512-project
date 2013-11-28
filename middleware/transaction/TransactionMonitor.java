package transaction;

import java.util.Enumeration;
import java.util.Hashtable;

public class TransactionMonitor extends Thread {

	private final long ttl = 120000;
	private Hashtable<Integer, Long> ttl_records = new Hashtable<Integer, Long>();
	private TransactionManager tm;

	public TransactionMonitor(TransactionManager tm) {
		this.tm = tm;
	}
	
	public void create(int xid) {
		ttl_records.put(xid, System.currentTimeMillis());
	}
	
	public void refresh(int xid) {
		if(ttl_records.containsKey(xid))
			ttl_records.put(xid, System.currentTimeMillis());
	}

	public void unwatch(int xid) {
		ttl_records.remove(xid);
	}

	@Override
	public synchronized void run() {
		super.run();

		while (true) {
			// FIXME this creates an infinite loop of aborts
			Enumeration<Integer> xids = ttl_records.keys();
			while (xids.hasMoreElements()) {
				Integer xid = xids.nextElement();
				long now = System.currentTimeMillis();
				Long previous = ttl_records.get(xid);
				if (previous != null) {
					if (now - previous > ttl) {
						try {
							tm.abort(xid);
						} catch (InvalidTransactionException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}

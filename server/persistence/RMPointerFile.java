package persistence;

import java.io.Serializable;

public class RMPointerFile implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String master;
	private int xid;
	
	public RMPointerFile(String master, int xid) {
		this.master = master;
		this.xid = xid;
	}

	/**
	 * @return the master
	 */
	public String getMaster() {
		return master;
	}

	/**
	 * @return the xid
	 */
	public int getXid() {
		return xid;
	}
}

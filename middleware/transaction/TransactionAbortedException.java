package transaction;

public class TransactionAbortedException extends Exception {

	private static final long serialVersionUID = -8758456328822031941L;
	protected int xid = 0;
	
	public TransactionAbortedException(int xid, String msg)
	{
		super(msg);
		this.xid = xid;
	}
	
	public int getXId() {
		return this.xid;
	}
}

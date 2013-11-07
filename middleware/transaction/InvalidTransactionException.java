package transaction;

public class InvalidTransactionException extends Exception {
	
	private static final long serialVersionUID = -4994864664694202046L;
	protected int xid = 0;
	
	public InvalidTransactionException(int xid, String msg)
	{
		super(msg);
		this.xid = xid;
	}
	
	public int getXId() {
		return this.xid;
	}
}

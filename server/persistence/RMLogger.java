package persistence;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;

import tools.Constants;
import tools.Constants.TransactionStatus;

import LockManager.LockManager;
import ResImpl.RMHashtable;
import ResImpl.RMItem;

public class RMLogger implements Serializable{

	private static final long serialVersionUID = -8178102514560271842L;
	
	private RMHashtable data;
	private Hashtable<Integer, HashMap<String, RMItem>> t_records;
	private Hashtable<Integer, TransactionStatus> t_status;
	private LockManager lockManager;
	
	public RMLogger(RMHashtable data,
			Hashtable<Integer, HashMap<String, RMItem>> t_records,
			Hashtable<Integer, TransactionStatus> t_status,
			LockManager lockManager) {
		super();
		this.data = data;
		this.t_records = t_records;
		this.t_status = t_status;
		this.lockManager = lockManager;
	}

	/**
	 * @return the data
	 */
	public RMHashtable getData() {
		return data;
	}

	/**
	 * @return the t_records
	 */
	public Hashtable<Integer, HashMap<String, RMItem>> getT_records() {
		return t_records;
	}

	/**
	 * @return the t_status
	 */
	public Hashtable<Integer, TransactionStatus> getT_status() {
		return t_status;
	}

	/**
	 * @return the lockManager
	 */
	public LockManager getLockManager() {
		return lockManager;
	}
	
}

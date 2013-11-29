package persistence;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import tools.Constants.RMType;
import tools.Constants.TransactionStatus;

public class TMLogger implements Serializable {
	private static final long serialVersionUID = 2188218456231557546L;
	private int tid_counter;
	protected HashMap<Integer,LinkedList<RMType>> rm_records;
	private HashMap<Integer, TransactionStatus> t_status;
	
	public TMLogger(int tid_counter,
			HashMap<Integer, LinkedList<RMType>> rm_records,
			HashMap<Integer, TransactionStatus> t_status) {
		super();
		this.tid_counter = tid_counter;
		this.rm_records = rm_records;
		this.t_status = t_status;
	}

	/**
	 * @return the tid_counter
	 */
	public int getTid_counter() {
		return tid_counter;
	}

	/**
	 * @return the rm_records
	 */
	public HashMap<Integer, LinkedList<RMType>> getRm_records() {
		return rm_records;
	}

	/**
	 * @return the t_status
	 */
	public HashMap<Integer, TransactionStatus> getT_status() {
		return t_status;
	}
	
}

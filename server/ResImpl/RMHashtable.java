// -------------------------------// Kevin T. Manley// CSE 593// -------------------------------package ResImpl;import java.io.Serializable;import java.util.*;// A specialization of Hashtable with some//  extra diagnosticspublic class RMHashtable extends Hashtable {	private static final long serialVersionUID = 5950097142436197148L;	public RMHashtable() {		super();	}	public String toString() {		String s = "--- BEGIN RMHashtable ---\n";		Object key = null;		for (Enumeration e = keys(); e.hasMoreElements();) {			key = e.nextElement();			RMItem value = (RMItem) get(key);			s = s + "[KEY='" + key + "']" + value.toString() + "\n";		}		s = s + "--- END RMHashtable ---";		return s;	}	public void dump() {		System.out.println(toString());	}}
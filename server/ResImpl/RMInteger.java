// -------------------------------
// adapted Kevin T. Manley
// CSE 593
// -------------------------------
package ResImpl;

import java.io.*;

// A simple Integer wrapper
public class RMInteger extends RMItem implements Serializable {
	private static final long serialVersionUID = 4206002016627062536L;
	protected int m_value;

	public RMInteger(int value) {
		m_value = value;
	}

	public int getValue() {
		return m_value;
	}

	public void setValue(int value) {
		m_value = value;
	}

	public String toString() {
		return String.valueOf(m_value);
	}
}

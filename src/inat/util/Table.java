package inat.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class represents a table. This table is indexed in the classical X,Y
 * manner. With X being the first index, and Y being the second index. Column is
 * an alias for X, and Row is an alias for Y.
 * 
 * @author Brend Wanders
 * 
 */
public class Table implements Iterable<int[]>, Serializable {
	private static final long serialVersionUID = 1098840617529402193L;
	private int[][] values;

	/**
	 * Constructor that sets the number of rows and columns.
	 * 
	 * @param rows the number of rows in this table
	 * @param cols the number of columns in this table
	 */
	public Table(int rows, int cols) {
		this.values = new int[cols][rows];
	}
	
	public Table() {
	}
	
	public int[][] getValues() {
		return this.values;
	}
	
	public void setValues(int[][] values) {
		this.values = values;
	}
	
	/**
	 * returns a (deep) copy of this
	 * @return
	 */
	public Table copy() {
		Table table = new Table(this.values[0].length, this.values.length);
		for (int i=0;i<this.values.length;i++) {
			for (int j=0;j<this.values[i].length;j++) {
				table.values[i][j] = this.values[i][j];
			}
		}
		return table;
	}

	/**
	 * Gets a value from this table.
	 * 
	 * @param row the row to retrieve a value from
	 * @param col the column to retrieve the value from
	 * @return the value
	 */
	public int get(int row, int col) {
		return this.values[col][row];
	}

	/**
	 * Sets the value at the given row and column.
	 * 
	 * @param row the row index
	 * @param col the column index
	 * @param value the value to set
	 */
	public void set(int row, int col, int value) {
		this.values[col][row] = value;
	}

	/**
	 * Returns the number of rows in this table.
	 * 
	 * @return the number of rows
	 */
	public int getRowCount() {
		return this.values[0].length;
	}

	/**
	 * Returns the number of columns in this table.
	 * 
	 * @return the number of columns
	 */
	public int getColumnCount() {
		return this.values.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		for (int r = 0; r < this.getRowCount(); r++) {
			sb.append("[");
			for (int c = 0; c < this.getColumnCount(); c++) {
				sb.append(this.get(r, c));
				if (c < this.getColumnCount() - 1) {
					sb.append(", ");
				}
			}
			sb.append("]");
			if (r < this.getRowCount() - 1) {
				sb.append(", ");
			}
		}

		sb.append("]");
		return sb.toString();
	}

	/**
	 * Returns an iterator that iteratres over each column in this table.
	 */
	@Override
	public Iterator<int[]> iterator() {
		return Arrays.asList(this.values).iterator();
	}

	/**
	 * Returns a column by index.
	 * 
	 * @param col the column to return
	 * @return the column
	 */
	public int[] getColumn(int col) {
		return Arrays.copyOf(this.values[col], this.getRowCount());
	}

	/**
	 * Returns a row by index.
	 * 
	 * @param row the row to return
	 * @return the row
	 */
	public int[] getRow(int row) {
		int[] result = new int[this.getColumnCount()];
		int i = 0;
		for (int[] col : this) {
			result[i++] = col[row];
		}
		return result;
	}
}

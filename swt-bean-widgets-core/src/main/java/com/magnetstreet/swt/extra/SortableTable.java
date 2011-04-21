package com.magnetstreet.swt.extra;

import com.magnetstreet.swt.util.DateUtil;
import com.magnetstreet.swt.util.TableColumnComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a simple wrapper of the Table SWT gui
 * widget to provide default Java based sortable columns
 * in the least obtrusive way possible. By wrapping the
 * original SWT Table we insure compatability on all systems
 * and future upgrades.
 * @author Martin Dale Lyness <martin.lyness@gmail.com>
 * @version 0.1.0
 */
public class SortableTable {
    private static Logger logger = Logger.getLogger(SortableTable.class.getSimpleName());

	private Table tbl;
	private SortedColumn[] sortedCols;
    private SortedColumn currentSortCol;

	
	/**
	 * Defines the basic string comparison algorithm used by default on
	 * a sortable table
	 */
	private class DefaultComparisonAlgorithm implements TableColumnComparator {
		private int colToCompare;
		private boolean reverseOrder;
		public DefaultComparisonAlgorithm(int col, boolean reverse) {
			colToCompare = col;
			reverseOrder = reverse;
		}
		public void setReverseBit(boolean reverse) { this.reverseOrder = reverse; }
		public int compare(TableItem a, TableItem b) {
			int v = a.getText(colToCompare).compareTo(b.getText(colToCompare));
			if(reverseOrder)
				return -1 * v;
			return v;
		}
	}

    /**
     * Defines a simple integer comparison algorithm which can be swapped for columns
     * that will only ever contain integers.
     */
    public static class IntegerComparisonAlgorithm implements TableColumnComparator {
        private int colToCompare;
		private boolean reverseOrder;
		public IntegerComparisonAlgorithm(int col, boolean reverse) {
			colToCompare = col;
			reverseOrder = reverse;
		}
		public void setReverseBit(boolean reverse) { this.reverseOrder = reverse; }
		public int compare(TableItem a, TableItem b) {
            int v = 0;
            try {
			    v = new Integer(a.getText(colToCompare)).compareTo(new Integer(b.getText(colToCompare)));
            } catch(NumberFormatException nfe) {
                logger.log(Level.SEVERE, "Attempted to use integer comparison while sorting column with values: " + a.getText(colToCompare) + ", " + b.getText(colToCompare), nfe);
            }
			if(reverseOrder)
				return -1 * v;
			return v;
		}
    }

    /**
     * Defines a simple BigDecimal comparison algorithm which can be swapped for columns
     * that will only ever contain BigDecimal values.
     */
    public static class BigDecimalComparisonAlgorithm implements TableColumnComparator {
        private int colToCompare;
		private boolean reverseOrder;
		public BigDecimalComparisonAlgorithm(int col, boolean reverse) {
			colToCompare = col;
			reverseOrder = reverse;
		}
		public void setReverseBit(boolean reverse) { this.reverseOrder = reverse; }
		public int compare(TableItem a, TableItem b) {
            int v = 0;
            try {
			    v = new BigDecimal(a.getText(colToCompare)).compareTo(new BigDecimal(b.getText(colToCompare)));
            } catch(NumberFormatException nfe) {
                logger.log(Level.SEVERE, "Attempted to use BigDecimal comparison while sorting column with values: " + a.getText(colToCompare) + ", " + b.getText(colToCompare), nfe);
            }
			if(reverseOrder)
				return -1 * v;
			return v;
		}
    }

    /**
     * Defines a simple Date comparison algorithm which can be used for columns that have
     * data always formatted in some standard date form (defined by the user at construction).
     */
    public static class DateComparisonAlgorithm implements TableColumnComparator {
        private int colToCompare;
		private boolean reverseOrder;
        private SimpleDateFormat formatter;
        public DateComparisonAlgorithm(int col, boolean reverse) {
            colToCompare = col;
			reverseOrder = reverse;
            formatter = DateUtil.defaultDateFormat;
        }
		public DateComparisonAlgorithm(int col, boolean reverse, String dateFormat) {
			colToCompare = col;
			reverseOrder = reverse;
            formatter = new SimpleDateFormat(dateFormat);
		}
        public void setReverseBit(boolean reverse) { this.reverseOrder = reverse; }
        @Override public int compare(TableItem a, TableItem b) {
            int v = 0;
            try {
                Date dateA = (Date)formatter.parse(a.getText(colToCompare));
                Date dateB = (Date)formatter.parse(b.getText(colToCompare));
                v = dateA.compareTo(dateB);
            } catch(Throwable t) {
                logger.log(Level.SEVERE, "Unable to sort table by date on col '" + colToCompare + "' exception thrown.", t);
            }
            if(reverseOrder)
                return -1 * v;
            return v;
        }
    }
	
	/**
	 * Container object used like a Structure in C to hold a group of
	 * objects together for maintaining sorting order on a specific 
	 * column.
	 */
	private class SortedColumn {
		TableColumn col;
		int colId;
		boolean direction;
		TableColumnComparator cmpAlg;
		
		public SortedColumn(TableColumn col, int colId, boolean direction, TableColumnComparator cmpAlg) {
			this.col = col;
			this.colId = colId;
			this.direction = direction;
			this.cmpAlg = cmpAlg;
		}
	}
	
	/**
	 * A re-implementation of the selection adapter to allow the table
	 * object to be passed in on the selection event hanlder creation.
	 */
	private class SelectionAdapterWithSortableTable extends SelectionAdapter {
		protected SortableTable tbl;
		protected int colIndex;
		public SelectionAdapterWithSortableTable(SortableTable tbl, int colIndex) {
			this.tbl = tbl;
			this.colIndex = colIndex;
		}
	}
	
	public SortableTable(Table tbl) {
		this.tbl = tbl;
		hookColumns();
	}
	
	/**
	 * Sets up the listener hooks and addition information needed
	 * to track default sorting by this class. Called only once on
	 * creation by constructor.
	 */
	private void hookColumns() {
		TableColumn[] cols = tbl.getColumns();
		sortedCols = new SortedColumn[cols.length];
		for(int i=0; i<cols.length; i++) {
			sortedCols[i] = new SortedColumn(cols[i], i, false, new DefaultComparisonAlgorithm(i, false));
			cols[i].addSelectionListener(new SelectionAdapterWithSortableTable(this, i) {
				public void widgetSelected(SelectionEvent evt) {
					tbl.sortByColumn(colIndex);
				}
			});
		}
	}
	
	/**
	 * Allows a user to specify a custom comparison class to determine
	 * the ordering when a column is clicked for ordering. The default
	 * method tracks reverse ordering on second click internally, user
	 * supplied comparators will need to re implement this feature.
	 * @param col
	 * @param cmpAlg
	 */
	public void setComparator(int col, TableColumnComparator cmpAlg) {
		sortedCols[col].cmpAlg = cmpAlg;
	}

    /**
     * @return The current column id that the table is sorted by
     */
    public int getCurrentSortColumnId() { return currentSortCol.colId; }

    /**
     * @return The direction bit that tells the sorter to go in forward or backward direction.
     */
    public boolean getCurrentReverseBit() { return currentSortCol.direction; }

    /**
     * Resorts the table with the last used column and direction.
     */
    public void resort() {
        sortByColumn(getCurrentSortColumnId(), getCurrentReverseBit());
    }
	/**
	 * Sorts the table by the given colIndex by the comparator
	 * algorithm attached to the column
	 * @see SortableTable#setComparator(int, TableColumnComparator)
	 * @param colIndex
	 */
    public void sortByColumn(int colIndex) {
        sortByColumn(colIndex, !sortedCols[colIndex].direction);
    }
	public void sortByColumn(int colIndex, boolean direction) {
        logger.logp(Level.FINER, "SortableTable", "sortByColumn", "Sorting by column: "+colIndex+", reverse: " + direction);
        // Set current col for return to using classes
        currentSortCol = sortedCols[colIndex];
		// set sort direction
		sortedCols[colIndex].cmpAlg.setReverseBit(direction);
		// Simple bit switch to toggle directions of sort
		sortedCols[colIndex].direction = direction;
		
		TableItem[] items = tbl.getItems();
		int oldSelectionIndex = tbl.getSelectionIndex();
		int itemCount = items.length;
		int colCount = sortedCols.length;
		Arrays.sort(items, sortedCols[colIndex].cmpAlg);
		
		tbl.setRedraw(false);
		tbl.setSortColumn(sortedCols[colIndex].col);
		if(sortedCols[colIndex].direction)
			tbl.setSortDirection(0);
		else
			tbl.setSortDirection(1);
		
		for(int i=0; i<itemCount; i++) {
			TableItem item = new TableItem(tbl, SWT.NONE);
			item.setBackground(items[i].getBackground());
			item.setChecked(items[i].getChecked());
			item.setData(items[i].getData());
			item.setFont(items[i].getFont());
			item.setForeground(items[i].getForeground());
			item.setGrayed(items[i].getGrayed());
			String[] tmp = new String[colCount];
			for(int j=0; j<tmp.length; j++) {
				tmp[j] = items[i].getText(j);
			}
			item.setText(tmp);
		}

		tbl.remove(0, itemCount-1);
		tbl.select(oldSelectionIndex);
		tbl.setRedraw(true);
	}

    public Table getTable() {
        return tbl;
    }
}
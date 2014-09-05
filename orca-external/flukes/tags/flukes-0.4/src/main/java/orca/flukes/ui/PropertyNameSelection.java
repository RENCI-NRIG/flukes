package orca.flukes.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KScrollPane;
import com.hyperrealm.kiwi.ui.KTextArea;

public class PropertyNameSelection extends KPanel {
    private JTable table;
    private KTextArea output;
    private PropertyTableModel model;
 
    public PropertyNameSelection(List<Map<String, String>> l) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
 
        model = new PropertyTableModel(l);
        table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(30, 300));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        
        // adjust column width
        table.getColumnModel().getColumn(0).setPreferredWidth(10);
        
        add(new KScrollPane(table));
 
        output = new KTextArea(5, 40);
        output.setEditable(false);
        add(new KScrollPane(output));
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
        	String notice = model.getPropertyValueAt(table.getSelectedRow());
        	output.setText((notice != null ? notice : "No information available"));
        }
    }
 
    class PropertyTableModel extends AbstractTableModel {
        private String[] columnNames = {"Unit", "Property Name"};
        List<List<String>> properties;
        
        PropertyTableModel(List<Map<String, String>> p) {
        	
        	properties = new ArrayList<List<String>>();
        	int unit = 0;
        	for(Map<String, String> m1: p) {
        		for (Map.Entry<String, String> e: m1.entrySet()) {
        			List<String> ll = new ArrayList<String>();
        			ll.add("" + unit);
        			ll.add(e.getKey());
        			ll.add(e.getValue());
        			properties.add(ll);
        		}
        		unit++;
        	}
        }
        
        public int getColumnCount() {
            return 2;
        }
 
        public int getRowCount() {
        	return properties.size();
        }
 
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
 
        public Object getValueAt(int row, int col) {
        	try {
        		if (col == 0)
        			return properties.get(row).get(col);
        		if (col == 1)
        			return properties.get(row).get(col);
        	} catch (IndexOutOfBoundsException e) {
        		return null;
        	}
        	return null;
        } 
        
        public String getPropertyValueAt(int row) {
        	try {
        		return properties.get(row).get(2);
        	} catch (IndexOutOfBoundsException e) {
        		return null;
        	}
        }
    }
}

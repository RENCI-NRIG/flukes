package orca.flukes.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import orca.flukes.OrcaResource;

import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KScrollPane;
import com.hyperrealm.kiwi.ui.KTextArea;

public class TableSelection extends KPanel {
    private JTable table;
    private KTextArea output;
    private ResourceTableModel model;
 
    public TableSelection(List<OrcaResource> l) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
 
        model = new ResourceTableModel(l);
        table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        
        // adjust column width
        table.getColumnModel().getColumn(0).setPreferredWidth(350);
        
        add(new KScrollPane(table));
 
        output = new KTextArea(5, 40);
        output.setEditable(false);
        add(new KScrollPane(output));
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
        	String notice = model.getNoticeAt(table.getSelectedRow());
        	output.setText((notice != null ? notice : "No information available"));
        }
    }
 
    class ResourceTableModel extends AbstractTableModel {
        private String[] columnNames = {"Resource Name",
                                        "Resource State"};
        List<OrcaResource> resources;
        
        ResourceTableModel(List<OrcaResource> l) {
        	resources = new ArrayList<OrcaResource>();
        	for(OrcaResource res: l) {
        		if (res.isResource())
        			resources.add(res);
        	}
        }
        
        public int getColumnCount() {
            return 2;
        }
 
        public int getRowCount() {
        	return resources.size();
        }
 
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
 
        public Object getValueAt(int row, int col) {
        	try {
        		if (col == 0)
        			return resources.get(row).getName();
        		if (col == 1)
        			return resources.get(row).getState();
        	} catch (IndexOutOfBoundsException e) {
        		return null;
        	}
        	return null;
        } 
        
        public String getNoticeAt(int row) {
        	try {
        		return resources.get(row).getReservationNotice();
        	} catch (IndexOutOfBoundsException e) {
        		return null;
        	}
        }
    }
}

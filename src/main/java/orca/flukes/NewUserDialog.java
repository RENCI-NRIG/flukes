package orca.flukes;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import com.hyperrealm.kiwi.ui.KCheckBox;
import com.hyperrealm.kiwi.ui.KPanel;
import com.hyperrealm.kiwi.ui.KTextArea;
import com.hyperrealm.kiwi.ui.KTextField;
import com.hyperrealm.kiwi.ui.dialog.ComponentDialog;
import com.hyperrealm.kiwi.ui.dialog.KMessageDialog;

public class NewUserDialog extends ComponentDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JFrame parent;
    private KPanel kp;
    private final KTextArea ta;
    private KTextField username;
    private KCheckBox sudoCb;
    protected int ycoord = 1;
    private boolean sudoState = false;
    private GridBagLayout gbl_contentPanel;
    // [a-z_][a-z0-9_-]*[$]? for username
    private Pattern usernamePattern = Pattern.compile("[a-z_][a-z0-9_-]*[$]?");
    // #ssh-rsa AAAA[0-9A-Za-z+/]+[=]{0,3} ([^@]+@[^@]+)# for RSA pub key
    // #ssh-dss AAAA[0-9A-Za-z+/]+[=]{0,3} ([^@]+@[^@]+)# for DSA pub key
    private Pattern rsaPattern = Pattern.compile("ssh-rsa AAAA[0-9A-Za-z+/]+[=]{0,3} ([^@]+@[^@]+)");
    private Pattern dsaPattern = Pattern.compile("ssh-dss AAAA[0-9A-Za-z+/]+[=]{0,3} ([^@]+@[^@]+)");


    public NewUserDialog(JFrame parent, String title, String message, int r, int c) {
        super(parent, title, true);
        super.setLocationRelativeTo(parent);

        this.parent = parent;
        setComment(message);

        gbl_contentPanel = new GridBagLayout();
        kp.setLayout(gbl_contentPanel);

        {
            JLabel lblNewLabel_1 = new JLabel("Username: ");
            GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
            gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
            gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
            gbc_lblNewLabel_1.gridx = 0;
            gbc_lblNewLabel_1.gridy = ycoord;
            kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
        }
        {
            username = new KTextField();
            GridBagConstraints gbc_list = new GridBagConstraints();
            gbc_list.insets = new Insets(0, 0, 5, 5);
            gbc_list.fill = GridBagConstraints.HORIZONTAL;
            gbc_list.gridx = 1;
            gbc_list.gridy = ycoord;
            kp.add(username, gbc_list);
        }
        ycoord++;
        {
            JLabel lblNewLabel_1 = new JLabel("Sudo: ");
            GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
            gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
            gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
            gbc_lblNewLabel_1.gridx = 0;
            gbc_lblNewLabel_1.gridy = ycoord;
            kp.add(lblNewLabel_1, gbc_lblNewLabel_1);
        }
        {
            sudoCb = new KCheckBox(new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    sudoState = (sudoState ? false : true);
                }
            });
            GridBagConstraints gbc_list = new GridBagConstraints();
            gbc_list.insets = new Insets(0, 0, 5, 5);
            gbc_list.fill = GridBagConstraints.HORIZONTAL;
            gbc_list.gridx = 1;
            gbc_list.gridy = ycoord;
            kp.add(sudoCb, gbc_list);
        }
        ycoord++;
        {      
            ta = new KTextArea();
            ta.setRows(r);
            ta.setColumns(c);
            ta.setMinimumSize(new Dimension(500, 500));
            ta.setEditable(true);
            JScrollPane areaScrollPane = new JScrollPane(ta);
            areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            GridBagConstraints gbc_list = new GridBagConstraints();
            gbc_list.insets = new Insets(0, 0, 5, 5);
            gbc_list.fill = GridBagConstraints.BOTH;
            gbc_list.gridx = 1;
            gbc_list.gridy = ycoord;
            kp.add(areaScrollPane, gbc_list);
        }
    }

    @Override
    protected Component buildDialogUI() {
        kp = new KPanel();

        return kp;
    }

    public String getSSHKeys() {
        return ta.getText();
    }

    public String getUsername() {
        return username.getText();
    }

    public boolean getSudo() {
        return sudoState;
    }

    @Override
    public boolean accept() {
        if (getUsername().length() > 32) {
            inputErrorDialog("Invalid username", "Username too long!");
            return false;
        }

        Matcher usernameMatcher = usernamePattern.matcher(getUsername());
        if (!usernameMatcher.matches()) {
            inputErrorDialog("Invalid username", "Username doesn't match pattern");
            return false;
        }


        if (getSSHKeys().length() > 1000) {
            inputErrorDialog("Invalid public SSH key", "Public SSH key too long");
            return false;
        }

        Matcher rsaMatcher = rsaPattern.matcher(getSSHKeys());
        Matcher dsaMatcher = dsaPattern.matcher(getSSHKeys());

        if (!rsaMatcher.matches() && !dsaMatcher.matches()) {
            inputErrorDialog("Invalid Public SSH key", "Public SSH key doesn't match pattern for RSA or DSA");
            return false;
        }

        return true;
    }

    private void inputErrorDialog(String title, String message) {
        KMessageDialog kmd = new KMessageDialog(parent, title, true);
        kmd.setLocationRelativeTo(parent);
        kmd.setMessage(message);
        kmd.setVisible(true);
    }
}

/**
 * Author: Lon Smith, Ph.D.
 * Description: This is the framework for the database program. Additional requirements and functionality
 *    are to be built by you and your group.
 */

import java.awt.EventQueue;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.util.List;

import src.database.DatabaseManager;
import src.query.EmployeeSearchQuery;

public class EmployeeSearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtDatabase;
	private JList<String> lstDepartment;
	private DefaultListModel<String> department = new DefaultListModel<String>();
	private JList<String> lstProject;
	private DefaultListModel<String> project = new DefaultListModel<String>();
	private JTextArea textAreaEmployee;
	private final EmployeeSearchQuery searchQuery = new EmployeeSearchQuery();
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmployeeSearchFrame frame = new EmployeeSearchFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EmployeeSearchFrame() {
		setTitle("Employee Search");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 347);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Database:");
		lblNewLabel.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblNewLabel.setBounds(21, 23, 59, 14);
		contentPane.add(lblNewLabel);
		
		txtDatabase = new JTextField();
		txtDatabase.setBounds(90, 20, 193, 20);
		contentPane.add(txtDatabase);
		txtDatabase.setColumns(10);
		
		JButton btnDBFill = new JButton("Fill");
		/**
		 * The btnDBFill should fill the department and project JList with the 
		 * departments and projects from your entered database name.
		 */
		btnDBFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Reset both lists, then reload them from the entered database.
				department.clear();
				project.clear();
				String name = txtDatabase.getText().trim();
				try {
					DatabaseManager.open(name);
					searchQuery.checkSchema();
					for (String d : searchQuery.loadDepartments()) department.addElement(d);
					for (String p : searchQuery.loadProjects()) project.addElement(p);
				} catch (SQLException ex) {
					DatabaseManager.closeConnection();
					JOptionPane.showMessageDialog(EmployeeSearchFrame.this,
							"The database \"" + name + "\" could not be opened.\n\n" + ex.getMessage(),
							"Database Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		btnDBFill.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnDBFill.setBounds(307, 19, 68, 23);
		contentPane.add(btnDBFill);
		
		JLabel lblDepartment = new JLabel("Department");
		lblDepartment.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblDepartment.setBounds(52, 63, 89, 14);
		contentPane.add(lblDepartment);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblProject.setBounds(255, 63, 47, 14);
		contentPane.add(lblProject);
		
		
lstProject = new JList<String>(project);
lstProject.setFont(new Font("Tahoma", Font.PLAIN, 12));

// Allow multiple projects to be selected
lstProject.setSelectionMode(
    javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
);

// Add a scrollable container
JScrollPane scrollProject = new JScrollPane(lstProject);
scrollProject.setBounds(225, 84, 150, 50);
contentPane.add(scrollProject);

		
		JCheckBox chckbxNotDept = new JCheckBox("Not");
		chckbxNotDept.setBounds(71, 133, 59, 23);
		contentPane.add(chckbxNotDept);
		
		JCheckBox chckbxNotProject = new JCheckBox("Not");
		chckbxNotProject.setBounds(270, 133, 59, 23);
		contentPane.add(chckbxNotProject);
		
		
lstDepartment = new JList<String>(department);
lstDepartment.setFont(new Font("Tahoma", Font.PLAIN, 12));

// Allow multiple departments to be selected
lstDepartment.setSelectionMode(
    javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
);

// Add a scrollable container
JScrollPane scrollDepartment = new JScrollPane(lstDepartment);
scrollDepartment.setBounds(36, 84, 172, 50);
contentPane.add(scrollDepartment);

		
		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblEmployee.setBounds(52, 179, 89, 14);
		contentPane.add(lblEmployee);
		
		
	
JButton btnSearch = new JButton("Search");

btnSearch.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        try {
            // Get selected departments and projects
            List<String> selectedDepartments =
                lstDepartment.getSelectedValuesList();

            List<String> selectedProjects =
                lstProject.getSelectedValuesList();

            // Get NOT checkbox values
            boolean notDepartment = chckbxNotDept.isSelected();
            boolean notProject = chckbxNotProject.isSelected();

            // Execute employee search
            List<String> employees = searchQuery.searchEmployees(
                selectedDepartments,
                notDepartment,
                selectedProjects,
                notProject
            );

            // Display results
            textAreaEmployee.setText("");

            if (employees.isEmpty()) {
                textAreaEmployee.setText("No employees found.");
            } else {
                textAreaEmployee.setText(
                    String.join("\n", employees)
                );
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                EmployeeSearchFrame.this,
                "Employee search failed:\n" + ex.getMessage(),
                "Search Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
});


		btnSearch.setBounds(80, 276, 89, 23);
		contentPane.add(btnSearch);
		
		
JButton btnClear = new JButton("Clear");
btnClear.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {

        // Clear employee search results
        textAreaEmployee.setText("");

        // Clear selected departments
        lstDepartment.clearSelection();

        // Clear selected projects
        lstProject.clearSelection();

        // Uncheck both NOT checkboxes
        chckbxNotDept.setSelected(false);
        chckbxNotProject.setSelected(false);
    }
});


		btnClear.setBounds(236, 276, 89, 23);
		contentPane.add(btnClear);
		
		
textAreaEmployee = new JTextArea();
textAreaEmployee.setEditable(false);
textAreaEmployee.setLineWrap(true);
textAreaEmployee.setWrapStyleWord(true);

// Add scrollable container for employee results
JScrollPane scrollEmployee = new JScrollPane(textAreaEmployee);
scrollEmployee.setBounds(36, 197, 339, 68);
contentPane.add(scrollEmployee);

	}
}

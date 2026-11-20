/*
 * Original framework by Lon Smith, Ph.D.
 * Extended with SQLite queries and Swing event handling.
 * GUI tools: Eclipse WindowBuilder (original template); hand-written Java Swing
 * using GridBagLayout for this implementation. See README.md for setup.
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class EmployeeSearchFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField txtDatabase = new JTextField(30);
    private final DefaultListModel<CompanyDatabase.Choice> department = new DefaultListModel<>();
    private final DefaultListModel<CompanyDatabase.Choice> project = new DefaultListModel<>();
    private final JList<CompanyDatabase.Choice> lstDepartment = new JList<>(department);
    private final JList<CompanyDatabase.Choice> lstProject = new JList<>(project);
    private final JCheckBox chckbxNotDept = new JCheckBox("Not");
    private final JCheckBox chckbxNotProject = new JCheckBox("Not");
    private final JTextArea textAreaEmployee = new JTextArea(8, 40);
    private final JButton btnDBFill = new JButton("Fill");
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnClear = new JButton("Clear");
    private final JLabel status = new JLabel("Enter a SQLite database filename, then click Fill.");

    private transient CompanyDatabase database;
    private String loadedFilename;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new EmployeeSearchFrame().setVisible(true));
    }

    public EmployeeSearchFrame() {
        setTitle("Employee Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(18, 22, 18, 22));
        setContentPane(contentPane);

        JPanel databaseRow = new JPanel(new BorderLayout(12, 0));
        JLabel lblDatabase = new JLabel("Database:");
        lblDatabase.setLabelFor(txtDatabase);
        databaseRow.add(lblDatabase, BorderLayout.WEST);
        databaseRow.add(txtDatabase, BorderLayout.CENTER);
        databaseRow.add(btnDBFill, BorderLayout.EAST);
        txtDatabase.setToolTipText("Path to an existing SQLite COMPANY database file");
        addCell(contentPane, databaseRow, 0, 0, 2, 0);

        JLabel lblDepartment = new JLabel("Department");
        lblDepartment.setLabelFor(lstDepartment);
        addCell(contentPane, lblDepartment, 0, 1, 1, 0);
        JLabel lblProject = new JLabel("Project");
        lblProject.setLabelFor(lstProject);
        addCell(contentPane, lblProject, 1, 1, 1, 0);

        lstDepartment.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstProject.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstDepartment.setVisibleRowCount(6);
        lstProject.setVisibleRowCount(6);
        String selectionHelp = "Ctrl-click (Command-click on macOS) or Shift-click to select multiple items";
        lstDepartment.setToolTipText(selectionHelp);
        lstProject.setToolTipText(selectionHelp);
        addCell(contentPane, new JScrollPane(lstDepartment), 0, 2, 1, 1);
        addCell(contentPane, new JScrollPane(lstProject), 1, 2, 1, 1);
        chckbxNotDept.setToolTipText("Exclude employees in any selected department");
        chckbxNotProject.setToolTipText("Exclude employees working on any selected project");
        chckbxNotDept.getAccessibleContext().setAccessibleName("Not in selected departments");
        chckbxNotProject.getAccessibleContext().setAccessibleName("Not on selected projects");
        addCell(contentPane, chckbxNotDept, 0, 3, 1, 0);
        addCell(contentPane, chckbxNotProject, 1, 3, 1, 0);

        JLabel lblEmployee = new JLabel("Employee");
        lblEmployee.setLabelFor(textAreaEmployee);
        addCell(contentPane, lblEmployee, 0, 4, 2, 0);
        textAreaEmployee.setEditable(false);
        addCell(contentPane, new JScrollPane(textAreaEmployee), 0, 5, 2, 2);
        addCell(contentPane, btnSearch, 0, 6, 1, 0);
        addCell(contentPane, btnClear, 1, 6, 1, 0);
        addCell(contentPane, status, 0, 7, 2, 0);

        btnDBFill.addActionListener(event -> fillDatabase());
        txtDatabase.addActionListener(event -> fillDatabase());
        btnSearch.addActionListener(event -> searchEmployees());
        btnClear.addActionListener(event -> {
            clearSearch();
            status.setText(database == null ? "Enter a database filename, then click Fill." : "Search cleared.");
        });
        btnSearch.setEnabled(false);
        getRootPane().setDefaultButton(btnDBFill);
        setMinimumSize(new Dimension(580, 470));
        setSize(760, 570);
        setLocationByPlatform(true);
    }

    private static void addCell(JPanel panel, java.awt.Component component,
            int x, int y, int width, double weightY) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.weightx = 1;
        constraints.weighty = weightY;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(5, 8, 5, 8);
        panel.add(component, constraints);
    }

    private void clearSearch() {
        textAreaEmployee.setText("");
        lstDepartment.clearSelection();
        lstProject.clearSelection();
        chckbxNotDept.setSelected(false);
        chckbxNotProject.setSelected(false);
    }

    private void fillDatabase() {
        final String filename = txtDatabase.getText().trim();
        // Discard old data even if opening the next database fails.
        database = null;
        loadedFilename = null;
        department.clear();
        project.clear();
        clearSearch();
        setBusy(true);
        status.setText("Opening database...");

        new SwingWorker<CompanyDatabase.Catalog, Void>() {
            private CompanyDatabase candidate;

            @Override
            protected CompanyDatabase.Catalog doInBackground() throws Exception {
                candidate = new CompanyDatabase(filename);
                return candidate.loadCatalog();
            }

            @Override
            protected void done() {
                try {
                    CompanyDatabase.Catalog catalog = get();
                    for (CompanyDatabase.Choice choice : catalog.departments) {
                        department.addElement(choice);
                    }
                    for (CompanyDatabase.Choice choice : catalog.projects) {
                        project.addElement(choice);
                    }
                    database = candidate;
                    loadedFilename = filename;
                    status.setText(department.size() + " departments, " + project.size() + " projects loaded.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("Database could not be opened", exception);
                } catch (ExecutionException exception) {
                    showFailure("Database could not be opened", exception.getCause());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void searchEmployees() {
        if (database == null || !txtDatabase.getText().trim().equals(loadedFilename)) {
            JOptionPane.showMessageDialog(this, "Click Fill to load the database before searching.",
                    "Load database", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final CompanyDatabase currentDatabase = database;
        final List<CompanyDatabase.Choice> departments = lstDepartment.getSelectedValuesList();
        final List<CompanyDatabase.Choice> projects = lstProject.getSelectedValuesList();
        final boolean notDepartment = chckbxNotDept.isSelected();
        final boolean notProject = chckbxNotProject.isSelected();
        textAreaEmployee.setText("");
        setBusy(true);
        status.setText("Searching...");

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return currentDatabase.search(departments, notDepartment, projects, notProject);
            }

            @Override
            protected void done() {
                try {
                    List<String> employees = get();
                    textAreaEmployee.setText(String.join("\n", employees));
                    textAreaEmployee.setCaretPosition(0);
                    status.setText(employees.size() + " employee(s) found.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("Employee search failed", exception);
                } catch (ExecutionException exception) {
                    showFailure("Employee search failed", exception.getCause());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        txtDatabase.setEnabled(!busy);
        btnDBFill.setEnabled(!busy);
        btnSearch.setEnabled(!busy && database != null);
        btnClear.setEnabled(!busy);
        lstDepartment.setEnabled(!busy);
        lstProject.setEnabled(!busy);
        chckbxNotDept.setEnabled(!busy);
        chckbxNotProject.setEnabled(!busy);
    }

    private void showFailure(String title, Throwable cause) {
        status.setText(title + ".");
        String detail = cause.getMessage();
        JOptionPane.showMessageDialog(this, title + ".\n"
                + (detail == null ? cause.getClass().getSimpleName() : detail), title, JOptionPane.ERROR_MESSAGE);
    }
}

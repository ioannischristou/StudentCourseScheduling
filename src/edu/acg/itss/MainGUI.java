package edu.acg.itss;

import gurobi.GRBException;
import java.awt.Cursor;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * main entry point to the ACG SCORER application. The application allows 
 * students to create course plans, ie what courses to take when, until their 
 * graduation. Students may then edit their course plans, by specifying further
 * desired (or undesired!) courses, when to take certain courses, and set 
 * constraints on the number of courses they wish to take on specific terms. 
 * Once they have specified all their preferences, they can hit the "Change 
 * Terms" button, and then the "Run" button to produce a new plan that satisfies
 * their new preferences as well.
 * Course planning is modeled as a Mixed Integer Programming problem
 * with binary and continuous variables that is solved by an optimization solver
 * (currently GUROBI, but the Open-Source SCIP solver also solves all resulting
 * problems, but at a slower rate.)
 * @author itc
 */
public class MainGUI extends javax.swing.JFrame {

    /**
     * path to all required files is given at command line.
     */
    private static String _dir2Files = null;
    
    /**
     * student-name needed to add to the [passed|desired]courses.txt files to 
     * differentiate from one application process to another concurrently 
     * running. Has package-access level as the _startTime so it is accessible
     * from MIPHandler.
     */
    static String _studentName = "Student";
    /**
     * the time-stamp when the process starts is needed for differentiating 
     * between independent applications of the same program running at the 
     * same time (but they must have all started at a different time, when
     * the granularity is taken at the milli-second level. Student name already
     * should be sufficient for this differentiation.
     */
    final static long _startTime = System.currentTimeMillis();
    
    /**
     * number of OU courses registered for so far in the current academic year.
     * Required if the next term is not the FALL term.
     */
    private int _passed_OU_in_cur_academic_year = -1;
    
    /**
     * set to true when user clicks on the _feasCheckBtn, and only remains true
     * while the system performs the feasibility check.
     */
    private boolean _feasChkAsked = false;
    
    /**
     * model behind all courses maintains Course objects.
     */
    private final DefaultListModel _classListModel = new DefaultListModel();
    /**
     * model behind ITC "desired" courses maintains CodeNameAllowedTerms 
     * objects.
     */
    private final DefaultListModel _itcClassListModel = new DefaultListModel();
    /**
     * model behind concentration areas ("focus areas") objects.
     */
    private final DefaultListModel _concAreasModel = new DefaultListModel();
    private final MIPHandler _miphdlr = new MIPHandler();
    
    /**
     * maintains for each course-id in the solution the text-field that has
     * the user preferences for when to take the course.
     */
    private final HashMap<Integer, JTextField> _varTermsMap = new HashMap<>();
    /**
     * maintains for each termno in the solution, the text-field that contains
     * the user preferences for how many courses to take in that term, eg 
     * "&lt;4" means user want to take strictly less than 4 courses to take on
     * that term, etc. User could also specify "&ge;3", or just "2". Whenever 
     * such a value is provided in the text-field, it overrides any user 
     * preferences in the <CODE>_maxNumCrsPerSemFld</CODE> text-field, but does
     * NOT override college-imposed constraints on the maximum number of credits
     * or courses, and thus it can result in infeasible schedules if not used
     * carefully.
     */
    private final HashMap<Integer, JTextField> _numCoursesPerTerm2FldMap = 
        new HashMap<>();
    /**
     * same as above, but holds as values the strings that were typed in each
     * text-box, and is given as input to the 
     * <CODE>MIPHandler.createMIPFile()</CODE> method.
     */
    private final HashMap<Integer, String> _numCoursesPerTerm2StrMap = 
        new HashMap<>();
    
    /**
     * Creates new form MainGUI.
     */
    public MainGUI() {
        // first, ask for the name of the student for whom the plan will be 
        // built.
        _studentName = JOptionPane.showInputDialog("Enter Student ID (or name):");
        // replace spaces with underscores to avoid issues with command-line
        _studentName = _studentName.replace(" ", "_");
        // next call technically "escapes constructor" but seems to be OK
        this.setTitle("ACG SCORER for "+_studentName);
        populateCourseListModels();
        initComponents();
        // write current date in txtfld
        LocalDate now = LocalDate.now();
        int cur_day = now.getDayOfMonth();
        int cur_mon = now.getMonthValue();
        int cur_year = now.getYear();
        CurrentDate._curDay = cur_day;
        CurrentDate._curMonth = cur_mon;
        CurrentDate._curYear = cur_year;
        this._curDateTxtFld.setText(Integer.toString(cur_day)+"/"+
                                    Integer.toString(cur_mon)+"/"+
                                    Integer.toString(cur_year));
        // load passed and desired courses if there are any
        _loadMenuItemActionPerformed(null);
        enableRightClick4DesiredCoursesList();
    }
    
    
    /**
     * calls MIPHandler to read problem data and populates the list models for
     * display in the GUI elements.
     */
    final void populateCourseListModels() {
        // reset the course models as they are about to be (re-?)loaded
        _itcClassListModel.clear();
        _classListModel.clear();
        _concAreasModel.clear();
        
        _miphdlr.readProblemData(_studentName);
        
        // disable the Edit buttons if AllowEdit is not true
        boolean allow_edit = _miphdlr.getScheduleParams().getAllowEdit();
        if (!allow_edit) {
            this._editCoursesMenuItem.setEnabled(false);
            this._editCourseGroupsMenuItem.setEnabled(false);
        }
        
        String program_code = _miphdlr.getScheduleParams().getProgramCode();
        Iterator<String> codes = Course.getAllCodesIterator();
        while (codes.hasNext()) {
            String c = codes.next();
            Course crs = Course.getCourseByCode(c);
            if (c.startsWith(program_code)) {
                CodeNameAllowedTerms cnat = 
                        new CodeNameAllowedTerms(crs.getCode(), crs.getName(), 
                                                 "allterms");
                _itcClassListModel.addElement(cnat);
            }
            _classListModel.addElement(crs);
        }
        Set<String> conc_areas = CourseGroup.getAllConcentrationAreas();
        for (String name: conc_areas) this._concAreasModel.addElement(name);
        
        // check if the first planning semester is a FALL term, and if it's not
        // then ask for the number of passed OU courses during the current 
        // academic year
        // int passed_OU_in_cur_academic_year = 0;
        while (!Course.isFallTerm(1) && _passed_OU_in_cur_academic_year==-1) {
            String num_str = 
                    JOptionPane.showInputDialog("#OU courses already taken "+
                                                "during current academic year");
            try {
             _passed_OU_in_cur_academic_year = Integer.parseInt(num_str);
            }
            catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter "+
                                                    "a non-negative integer");
            }
        }
        
    }
    
    
    /**
     * allow desired courses list items to be edited (using Ctl-Right-Click)
     * to set (or reset) desired terms to take the course. If the terms entered
     * are not OK, the course is considered and labeled as undesired!
     */
    final void enableRightClick4DesiredCoursesList() {
        // allow the desired courses list items (CodeNameAllowedTerms
        // objects) to be edited by Ctl-right-click for desired term
        _desiredCoursesList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // first, set the CurrentDate
                String cur_date = _curDateTxtFld.getText();
                String[] cds = cur_date.split("/");
                CurrentDate._curDay = Integer.parseInt(cds[0]);
                CurrentDate._curMonth = Integer.parseInt(cds[1]);
                CurrentDate._curYear = Integer.parseInt(cds[2]);
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = 
                            _desiredCoursesList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        // preserve existing selections
                        _desiredCoursesList.addSelectionInterval(index, index);

                        if (e.isControlDown()) {
                            CodeNameAllowedTerms item = 
                                (CodeNameAllowedTerms) _itcClassListModel.
                                                         getElementAt(index);

                            String newValue = 
                                    JOptionPane.showInputDialog(
                                                  _desiredCoursesList,
                                                  "Enter desired term " +
                                                  "(or '-' or 'clear') for " + 
                                                  item._code + ":",
                                                  "Desired Terms", 
                                                  JOptionPane.PLAIN_MESSAGE);

                            if (newValue!=null && !newValue.trim().isEmpty()) {
                                // ensure newValue is OK
                                if ("clear".equals(newValue)) {
                                    item._allowedTerms = "allterms";
                                    _itcClassListModel.setElementAt(item, index);
                                    return;
                                }
                                final int Smax = 
                                        _miphdlr.getScheduleParams().getSmax();
                                if (CodeNameAllowedTerms.prefferedTermsAllowed(
                                                           item._code, 
                                                           newValue, 
                                                           -1, 
                                                           Smax)==false) {
                                    newValue = "-";
                                }
                                item._allowedTerms = newValue;
                                _itcClassListModel.setElementAt(item, index);
                            }
                            else {  // reset desired terms
                                item._allowedTerms = "";
                                _itcClassListModel.setElementAt(item, index);
                            }
                        }
                    }
                }
            }
        });        
    }
    
    
    /**
     * get the directory name relative to the root of the application where the
     * files are located. This string is gotten from the cmd-line arguments.
     * @return String
     */
    public static String getDir2Files() {
        return _dir2Files;
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        _outputsPanel = new javax.swing.JPanel();
        _outputsPanelLbl = new javax.swing.JLabel();
        _outputAreaScrollPane = new javax.swing.JScrollPane();
        _outputsArea = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        _outputTextPane = new javax.swing.JTextPane();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        _inputsPanel = new javax.swing.JPanel();
        _inputsPanelLbl = new javax.swing.JLabel();
        _passedCourseSelectorLbl = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        _passedCoursesList = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        _concNamesList = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        _runBtn = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        _desiredCoursesList = new javax.swing.JList<>();
        _honorStudentChkBox = new javax.swing.JRadioButton();
        _shortestComplTimeBtn = new javax.swing.JRadioButton();
        _diffiBalanceBtn = new javax.swing.JRadioButton();
        _summerSemestersOffChkBox = new javax.swing.JRadioButton();
        _s1ChkBox = new javax.swing.JCheckBox();
        _s2ChkBox = new javax.swing.JCheckBox();
        _stChkBox = new javax.swing.JCheckBox();
        _curDateLbl = new javax.swing.JLabel();
        _curDateTxtFld = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        _maxNumCrsPerSemFld = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        _maxNumCoursesDuringThesisFld = new javax.swing.JTextField();
        _feasCheckBtn = new javax.swing.JButton();
        _menuBar = new javax.swing.JMenuBar();
        _fileMenu = new javax.swing.JMenu();
        _loadMenuItem = new javax.swing.JMenuItem();
        _saveCoursesMenuItem = new javax.swing.JMenuItem();
        _saveScheduleMenuItem = new javax.swing.JMenuItem();
        _exitMenuItem = new javax.swing.JMenuItem();
        _editMenu = new javax.swing.JMenu();
        _editCoursesMenuItem = new javax.swing.JMenuItem();
        _editCourseGroupsMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        _outputsPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        _outputsPanelLbl.setText("Outputs Area");

        _outputAreaScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        _outputAreaScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        _outputsArea.setEditable(false);
        _outputsArea.setColumns(20);
        _outputsArea.setRows(5);
        _outputAreaScrollPane.setViewportView(_outputsArea);

        jScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane4.setPreferredSize(_outputTextPane.getPreferredSize());
        jScrollPane4.setViewportView(_outputTextPane);

        jLabel5.setText("Schedule:");

        jLabel6.setText("Edit Time-Slots for Courses:");

        javax.swing.GroupLayout _outputsPanelLayout = new javax.swing.GroupLayout(_outputsPanel);
        _outputsPanel.setLayout(_outputsPanelLayout);
        _outputsPanelLayout.setHorizontalGroup(
            _outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_outputsPanelLayout.createSequentialGroup()
                .addGroup(_outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(_outputsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(_outputsPanelLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel5))
                    .addComponent(_outputAreaScrollPane))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        _outputsPanelLayout.setVerticalGroup(
            _outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_outputsPanelLayout.createSequentialGroup()
                .addGroup(_outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(_outputsPanelLbl)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_outputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                    .addComponent(_outputAreaScrollPane))
                .addContainerGap())
        );

        _inputsPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        _inputsPanelLbl.setText("Inputs Area");

        _passedCourseSelectorLbl.setText("Select all courses passed so far:");

        _passedCoursesList.setModel(_classListModel);
        _passedCoursesList.setToolTipText("select all courses passed already (use Ctl-Left Click for multiple selections)");
        jScrollPane2.setViewportView(_passedCoursesList);

        jLabel1.setText("Select Desired Concentration:");

        _concNamesList.setModel(this._concAreasModel);
        _concNamesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        _concNamesList.setToolTipText("Select one from the list");
        jScrollPane3.setViewportView(_concNamesList);

        jLabel2.setText("Select Schedule Objective:");

        _runBtn.setText("RUN");
        _runBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _runBtnActionPerformed(evt);
            }
        });

        jLabel3.setText("Select Desired Courses To Take:");

        _desiredCoursesList.setModel(_itcClassListModel);
        _desiredCoursesList.setToolTipText("Use R-Click for multiple selection; use Ctl-R-Click to specify term to take course");
        jScrollPane5.setViewportView(_desiredCoursesList);

        _honorStudentChkBox.setText("I am an honor student");
        _honorStudentChkBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _honorStudentChkBoxActionPerformed(evt);
            }
        });

        buttonGroup1.add(_shortestComplTimeBtn);
        _shortestComplTimeBtn.setSelected(true);
        _shortestComplTimeBtn.setText("Shortest Compl. Time");
        _shortestComplTimeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _shortestComplTimeBtnActionPerformed(evt);
            }
        });

        buttonGroup1.add(_diffiBalanceBtn);
        _diffiBalanceBtn.setText("Max. Expected GPA");

        _summerSemestersOffChkBox.setText("Summer Periods Off");
        _summerSemestersOffChkBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _summerSemestersOffChkBoxActionPerformed(evt);
            }
        });

        _s1ChkBox.setText("S1 off");

        _s2ChkBox.setText("S2 off");

        _stChkBox.setText("ST off");

        _curDateLbl.setText("Current Date: ");

        _curDateTxtFld.setText("DD/MM/YYYY");
        _curDateTxtFld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _curDateTxtFldActionPerformed(evt);
            }
        });

        jLabel4.setText("Max #Courses/Sem.:");

        _maxNumCrsPerSemFld.setText("5");
        _maxNumCrsPerSemFld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _maxNumCrsPerSemFldActionPerformed(evt);
            }
        });

        jLabel7.setText("Max #Courses dur. Thesis:");

        _maxNumCoursesDuringThesisFld.setText("1");

        _feasCheckBtn.setText("Feas. Chk");
        _feasCheckBtn.setToolTipText("Check whether desired courses lead to feasible schedule, when next term only contains courses designated for then");
        _feasCheckBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _feasCheckBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout _inputsPanelLayout = new javax.swing.GroupLayout(_inputsPanel);
        _inputsPanel.setLayout(_inputsPanelLayout);
        _inputsPanelLayout.setHorizontalGroup(
            _inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_inputsPanelLayout.createSequentialGroup()
                .addComponent(_inputsPanelLbl)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(_inputsPanelLayout.createSequentialGroup()
                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(_inputsPanelLayout.createSequentialGroup()
                        .addComponent(_passedCourseSelectorLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2))
                    .addGroup(_inputsPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(_inputsPanelLayout.createSequentialGroup()
                                .addComponent(_curDateLbl)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(_curDateTxtFld, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(_runBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(_inputsPanelLayout.createSequentialGroup()
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(_shortestComplTimeBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(_diffiBalanceBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(_inputsPanelLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(_inputsPanelLayout.createSequentialGroup()
                                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(_summerSemestersOffChkBox)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, _inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(_stChkBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(_s2ChkBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(_s1ChkBox, javax.swing.GroupLayout.Alignment.TRAILING))
                                    .addComponent(_honorStudentChkBox))
                                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(_inputsPanelLayout.createSequentialGroup()
                                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel7)
                                            .addComponent(jLabel4))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(_maxNumCrsPerSemFld, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(_maxNumCoursesDuringThesisFld, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, _inputsPanelLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(_feasCheckBtn)))))))
                .addContainerGap())
        );
        _inputsPanelLayout.setVerticalGroup(
            _inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_inputsPanelLayout.createSequentialGroup()
                .addComponent(_inputsPanelLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(_passedCourseSelectorLbl)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(_inputsPanelLayout.createSequentialGroup()
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(_inputsPanelLayout.createSequentialGroup()
                                .addComponent(_shortestComplTimeBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(_diffiBalanceBtn))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(_honorStudentChkBox)
                            .addComponent(jLabel4)
                            .addComponent(_maxNumCrsPerSemFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(_summerSemestersOffChkBox)
                            .addComponent(jLabel7)
                            .addComponent(_maxNumCoursesDuringThesisFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(_s1ChkBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(_s2ChkBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(_stChkBox)
                            .addComponent(_feasCheckBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addGroup(_inputsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(_runBtn)
                            .addComponent(_curDateTxtFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(_curDateLbl)))
                    .addComponent(jScrollPane5)))
        );

        _fileMenu.setText("File");

        _loadMenuItem.setText("Load Courses");
        _loadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _loadMenuItemActionPerformed(evt);
            }
        });
        _fileMenu.add(_loadMenuItem);

        _saveCoursesMenuItem.setText("Save Courses");
        _saveCoursesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveCoursesMenuItemActionPerformed(evt);
            }
        });
        _fileMenu.add(_saveCoursesMenuItem);

        _saveScheduleMenuItem.setText("Save Schedule...");
        _saveScheduleMenuItem.setEnabled(false);
        _saveScheduleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveScheduleMenuItemActionPerformed(evt);
            }
        });
        _fileMenu.add(_saveScheduleMenuItem);

        _exitMenuItem.setText("Exit");
        _exitMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                _exitMenuItemMousePressed(evt);
            }
        });
        _fileMenu.add(_exitMenuItem);

        _menuBar.add(_fileMenu);

        _editMenu.setText("Edit");

        _editCoursesMenuItem.setText("Edit Courses");
        _editCoursesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _editCoursesMenuItemActionPerformed(evt);
            }
        });
        _editMenu.add(_editCoursesMenuItem);

        _editCourseGroupsMenuItem.setText("Edit Group Constraints");
        _editCourseGroupsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _editCourseGroupsMenuItemActionPerformed(evt);
            }
        });
        _editMenu.add(_editCourseGroupsMenuItem);

        _menuBar.add(_editMenu);

        setJMenuBar(_menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(_outputsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(_inputsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(_inputsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(_outputsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void _exitMenuItemMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event__exitMenuItemMousePressed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event__exitMenuItemMousePressed

    
    private void _runBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__runBtnActionPerformed
        // create the MIP model, and then execute the GUROBI or SCIP optimizer
        // to solve the model and write the results to the output area. During
        // this time, the GUI will be un-responsive.
        // before anything else, set current date
        String cur_date = this._curDateTxtFld.getText();
        String[] cds = cur_date.split("/");
        CurrentDate._curDay = Integer.parseInt(cds[0]);
        CurrentDate._curMonth = Integer.parseInt(cds[1]);
        CurrentDate._curYear = Integer.parseInt(cds[2]);
        final int Smax = _miphdlr.getScheduleParams().getSmax();
        if (!_feasChkAsked)
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // first, get the passed courses and the desired courses from the JList
        // objects
        int[] sel_passed = this._passedCoursesList.getSelectedIndices();
        Set<String> passed_codes = new HashSet<>();
        for (int i=0; i<sel_passed.length; i++) {
            Course ci = (Course) this._classListModel.elementAt(sel_passed[i]);
            passed_codes.add(ci.getCode());
        }
        int[] sel_desired = this._desiredCoursesList.getSelectedIndices();
        Set<String> desired_codes = new HashSet<>();
        for (int i=0; i<sel_desired.length; i++) {
            CodeNameAllowedTerms cnati = (CodeNameAllowedTerms) 
                    this._itcClassListModel.elementAt(sel_desired[i]);
            if (cnati._allowedTerms!=null && cnati._allowedTerms.length()>1)
                desired_codes.add(cnati._code+";"+cnati._allowedTerms);
            else desired_codes.add(cnati._code+";");  // course is NOT to take
        }
        boolean isHonor = this._honorStudentChkBox.isSelected();
        boolean stoff = this._stChkBox.isSelected();
        boolean s1off = this._s1ChkBox.isSelected();
        boolean s2off = this._s2ChkBox.isSelected();
        String concentration_name = "";
        int conc_sel_ind = this._concNamesList.getSelectedIndex();
        if (conc_sel_ind>=0) 
            concentration_name = this._concNamesList.getSelectedValue();
        else {
            JOptionPane.showConfirmDialog(null, 
                                          "You must select "+
                                          "a concentration area first");
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        int max_crs_per_sem = Integer.MAX_VALUE;
        try {
            max_crs_per_sem = 
                    Integer.parseInt(this._maxNumCrsPerSemFld.getText());
        }
        catch (NumberFormatException e) {
            System.err.println("couldn't parse "+
                               this._maxNumCrsPerSemFld.getText()+
                               " will stay at Integer.MAX_VALUE instead");
        }
        int max_num_courses_dur_thesis = 1;
        try {
            max_num_courses_dur_thesis = 
                    Integer.parseInt(
                              this._maxNumCoursesDuringThesisFld.getText());
        }
        catch (NumberFormatException e) {
            System.err.println("couldn't parse "+
                               this._maxNumCrsPerSemFld.getText()+
                               " will stay at Integer.MAX_VALUE instead");
        }
        String schedfile = null;
        String group2ignore = "NONE";
        if (this._shortestComplTimeBtn.isSelected()) {
            int DN = 1000; int DL = 100; int Cr = 1; int Gr = 10;
            // if we are in feasibility check mode, make sure the 1st term
            // only has the courses the student has indicated as desired 
            // for the upcoming term; also ignore any soft-order constraints
            if (this._feasChkAsked) {
                String first_term_name = Course.getTermNameByTermNo(1);
                DesiredCourses des_courses = new DesiredCourses();
                des_courses.addAll(desired_codes);
                int num_crs_4_next_term = 
                    des_courses.getNumDesiredCoursesForTerm(first_term_name);
                _numCoursesPerTerm2StrMap.put(1, 
                                              new Integer(num_crs_4_next_term).
                                                      toString());
                DN = 0; DL = 1; Cr = 0; Gr = 0;
                group2ignore = "softorder";
            } else {  // itc20260216: correct map after a Feas.Chk request
                // not a Feas. Chk, so restore the _numCoursesPerTerm2StrMap[1]
                JTextField txtFld = 
                        (JTextField) this._numCoursesPerTerm2FldMap.get(1);
                if (txtFld!=null) {
                    String txt = txtFld.getText();
                    if (txt!=null && txt.length()>0)
                        _numCoursesPerTerm2StrMap.put(1, txt);
                    else _numCoursesPerTerm2StrMap.put(1, "");
                }
                else _numCoursesPerTerm2StrMap.put(1, "");
            }
            schedfile = _miphdlr.createMIPFile(isHonor, 
                                               max_crs_per_sem, 
                                               max_num_courses_dur_thesis,
                                               s1off, s2off, stoff, 
                                               _numCoursesPerTerm2StrMap, 
                                               passed_codes, 
                                               _passed_OU_in_cur_academic_year,
                                               desired_codes, 
                                               concentration_name,
                                               DN, DL, Cr, Gr, group2ignore);
        }
        else if (this._diffiBalanceBtn.isSelected()) {
            int DN = 1; int DL = 100; int Cr = 10; int Gr = 1000;
            // if we are in feasibility check mode, make sure the 1st term
            // only has the courses the student has indicated as desired 
            // for the upcoming term
            if (this._feasChkAsked) {
                String first_term_name = Course.getTermNameByTermNo(1);
                DesiredCourses des_courses = new DesiredCourses();
                des_courses.addAll(desired_codes);
                int num_crs_4_next_term = 
                    des_courses.getNumDesiredCoursesForTerm(first_term_name);
                _numCoursesPerTerm2StrMap.put(1, 
                                              new Integer(num_crs_4_next_term).
                                                      toString());
                DN = 0; DL = 1; Cr = 0; Gr = 0;
                group2ignore = "softorder";
            }
            schedfile = _miphdlr.createMIPFile(isHonor, 
                                               max_crs_per_sem, 
                                               max_num_courses_dur_thesis,
                                               s1off, s2off, stoff, 
                                               _numCoursesPerTerm2StrMap,
                                               passed_codes, 
                                               _passed_OU_in_cur_academic_year,
                                               desired_codes, 
                                               concentration_name,
                                               DN, DL, Cr, Gr, 
                                               group2ignore);            
        }
        this._outputsArea.setText(schedfile+" created.\nNow running OPTIMIZER");
        String result = null;
        try {
            //final String program_code = 
            //        _miphdlr.getScheduleParams().getProgramCode();
            result = _miphdlr.optimizeSchedule(schedfile);
            if (this._feasChkAsked) {
                // don't write in the Outputs area the entire schedule
                if (!result.startsWith("FAILURE")) {
                    result = "Schedule feasibility OK!!!";
                }
                else {  
                    // see if any single group of constraints is responsible
                    // also check some (not all) common group combinations
                    String[] groups_2_ignore = 
                      {"PREREQS", "COREQS", 
                       "L-5", "OTHER_L-5",
                       "PREREQS L-5 OTHER_L-5",  // check L5 + PREREQS
                       "L-6_L-4", "L-6_L-5", 
                       "L-6_L-4 L-6_L-5",  // check both L6 
                       "PREREQS L-6_L-4 L-6_L-5",  // check both L6 + PREREQS
                       "PREREQS L-6_L-4 L-6_L-5 summer",  // add summers-off too
                       "CREDIT", "LE_UPPER", 
                       "TERM_CREDIT", "FRESHMAN", "TERM_DESIRE", "SUMMER", 
                       "desired", "summer", "capstone", "THESIS", 
                       "HonorGroup", "OU", "course_avail"};
                    int DN = 0; int DL = 1; int Cr = 0; int Gr = 0;
                    boolean cont = true;
                    for (String group_constr : groups_2_ignore) {
                        String group_constr2 = 
                                group_constr + " softorder";
                        schedfile = _miphdlr.createMIPFile(isHonor, 
                                               max_crs_per_sem, 
                                               max_num_courses_dur_thesis,
                                               s1off, s2off, stoff, 
                                               _numCoursesPerTerm2StrMap,
                                               passed_codes, 
                                               _passed_OU_in_cur_academic_year,
                                               desired_codes, 
                                               concentration_name,
                                               DN, DL, Cr, Gr, group_constr2);
                        String res = _miphdlr.optimizeSchedule(schedfile);
                        if (!res.startsWith("FAILURE")) {
                            result = 
                              "INFEASIBLE PLAN; RESPONSIBLE CONSTRAINT GROUP=" +
                              group_constr;
                            cont = false;
                            break;
                        }
                    }
                    // do the same for the group-constraints
                    Iterator<String> gnamesit = 
                            CourseGroup.getCourseGroupNameIterator();
                    while (cont && gnamesit.hasNext()) {
                        String group_constr = gnamesit.next();
                        String group_constr2 = group_constr + " softorder";
                        schedfile = _miphdlr.createMIPFile(isHonor, 
                                               max_crs_per_sem, 
                                               max_num_courses_dur_thesis,
                                               s1off, s2off, stoff, 
                                               _numCoursesPerTerm2StrMap,
                                               passed_codes, 
                                               _passed_OU_in_cur_academic_year,
                                               desired_codes, 
                                               concentration_name,
                                               DN, DL, Cr, Gr, group_constr2);
                        String res = _miphdlr.optimizeSchedule(schedfile);
                        if (!res.startsWith("FAILURE")) {
                            result = 
                              "INFEASIBLE PLAN; RESPONSIBLE CONSTRAINT GROUP=" +
                              group_constr;
                            break;
                        }
                    }
                }
            }
            // write result to output editor-pane too
            HashMap<Integer, Integer> solnmap = 
                    _miphdlr.getLastOptimalSolution();
            this._outputTextPane.setText("");  // reset the output text pane
            _varTermsMap.clear();
            _numCoursesPerTerm2FldMap.clear();
            
            if (!this._feasChkAsked) {
                StyledDocument doc = this._outputTextPane.getStyledDocument();
                SimpleAttributeSet attr = new SimpleAttributeSet();
                /* below is example code for adding widgets in JTextPane
                for (String dat : data ) {
                    doc.insertString(doc.getLength(), dat, attr );
                    tp.setCaretPosition(tp.getDocument().getLength());
                    tp.insertComponent(new JButton("Click"));
                    doc.insertString(doc.getLength(), "\n", attr );
                }
                setLocationRelativeTo(null);
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setVisible(true);            
                */            
                // write courses per semester
                Iterator<Integer> vid_it = solnmap.keySet().iterator();
                // find tno_max for iteration over terms
                int tno_max = 0;
                // we need tree-map to have the keys in sorted asc order
                TreeMap<Integer, Set<Integer>> crss_by_trm_map = new TreeMap<>();
                while (vid_it.hasNext()) {
                    int vid = vid_it.next();
                    int tno = solnmap.get(vid);
                    if (tno > tno_max) tno_max = tno;
                    if (tno<=0) continue;  // don't show courses already taken
                    Set<Integer> crs = crss_by_trm_map.get(tno);
                    if (crs==null) {
                        crs = new HashSet<>();
                        crss_by_trm_map.put(tno, crs);
                    }
                    crs.add(vid);
                }
                Iterator<Integer> term_it = crss_by_trm_map.keySet().iterator();
                //while (term_it.hasNext()) {
                //    int tno = term_it.next();
                for (int tno=1; tno<=tno_max; ++tno) {
                    String tname = Course.getTermNameByTermNo(tno);
                    doc.insertString(doc.getLength(), "--- "+tname+" --- ", attr);
                    doc.insertString(doc.getLength(), " #Courses for Term: ", attr);
                    this._outputTextPane.setCaretPosition(this._outputTextPane.
                                                              getDocument().
                                                                  getLength());
                    JTextField tfld2 = new JTextField("");
                    tfld2.setToolTipText(
                                "Enter #Courses constraint for this term "+
                                "eg '<=3' or '2'");
                    // show constraint value if there exists one
                    if (_numCoursesPerTerm2StrMap.containsKey(tno)) {
                        tfld2.setText(_numCoursesPerTerm2StrMap.get(tno));
                    }
                    _numCoursesPerTerm2FldMap.put(tno, tfld2);
                    this._outputTextPane.insertComponent(tfld2);
                    doc.insertString(doc.getLength(), "\n", attr);

                    Set<Integer> cids = crss_by_trm_map.get(tno);
                    if (cids==null) cids = new HashSet<>();  // cannot be null
                    for (int cid : cids) {
                        Course c = Course.getCourseById(cid);
                        // ignore courses that are not ITC or MATH
                        // this can be modeled by querying if the course belongs
                        // to a particular CourseGroup, say the 
                        // "EditableTimeCoursesGroup", but it'd be a lot of work to
                        // create such group file containing all ITC and MA courses.
                        // below, we use just the program-code string
                        //if (!c.getCode().startsWith(program_code)) continue;
                        String term = Course.getTermNameByTermNo(tno);
                        String info = c.getCode()+" "+c.getName()+" Prefer Terms: ";
                        doc.insertString(doc.getLength(), info, attr);
                        this._outputTextPane.setCaretPosition(this._outputTextPane.
                                                                getDocument().
                                                                  getLength());
                        JTextField tfld = new JTextField("");
                        tfld.setToolTipText(
                                "Enter terms to allow separated by space or '-' to"+
                                " indicate undesired course or 'allotherterms' to"+
                                " indicate any other term OK; eg 'FA2022 SP2023'");
                        _varTermsMap.put(cid, tfld);
                        this._outputTextPane.insertComponent(tfld);
                        doc.insertString(doc.getLength(), "\n", attr);
                    }
                }
                JButton btn = new JButton("Change Terms");
                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent c) {
                        _numCoursesPerTerm2StrMap.clear();
                        Iterator<Integer> tit = 
                            _numCoursesPerTerm2FldMap.keySet().iterator();
                        while (tit.hasNext()) {
                            int tno = tit.next();
                            JTextField tfld = _numCoursesPerTerm2FldMap.get(tno);
                            _numCoursesPerTerm2StrMap.put(tno, tfld.getText());
                        }
                        Iterator<Integer> vit = _varTermsMap.keySet().iterator();
                        List<Integer> sel_inds = new ArrayList<>();
                        while (vit.hasNext()) {
                            int vid = vit.next();
                            int cur_termno = 
                                    _miphdlr.getLastOptimalSolution().get(vid);
                            JTextField vfld = _varTermsMap.get(vid);
                            if (vfld.getText().length()>0) {
                                String terms = vfld.getText().trim();
                                if (terms.length()>0) {
                                    Course cv = Course.getCourseById(vid);
                                    if ("-".equals(terms)) {  // undesired course
                                        terms = "";
                                    }
                                    else if (!CodeNameAllowedTerms.
                                                 prefferedTermsAllowed(cv.getCode(), 
                                                                       terms,
                                                                       cur_termno,
                                                                       Smax)){
                                        terms = "";  // indicates course is not 
                                                     // offered during terms
                                    }
                                    // search to find where in _itcClassListModel
                                    // is the given course; if not found (LE course)
                                    // add it to the model
                                    int sz = _itcClassListModel.getSize();
                                    boolean found = false;
                                    for (int i=0; i<sz; i++) {
                                        CodeNameAllowedTerms mi = 
                                                (CodeNameAllowedTerms) 
                                                  _itcClassListModel.get(i);
                                        if (mi._code.equals(cv.getCode())) {
                                            CodeNameAllowedTerms new_cnat = 
                                                    new CodeNameAllowedTerms(
                                                            cv.getCode(), 
                                                            cv.getName(), 
                                                            terms);
                                            _itcClassListModel.set(i, new_cnat);
                                            sel_inds.add(i);
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {  // course not in major program
                                            CodeNameAllowedTerms new_cnat = 
                                                    new CodeNameAllowedTerms(
                                                            cv.getCode(), 
                                                            cv.getName(), 
                                                            terms);                                    
                                        _itcClassListModel.addElement(new_cnat);
                                        sel_inds.add(_itcClassListModel.size()-1);
                                    }
                                }
                            }
                        }
                        // highlight also the indices for courses to change in 
                        // _desiredCoursesList
                        int[] indices = _desiredCoursesList.getSelectedIndices();
                        for (int ind : indices) {
                            if (!sel_inds.contains(ind)) sel_inds.add(ind);
                        }
                        indices = new int[sel_inds.size()];
                        for (int i=0; i<indices.length; i++) 
                            indices[i] = sel_inds.get(i);
                        _desiredCoursesList.setSelectedIndices(indices);
                    }
                });
                this._outputTextPane.insertComponent(btn);
            }
            // include the "Reset Desired..." no matter what
            JButton btn2 = new JButton("Reset Desired Courses/Terms");
            btn2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent c) {
                    int sz = _itcClassListModel.getSize();
                    for (int i=0; i<sz; i++) {
                        CodeNameAllowedTerms mi = 
                            (CodeNameAllowedTerms) _itcClassListModel.get(i);
                        CodeNameAllowedTerms new_cnat = 
                            new CodeNameAllowedTerms(mi._code, 
                                                     mi._title, 
                                                     "allterms");
                        _itcClassListModel.set(i, new_cnat);
                    }
                    _desiredCoursesList.clearSelection();
                }   
                });
            this._outputTextPane.insertComponent(btn2);
        }
        catch (GRBException e) {
            result = "GUROBI threw GRBException: "+e.getLocalizedMessage();
            result += "\nMIP program should be in file ./"+schedfile;
        }
        catch(IOException e) {
            result = "Printing into file ./"+schedfile+".result_vars.out fail?";            
        }
        catch (Exception e) {
            result = "oops...";
            e.printStackTrace();
        }
        this._outputsArea.setText(result);
        // now enable menu items as well
        this._saveScheduleMenuItem.setEnabled(true);
        // done, reset the cursor to normal
        if (!_feasChkAsked)
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event__runBtnActionPerformed

    
    private void _shortestComplTimeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__shortestComplTimeBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event__shortestComplTimeBtnActionPerformed

    
    private void _summerSemestersOffChkBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__summerSemestersOffChkBoxActionPerformed
        if (this._summerSemestersOffChkBox.isSelected()) {
            this._s1ChkBox.setSelected(true);
            this._s2ChkBox.setSelected(true);
            this._stChkBox.setSelected(true);
        }
        else {
            this._s1ChkBox.setSelected(false);
            this._s2ChkBox.setSelected(false);
            this._stChkBox.setSelected(false);            
        }
    }//GEN-LAST:event__summerSemestersOffChkBoxActionPerformed

    
    private void _curDateTxtFldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__curDateTxtFldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event__curDateTxtFldActionPerformed

    
    private void _saveScheduleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__saveScheduleMenuItemActionPerformed
        // Open a JFileDialogue to choose a file to save the contexts of the 
        // output area
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File("."));
        int res = jfc.showSaveDialog(null);
        if (res==JFileChooser.APPROVE_OPTION) {
            File sel_file = jfc.getSelectedFile();
            try(PrintWriter pw = new PrintWriter(new FileWriter(sel_file))) {
                pw.print(this._outputsArea.getText());
                pw.flush();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event__saveScheduleMenuItemActionPerformed

    
    private void _saveCoursesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__saveCoursesMenuItemActionPerformed
        // save the selected passed courses in file "passedcourses_stname.txt".
        boolean ok = true;
        String passedcoursesfilename = "passedcourses_"+_studentName+".txt";
        try(PrintWriter pw = 
                new PrintWriter(new FileWriter(passedcoursesfilename))) {
            int[] sel_passed_courses = 
                    this._passedCoursesList.getSelectedIndices();
            for (int i : sel_passed_courses) {
                Course ci = (Course) this._classListModel.get(i);
                pw.println(ci.getCode());
            }
            pw.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
            ok = false;
            JOptionPane.showConfirmDialog(null, 
                                          "failed to save passed courses");
        }
        // save the selected desired courses in file "desiredcourses_stname.txt"
        String desiredcoursesfilename = "desiredcourses_"+_studentName+".txt";
        try(PrintWriter pw = 
                new PrintWriter(new FileWriter(desiredcoursesfilename))) {
            int[] sel_desired_courses = 
                    this._desiredCoursesList.getSelectedIndices();
            for (int i : sel_desired_courses) {
                CodeNameAllowedTerms ci = 
                        (CodeNameAllowedTerms) this._itcClassListModel.get(i);
                pw.println(ci._code+";"+ci._allowedTerms);
            }
            pw.flush();
            if (ok) {
                JOptionPane.showConfirmDialog(null, "selections saved in "+
                                                    passedcoursesfilename+
                                                    " and "+
                                                    desiredcoursesfilename+
                                                    " files");
            }
            else JOptionPane.showConfirmDialog(null, "saved desired courses "+
                                                     " in "+
                                                     desiredcoursesfilename);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, 
                                          "failed to save desired courses");
        }
    }//GEN-LAST:event__saveCoursesMenuItemActionPerformed

    
    private void _loadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__loadMenuItemActionPerformed
        // load the passed courses from file "passedcourses_<studentname>.txt" 
        // if such file exists
        final int Smax = _miphdlr.getScheduleParams().getSmax();
        String pfname = "passedcourses_"+_studentName+".txt";
        File pf = new File(pfname);
        if (pf.exists()) {
            PassedCourses passed = new PassedCourses();
            passed.readPassedCoursesFromFile(pfname);
            List<Integer> l = new ArrayList<>();
            for (int i=0; i<_classListModel.getSize(); i++) {
                Course ci = (Course) _classListModel.elementAt(i);
                if (passed.contains(ci.getCode())) {
                    l.add(i);
                }
            }
            if (l.size()>0) {
                int[] larr = new int[l.size()];
                int i=0;
                for (Integer ii : l) larr[i++] = ii;
                _passedCoursesList.setSelectedIndices(larr);
            }
        }
        else {
            JOptionPane.showConfirmDialog(null, 
                                          "couldn't find "+pfname+" file");
        }
        // load desired courses from file "desiredcourses_<studentname.txt" if 
        // such file exists
        String dfname = "desiredcourses_"+_studentName+".txt";
        File df = new File(dfname);
        _desiredCoursesList.clearSelection();
        if (df.exists()) {
            DesiredCourses d = new DesiredCourses();
            d.readDesiredCoursesFromFile(dfname);
            List<Integer> sis = new ArrayList<>();
            for (int i=0; i<_itcClassListModel.size(); i++) {
                CodeNameAllowedTerms ci = 
                        (CodeNameAllowedTerms) _itcClassListModel.elementAt(i);
                if (d.contains(ci._code)) {
                    CodeNameAllowedTerms cnat = 
                            d.getCodeNameAllowedTerms(ci._code, Smax);
                    _itcClassListModel.setElementAt(cnat, i);
                    sis.add(i);
                }
            }
            if (sis.size()>0) {
                int[] sisA = new int[sis.size()];
                for (int i=0; i<sis.size(); i++) sisA[i] = sis.get(i);
                _desiredCoursesList.setSelectedIndices(sisA);
            }
        }
        else {
            JOptionPane.showConfirmDialog(null, 
                                          "couldn't find "+dfname+" file");
        }
    }//GEN-LAST:event__loadMenuItemActionPerformed

    
    private void _maxNumCrsPerSemFldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__maxNumCrsPerSemFldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event__maxNumCrsPerSemFldActionPerformed

    
    /**
     * invoke the <CODE>CourseEditor</CODE> to edit courses, and then re-load
     * the edited courses. The editor runs in a new thread to avoid "GUI freeze"
     * issues. Upon editor closing, the program exits.
     * @param evt 
     */
    private void _editCoursesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__editCoursesMenuItemActionPerformed
        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int res = JOptionPane.
                          showConfirmDialog(null, "Proceeding will terminate "+
                                                  "the main SCORER GUI...");
            if (res == JOptionPane.YES_OPTION) {
                Thread course_edit_thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Runtime rt = Runtime.getRuntime();
                            Process p = 
                                rt.exec("java -cp "+
                                        "./dist/ITStudentCourseScheduling.jar"+
                                        " edu.acg.itss.CourseEditor "+
                                        MainGUI._dir2Files);
                            // int retval = p.waitFor();  // wait until CourseEditor terminates
                            System.exit(0);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                course_edit_thread.start();
                // course_edit_thread.join();  // wait for thread to terminate
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "CourseEditor failed to run.");
        }
        finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));            
        }
    }//GEN-LAST:event__editCoursesMenuItemActionPerformed

    
    /**
     * invoke the <CODE>CourseGroupEditor</CODE> to edit course groups (that 
     * specify constraints for the study plan) in a new thread. Upon starting
     * the new thread, the main GUI terminates.
     * @param evt 
     */
    private void _editCourseGroupsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__editCourseGroupsMenuItemActionPerformed
        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int res = JOptionPane.
                          showConfirmDialog(null, "Proceeding will terminate "+
                                                  "the main SCORER GUI...");
            if (res == JOptionPane.YES_OPTION) {
                Thread course_group_edit_thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Runtime rt = Runtime.getRuntime();
                            Process p = 
                                rt.exec("java -cp "+
                                        "./dist/ITStudentCourseScheduling.jar"+
                                        " edu.acg.itss.CourseGroupEditor "+
                                        MainGUI._dir2Files);
                            // int retval = p.waitFor();  // wait until CourseGroupEditor terminates
                            System.exit(0);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                course_group_edit_thread.start();
                // course_group_edit_thread.join();  // wait for thread to terminate
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "CourseGroupEditor failed to run.");
        }
        finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));            
        }
    }//GEN-LAST:event__editCourseGroupsMenuItemActionPerformed

    private void _feasCheckBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__feasCheckBtnActionPerformed
        _feasChkAsked = true;  // indicate we are asking for a feas. check
        // call the method for obtaining the optimal schedule if it exists
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        _runBtnActionPerformed(evt);
        this.setCursor(Cursor.getDefaultCursor());
        _feasChkAsked = false;  // reset the boolean indicator of feas. checks
    }//GEN-LAST:event__feasCheckBtnActionPerformed

    private void _honorStudentChkBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__honorStudentChkBoxActionPerformed
        // increment number of courses to take when student is honor student
        if (_honorStudentChkBox.isSelected()) {  
            String thesisMaxNumCourses = 
                    _maxNumCoursesDuringThesisFld.getText();
            if (thesisMaxNumCourses.length()==0)
                _maxNumCoursesDuringThesisFld.setText("2");
            else {
                int num = Integer.parseInt(thesisMaxNumCourses);
                String s = new Integer(num+1).toString();
                _maxNumCoursesDuringThesisFld.setText(s);
            }
        }
        else {
            // decrement number of courses to take when student is NOT honor
            String thesisMaxNumCourses = 
                    _maxNumCoursesDuringThesisFld.getText();
            if (thesisMaxNumCourses.length()==0)
                _maxNumCoursesDuringThesisFld.setText("1");
            else {
                int num = Integer.parseInt(thesisMaxNumCourses);
                String s = new Integer(num-1).toString();
                if (num>1) _maxNumCoursesDuringThesisFld.setText(s);
            }
        }
    }//GEN-LAST:event__honorStudentChkBoxActionPerformed

    
    /**
     * main class to start the ACG SCORER app.
     * @param args the command line arguments must include as first argument
     * the name of the directory relative to the current directory where all
     * needed files (params.props, *.grp, and cls.csv) are to be found.
     */
    public static void main(String args[]) {
        MainGUI._dir2Files = args[0];
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            /*
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            */
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> _concNamesList;
    private javax.swing.JLabel _curDateLbl;
    private javax.swing.JTextField _curDateTxtFld;
    private javax.swing.JList<String> _desiredCoursesList;
    private javax.swing.JRadioButton _diffiBalanceBtn;
    private javax.swing.JMenuItem _editCourseGroupsMenuItem;
    private javax.swing.JMenuItem _editCoursesMenuItem;
    private javax.swing.JMenu _editMenu;
    private javax.swing.JMenuItem _exitMenuItem;
    private javax.swing.JButton _feasCheckBtn;
    private javax.swing.JMenu _fileMenu;
    private javax.swing.JRadioButton _honorStudentChkBox;
    private javax.swing.JPanel _inputsPanel;
    private javax.swing.JLabel _inputsPanelLbl;
    private javax.swing.JMenuItem _loadMenuItem;
    private javax.swing.JTextField _maxNumCoursesDuringThesisFld;
    private javax.swing.JTextField _maxNumCrsPerSemFld;
    private javax.swing.JMenuBar _menuBar;
    private javax.swing.JScrollPane _outputAreaScrollPane;
    private javax.swing.JTextPane _outputTextPane;
    private javax.swing.JTextArea _outputsArea;
    private javax.swing.JPanel _outputsPanel;
    private javax.swing.JLabel _outputsPanelLbl;
    private javax.swing.JLabel _passedCourseSelectorLbl;
    private javax.swing.JList<String> _passedCoursesList;
    private javax.swing.JButton _runBtn;
    private javax.swing.JCheckBox _s1ChkBox;
    private javax.swing.JCheckBox _s2ChkBox;
    private javax.swing.JMenuItem _saveCoursesMenuItem;
    private javax.swing.JMenuItem _saveScheduleMenuItem;
    private javax.swing.JRadioButton _shortestComplTimeBtn;
    private javax.swing.JCheckBox _stChkBox;
    private javax.swing.JRadioButton _summerSemestersOffChkBox;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    // End of variables declaration//GEN-END:variables
}

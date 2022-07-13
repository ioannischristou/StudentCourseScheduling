package edu.acg.itss;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import javax.swing.JOptionPane;


/**
 * class responsible for presenting a GUI for editing courses in the "cls.csv"
 * main file living in a sub-directory of the root app directory, specified in
 * the cmd-line of this program. The program presents at any time a form for a 
 * particular course details. Any changes the user makes to this form won't be 
 * saved in memory unless the user presses the "Save Course Changes" button. 
 * Even so, no changes will be written to disk until the user presses the 
 * "Save to Disk" button, which essentially "commits" all changes made to 
 * courses so far.
 * @author itc
 */
public class CourseEditor extends javax.swing.JFrame {

    private static String _dir2Files = null;
    
    private ScheduleParams _params;
    
    
    /**
     * Creates new form CourseEditor.
     */
    public CourseEditor() {
        initComponents();
        readData();
    }

    
    /**
     * reads data from the "cls.csv" and "params.props" files living in user
     * specified directories, to populate the <CODE>Course</CODE> class.
     */
    private void readData() {
        LocalDate now = LocalDate.now();
        int cur_day = now.getDayOfMonth();
        int cur_mon = now.getMonthValue();
        int cur_year = now.getYear();
        CurrentDate._curDay = cur_day;
        CurrentDate._curMonth = cur_mon;
        CurrentDate._curYear = cur_year;
        _params = new ScheduleParams(_dir2Files+"/params.props");
        Course.readAllCoursesFromFile(_dir2Files+"/cls.csv", _params.getSmax());
        // populate fields in form
        if (Course.getNumCourses()>0) {
            Course c = Course.getCourseById(0);
            populateForm(c);
            this._previousCrsBtn.setEnabled(false);
            boolean b = Course.getNumCourses()>1;
            this._nextCrsBtn.setEnabled(b);
            this._deleteCrsBtn.setEnabled(true);
            this._saveCrsBtn.setEnabled(true);
            this._saveAllCrssBtn.setEnabled(true);
            this._addNewCrsBtn.setEnabled(true);
        } else {
            // disable all buttons except "Add New Course"
            this._previousCrsBtn.setEnabled(false);
            this._nextCrsBtn.setEnabled(false);
            this._deleteCrsBtn.setEnabled(false);
            this._saveCrsBtn.setEnabled(false);
            this._saveAllCrssBtn.setEnabled(false);
            this._addNewCrsBtn.setEnabled(true);
        }
    }

    
    /**
     * populates the fields of the form with values from given Course object.
     * @param c Course
     */
    private void populateForm(Course c) {
        this._idLbl.setText(Integer.toString(c.getId()));
        this._codeFld.setText(c.getCode());
        String aka = "";
        for (String s : c.getSynonymCodes()) {
            aka += s+" ";
        }
        this._synonymsFld.setText(aka.trim());
        this._titleFld.setText(c.getName());
        this._creditsFld.setText(Integer.toString(c.getCredits()));
        this._diffLvlFld.setText(Integer.toString(c.getDifficultyLevel()));
        Set<Set<String>> prereqs = c.getPrereqs();
        String pstr = "";
        Iterator<Set<String>> pit = prereqs.iterator();
        while (pit.hasNext()) {
            Set<String> ps = pit.next();
            Iterator<String> ps_it = ps.iterator();
            while (ps_it.hasNext()) {
                pstr += ps_it.next();
                if (ps_it.hasNext()) pstr += "+";
            }
            if (pit.hasNext()) pstr += ",";
        }
        this._prereqsFld.setText(pstr);
        Set<String> coreqs = c.getCoreqs();
        String cstr = "";
        for (String cs : coreqs) {
            cstr += cs+" ";
        }
        this._coreqsFld.setText(cstr.trim());
        this._displayNameFld.setText(c.getScheduleDisplayName());
        String to = "";
        List<Integer> tolst = c.getTermsOffered(_params.getSmax());
        for (Integer t : tolst) {
            String tstr = Course.getTermNameByTermNo(t);
            to += tstr + " ";
        }
        this._termsOfferedFld.setText(to.trim());    
        if (c.getId()==0) this._previousCrsBtn.setEnabled(false);
        else this._previousCrsBtn.setEnabled(true);
        if (c.getId()==Course.getNumCourses()-1) 
            this._nextCrsBtn.setEnabled(false);
        else this._nextCrsBtn.setEnabled(true);
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        _mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        _codeFld = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        _synonymsFld = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        _titleFld = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        _creditsFld = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        _diffLvlFld = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        _prereqsFld = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        _coreqsFld = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        _displayNameFld = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        _termsOfferedFld = new javax.swing.JTextField();
        _deleteCrsBtn = new javax.swing.JButton();
        _addNewCrsBtn = new javax.swing.JButton();
        _saveAllCrssBtn = new javax.swing.JButton();
        _saveCrsBtn = new javax.swing.JButton();
        _searchBtn = new javax.swing.JButton();
        _previousCrsBtn = new javax.swing.JButton();
        _nextCrsBtn = new javax.swing.JButton();
        _idLbl = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        _findAllDescendantsBtn = new javax.swing.JButton();
        _showAllPrereqsBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("ID:");

        jLabel2.setText("Course Code:");

        jLabel3.setText("Synonyms:");

        _synonymsFld.setText("aka");
        _synonymsFld.setToolTipText("Enter synonym course codes (eg ITC3160 used to be ITC3260) etc.");

        jLabel4.setText("Title:");

        jLabel5.setText("#credits:");

        _creditsFld.setText("3");

        jLabel6.setText("Difficulty Lvl:");

        _diffLvlFld.setText("0");
        _diffLvlFld.setToolTipText("level 0 is the normal difficulty level, anything above denotes unusual difficulty");
        _diffLvlFld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _diffLvlFldActionPerformed(evt);
            }
        });

        jLabel7.setText("Pre-requisites:");

        _prereqsFld.setToolTipText("Enter prerequisite codes in CNF eg. \"MA2010,ITC1070+CS1070\"");

        jLabel8.setText("Co-requisites:");

        _coreqsFld.setToolTipText("Enter list of co-requisites as: \"MA2010,ITC1070\"");

        jLabel9.setText("Course Display Name:");

        _displayNameFld.setText("-");
        _displayNameFld.setToolTipText("Enter co-requisite display title such as \"LE in humanities\" or \"-\"");

        jLabel10.setText("Terms When Offered:");

        _termsOfferedFld.setText("alltimes");
        _termsOfferedFld.setToolTipText("enter terms as \"FA2022 SP2023\" or everyfall, everyspring, next<2|4>terms, alltimes. Enter \"-\" if not offered");

        _deleteCrsBtn.setText("Delete This Course");
        _deleteCrsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _deleteCrsBtnActionPerformed(evt);
            }
        });

        _addNewCrsBtn.setText("Add New Course");
        _addNewCrsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _addNewCrsBtnActionPerformed(evt);
            }
        });

        _saveAllCrssBtn.setText("Save to Disk");
        _saveAllCrssBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveAllCrssBtnActionPerformed(evt);
            }
        });

        _saveCrsBtn.setText("Save Course Changes");
        _saveCrsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveCrsBtnActionPerformed(evt);
            }
        });

        _searchBtn.setText("Search");
        _searchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _searchBtnActionPerformed(evt);
            }
        });

        _previousCrsBtn.setText("Previous Course");
        _previousCrsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _previousCrsBtnActionPerformed(evt);
            }
        });

        _nextCrsBtn.setText("Next Course");
        _nextCrsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _nextCrsBtnActionPerformed(evt);
            }
        });

        _idLbl.setText("---");

        jLabel11.setText("Show All Courses Having this as Pre-req:");

        _findAllDescendantsBtn.setText("Show");
        _findAllDescendantsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _findAllDescendantsBtnActionPerformed(evt);
            }
        });

        _showAllPrereqsBtn.setText("Show All Prereqs");
        _showAllPrereqsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _showAllPrereqsBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout _mainPanelLayout = new javax.swing.GroupLayout(_mainPanel);
        _mainPanel.setLayout(_mainPanelLayout);
        _mainPanelLayout.setHorizontalGroup(
            _mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(_mainPanelLayout.createSequentialGroup()
                        .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel8)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5)
                            .addComponent(jLabel4)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(_synonymsFld)
                            .addComponent(_titleFld)
                            .addComponent(_coreqsFld)
                            .addComponent(_displayNameFld)
                            .addComponent(_termsOfferedFld)
                            .addComponent(_prereqsFld)
                            .addGroup(_mainPanelLayout.createSequentialGroup()
                                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(_mainPanelLayout.createSequentialGroup()
                                        .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(_diffLvlFld, javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(_creditsFld, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(_showAllPrereqsBtn))
                                    .addComponent(_idLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(_mainPanelLayout.createSequentialGroup()
                                        .addComponent(_codeFld, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(_searchBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel11)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(_findAllDescendantsBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(_mainPanelLayout.createSequentialGroup()
                        .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(_previousCrsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(_deleteCrsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(_saveCrsBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(_saveAllCrssBtn)
                        .addGap(18, 18, 18)
                        .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(_addNewCrsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(_nextCrsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        _mainPanelLayout.setVerticalGroup(
            _mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(_mainPanelLayout.createSequentialGroup()
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(_idLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(_codeFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(_searchBtn)
                    .addComponent(jLabel11)
                    .addComponent(_findAllDescendantsBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(_synonymsFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(_titleFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(_creditsFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(_diffLvlFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(_showAllPrereqsBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(_prereqsFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(_coreqsFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(_displayNameFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(_termsOfferedFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(_previousCrsBtn)
                    .addComponent(_nextCrsBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(_mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(_deleteCrsBtn)
                    .addComponent(_addNewCrsBtn)
                    .addComponent(_saveAllCrssBtn)
                    .addComponent(_saveCrsBtn))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(_mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(_mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _diffLvlFldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__diffLvlFldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event__diffLvlFldActionPerformed

    private void _searchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__searchBtnActionPerformed
        String scode = this._codeFld.getText().trim();
        Course c = Course.getCourseByCode(scode);
        if (c!=null) {
            populateForm(c);
        }
        else {
            JOptionPane.showConfirmDialog(null, "Course NOT found");
        }
    }//GEN-LAST:event__searchBtnActionPerformed

    private void _previousCrsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__previousCrsBtnActionPerformed
        int cId = Integer.parseInt(this._idLbl.getText());
        while (cId>0) {
            --cId;
            Course c = Course.getCourseById(cId);
            if (c!=null) {
                populateForm(c);
                break;
            }
        }
    }//GEN-LAST:event__previousCrsBtnActionPerformed

    
    private void _nextCrsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__nextCrsBtnActionPerformed
        int cId = Integer.parseInt(this._idLbl.getText());
        while (cId<Course.getNumCourses()-1) {
            ++cId;
            Course c = Course.getCourseById(cId);
            if (c!=null) {
                populateForm(c);
                break;
            }
        }
    }//GEN-LAST:event__nextCrsBtnActionPerformed

    
    private void _saveCrsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__saveCrsBtnActionPerformed
        // save form data to Course object
        int Smax = _params.getSmax();
        Course.modifyCourse(this._idLbl.getText(), 
                            this._codeFld.getText(),
                            this._titleFld.getText(), 
                            this._synonymsFld.getText(), 
                            this._creditsFld.getText(),
                            this._prereqsFld.getText(), 
                            this._coreqsFld.getText(), 
                            this._termsOfferedFld.getText(), 
                            this._displayNameFld.getText(), 
                            this._diffLvlFld.getText(), 
                            Smax);
    }//GEN-LAST:event__saveCrsBtnActionPerformed

    
    private void _addNewCrsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__addNewCrsBtnActionPerformed
        int ncid = Course.getLastId();
        this._idLbl.setText(Integer.toString(ncid+1));
        this._codeFld.setText("");
        this._synonymsFld.setText("");
        this._creditsFld.setText("");
        this._prereqsFld.setText("");
        this._coreqsFld.setText("");
        this._termsOfferedFld.setText("-");
        this._displayNameFld.setText("");
        this._diffLvlFld.setText("");
        this._nextCrsBtn.setEnabled(false);
        this._previousCrsBtn.setEnabled(true);
    }//GEN-LAST:event__addNewCrsBtnActionPerformed

    
    private void _saveAllCrssBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__saveAllCrssBtnActionPerformed
        // rename the "cls.csv" file to "cls.csv.bak" and overwrite "cls.csv"
        // with the new data
        try {
            // make back-up file
            File src = new File(_dir2Files+"/cls.csv");
            File dest = new File(_dir2Files+"/cls.csv.bak");
            if (dest.exists()) dest.delete();
            Files.copy(src.toPath(), dest.toPath());
            // now overwrite main file
            final int Smax = _params.getSmax();
            PrintWriter pw = 
                    new PrintWriter(new FileWriter(_dir2Files+"/cls.csv"));
            // first print the header line so we know what each column is about
            pw.println("#"+_params.getCourseCSVFileHeader());
            final int last_id = Course.getLastId();
            for (int i=0; i<=last_id; i++) {
                Course ci = Course.getCourseById(i);
                if (ci==null) continue;  // course was deleted
                String crs_details = ci.getFullDetailsString(Smax);
                pw.println(crs_details);
            }
            pw.flush();
            pw.close();
            JOptionPane.showConfirmDialog(null, "File cls.csv updated");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event__saveAllCrssBtnActionPerformed

    
    private void _deleteCrsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__deleteCrsBtnActionPerformed
        Course.deleteCourse(this._idLbl.getText());
        // go to the previous course
        final int tid = Integer.parseInt(this._idLbl.getText());
        int id = tid;
        while (--id>=0) {
            Course c = Course.getCourseById(id);
            if (c!=null) {
               populateForm(c);
               if (Course.isLast(id)) {
                   this._nextCrsBtn.setEnabled(false);
               }
               if (Course.isFirst(id)) {
                   this._previousCrsBtn.setEnabled(false);
               }
               return;
            }
        }
        if (id<0) {
            id = tid;
            final int lastid = Course.getLastId();
            while (++id<=lastid) {
                Course c = Course.getCourseById(id);
                if (c!=null) {
                    populateForm(c);
                   if (Course.isLast(id)) {
                       this._nextCrsBtn.setEnabled(false);
                   }
                   if (Course.isFirst(id)) {
                       this._previousCrsBtn.setEnabled(false);
                    }
                    return;
                }
            }
        }
        // no courses left
        this._idLbl.setText("0");
        this._codeFld.setText("");
        this._titleFld.setText("");
        this._synonymsFld.setText("");
        this._creditsFld.setText("");
        this._prereqsFld.setText("");
        this._coreqsFld.setText("");
        this._termsOfferedFld.setText("-");
        this._displayNameFld.setText("");
        this._diffLvlFld.setText("");
        this._previousCrsBtn.setEnabled(false);
        this._nextCrsBtn.setEnabled(false);
    }//GEN-LAST:event__deleteCrsBtnActionPerformed

    private void _findAllDescendantsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__findAllDescendantsBtnActionPerformed
        // find all courses listing the course in the search box as ancestor.
        String ccode = this._codeFld.getText();
        Iterator<String> ccodes = Course.getAllCodesIterator();
        Set<Course> reqs = new TreeSet<>();
        while (ccodes.hasNext()) {
            String cc_str = ccodes.next();
            if (cc_str.equals(ccode.trim())) continue;
            Course c = Course.getCourseByCode(cc_str);
            if (c.requiresCourse(ccode)) reqs.add(c);
        }
        String result = "Courses Requiring "+ccode+":\n";
        int i=0;
        for (Course c : reqs) {
            result += c.getCode()+" "+c.getName();
            if (++i % 2 == 0) result += "\n";
            else result += " , ";
        }
        JOptionPane.showConfirmDialog(null, result);
    }//GEN-LAST:event__findAllDescendantsBtnActionPerformed

    private void _showAllPrereqsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__showAllPrereqsBtnActionPerformed
        // find all courses that are prereqs for this course (prereqs plus the
        // prereqs' prereqs)
        String ccode = this._codeFld.getText();
        Set<String> all_prereqs = new TreeSet<>();
        Set<String> stack = new HashSet<>();
        stack.add(ccode);
        while(stack.size()>0) {
            Iterator<String> sit = stack.iterator();
            ccode = sit.next();
            sit.remove();
            Course cc = Course.getCourseByCode(ccode);
            Set<Set<String>> ccodes = cc.getPrereqs();
            for (Set<String> cs : ccodes) {
                all_prereqs.addAll(cs);
                stack.addAll(cs);
            }
        }
        String result = "All Courses Required for "+_codeFld.getText()+":\n";
        result = all_prereqs.stream().
                        map(c -> Course.getCourseByCode(c).toString()+"\n").
                          reduce(result, String::concat);
        JOptionPane.showConfirmDialog(null, result);        
    }//GEN-LAST:event__showAllPrereqsBtnActionPerformed

    
    /**
     * invoke without any command-line arguments.
     * @param args the command line arguments the first argument must be the 
     * name of the directory relative to the root of the app where the files 
     * to check ("cls.csv", "params.props") are located.
     */
    public static void main(String args[]) {
        
        _dir2Files = args[0];
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CourseEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CourseEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CourseEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CourseEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CourseEditor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton _addNewCrsBtn;
    private javax.swing.JTextField _codeFld;
    private javax.swing.JTextField _coreqsFld;
    private javax.swing.JTextField _creditsFld;
    private javax.swing.JButton _deleteCrsBtn;
    private javax.swing.JTextField _diffLvlFld;
    private javax.swing.JTextField _displayNameFld;
    private javax.swing.JButton _findAllDescendantsBtn;
    private javax.swing.JLabel _idLbl;
    private javax.swing.JPanel _mainPanel;
    private javax.swing.JButton _nextCrsBtn;
    private javax.swing.JTextField _prereqsFld;
    private javax.swing.JButton _previousCrsBtn;
    private javax.swing.JButton _saveAllCrssBtn;
    private javax.swing.JButton _saveCrsBtn;
    private javax.swing.JButton _searchBtn;
    private javax.swing.JButton _showAllPrereqsBtn;
    private javax.swing.JTextField _synonymsFld;
    private javax.swing.JTextField _termsOfferedFld;
    private javax.swing.JTextField _titleFld;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    // End of variables declaration//GEN-END:variables
}

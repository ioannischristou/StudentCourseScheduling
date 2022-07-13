package edu.acg.itss;

import gurobi.*;
import java.io.*;
import java.util.*;


/**
 * class creates and handles the student course scheduling Mixed Integer 
 * Programming problem, passing it to a MIP solver (SCIP or GUROBI) to solve. It 
 * creates the problem in a .lp formatted file, and as such, is the most complex
 * class of the entire application, maintaining the logic for creating all 
 * constraints and objectives.
 * Notice that this class is only used by the <CODE>MainGUI</CODE> class of this
 * package, which in turn during start-up knows which directory contains the 
 * files to read from.
 * @author itc
 */
public class MIPHandler {
    private ScheduleParams _params;
    private PassedCourses _passed;
    private DesiredCourses _desired;  // PassedCourses & DesiredCourses classes
                                      // have exactly the same structure, and it
                                      // would be better to be represented by a
                                      // single class, say SpecialCourses or
                                      // smth like that.
    
    
    /**
     * value needed for ITC program to ensure that the "Concentration Electives"
     * 2nd constraint is also upheld, since the inclusion of MGxxxx and MUyyyy
     * course codes in the focus areas makes it impossible to uphold with 
     * constraints only. For other programs, likely it won't be needed.
     */
    private static final double _DOMAIN_COEFF_INCR = -0.001;
    
    
    /**
     * the solution is represented as a map of course-ids to term-no when the
     * course must be taken. If a course-id key is not in the map, the course
     * is not in the optimal schedule.
     */
    private HashMap<Integer, Integer> _cid2tnoMap = new HashMap<>();
    
    
    /**
     * single constructor is a no-op.
     */
    public MIPHandler() {
        // no-op
    }
    
    
    /**
     * reads all data from files in the specified directory in the class
     * <CODE>MainGUI</CODE> as well as some optional files in the current 
     * directory. This includes every group-data file, as well as the schedule 
     * params file. Group data files end with extension ".grp" (details of their 
     * format are found in the docs for class <CODE>CourseGroup</CODE>), and the 
     * schedule param file is "params.props".
     * The course data are read from file "cls.csv" (in the specific program 
     * directory currently in use). Details of this file's format are in the 
     * javadocs for class <CODE>Course</CODE>.
     * If the file "passedcourses.txt" exists in the current directory, it reads 
     * all course numbers the student has already passed: the file consists of 
     * course codes separated by semi-column. Desired courses must be present in 
     * the file "desiredcourses.txt" to be read from the current directory too.
     * Finally, if the file "estimated_grades.txt" exists in the current dir, it
     * reads all course numbers for which there exists an estimate (from QARMA)
     * of the grade the student is going to get, and sets the appropriate field
     * in the right <CODE>Course</CODE> objects (default is zero which does not
     * modify the problem at all), assuming the estimated grade is above the 
     * minimum threshold set in property "MinGradeThres".
     */
    public void readProblemData() {
        final String dir2Files = MainGUI.getDir2Files();
        _params = new ScheduleParams(dir2Files+"/params.props");
        Course.readAllCoursesFromFile(dir2Files+"/cls.csv", _params.getSmax());
        _passed = new PassedCourses();
        File psd = new File("passedcourses.txt");
        if (psd.exists()) {
            _passed.readPassedCoursesFromFile("passedcourses.txt");
        }
        _desired = new DesiredCourses();
        File dsd = new File("desiredcourses.txt");
        if (dsd.exists()) {
            _desired.readDesiredCoursesFromFile("desiredcourses.txt");
        }        
        File est = new File("estimated_grades.txt");
        if (est.exists()) {
            final float thres = _params.getMinGradeThres();
            try (BufferedReader br = new BufferedReader(new FileReader(est))) {
                while(true) {
                    String line = br.readLine();
                    if (line==null) break;  // EOF
                    String[] vals = line.split(",");
                    Course c = Course.getCourseByCode(vals[0]);
                    float val = Float.parseFloat(vals[1].trim());
                    if (val >= thres) {
                        c.setEstimatedGrade(val);
                    }
                }
            }
            catch (Exception e) {  // cannot get here
                e.printStackTrace();
                System.exit(-1);
            }
        }
        File cur_dir = new File(dir2Files);
        File[] cur_files = cur_dir.listFiles();
        for (File f : cur_files) {
            if (f.isFile() && f.getName().endsWith("grp")) {
                CourseGroup.readCourseGroup(f.getAbsolutePath());
            }
        }
        // in case of data entry errors: remove any passed courses from the 
        // desired courses
        Iterator<String> desired_it = _desired.getDesiredCourseCodesIterator();
        while (desired_it.hasNext()) {
            String dc = desired_it.next();
            if (_passed.contains(dc)) desired_it.remove();
        }
    }
    
    
    /**
     * get the schedule parameters object. Must have called 
     * <CODE>readProblemData()</CODE> first.
     * @return ScheduleParams
     */
    public ScheduleParams getScheduleParams() {
        return _params;
    }
    
    
    /**
     * get the reference to the <CODE>PassedCourses</CODE> object. Method should
     * only be called after <CODE>readProblemData()</CODE> has been invoked.
     * @return PassedCourses may be empty (but not null)
     */
    public PassedCourses getPassedCourses() {
        return _passed;
    }
    

    /**
     * get the reference to the <CODE>DesiredCourses</CODE> object. Method
     * should only be called after <CODE>readProblemData()</CODE> has been 
     * invoked.
     * @return DesiredCourses may be empty (but not null)
     */
    public DesiredCourses getDesiredCourses() {
        return _desired;
    }

    
    /**
     * creates the file "schedule.lp" that describes the MIP Programming 
     * problem of the student course scheduling problem, with objective being a 
     * weighted combination of time-to-completion, total-number-of-credits,
     * maximum-sum-of-course-difficulty-levels-per-semester and sum-of-grades. 
     * Always last, with coefficient -0.001, is the objective to maximize 
     * courses from designated program codes in the schedule params. The 
     * objective to maximize individual student GPA is also present and is 
     * implemented by maximizing the weighted sum of courses taken with weights 
     * equal to the estimated grade of the course (assuming such estimates are 
     * known, and only for courses for which the estimate is above 3.0/4.0). 
     * Notice that these estimates, if they exist, are in the file 
     * "estimated_grades.txt".
     * Notice that despite the fact that this class holds references to the 
     * <CODE>[Passed | Desired]Courses</CODE> objects, the final say is whatever 
     * is selected in the GUI, and this is why these two sets are passed in as 
     * arguments to this method.
     * @param isHonorStudent boolean whether the student is an honors student
     * @param maxNumCrsPerSem int student-imposed max num courses per semester
     * @param maxNumCrsDurThesis int student-imposed max num courses during the
     * semester when thesis is carried out (must be &ge; 1)
     * @param s1off boolean true iff the student wishes not to register
     * for any course during the Summer1 session
     * @param s2off boolean true iff the student wishes not to register
     * for any course during the Summer2 session
     * @param stoff boolean true iff the student wishes not to register
     * for any course during the Summer-Term
     * @param numCoursesPerTrm2StrMap Map&lt;Integer tno, String constr&gt; may
     * have constraints on #courses to take on specific terms
     * @param passed Set&lt;String&gt; the set of courses the student has passed
     * @param num_OU_cur_academic_year int the number of OU courses already 
     * passed during the current academic year (0 if the next term is FALL)
     * @param desired Set&lt;String&gt; the set of courses the student wishes
     * to take; each string in the set may be either "ITC3160", or "ITC3160;"
     * which indicates student doesn't want to take the course, or it may be 
     * smth like "ITC3160;FA2022 SP2023" which indicates when student wants to
     * take the course
     * @param concentration String the chosen concentration area
     * @param DNcoeff int the coefficient for the shortest-time-to-completion
     * @param DLcoeff int the coefficient for the max-sum-of-difficulty-levels
     * (per semester)
     * @param Crcoeff int the coefficient for the total-sum-of-credits objective
     * @param Grcoeff int the coefficient for the expected-GPA objective
     */
    public void createMIPFile(boolean isHonorStudent,
                              int maxNumCrsPerSem, int maxNumCrsDurThesis,
                              boolean s1off, boolean s2off, boolean stoff,
                              Map<Integer, String> numCoursesPerTrm2StrMap,
                              Set<String> passed, int num_OU_cur_academic_year, 
                              Set<String> desired,
                              String concentration,
                              int DNcoeff, int DLcoeff, int Crcoeff, 
                              int Grcoeff) {
        if (concentration==null || concentration.length()==0)
            throw new IllegalArgumentException("concentration area name "+
                                               "cannot be null or empty");
        // 0. update data structures: the passed and desired arguments are the 
        //    final word in this matter
        _passed.clear();
        _passed.addAll(passed);
        _desired.clear();
        _desired.addAll(desired);
        // 1. set the objective
        StringBuffer prob = 
                new StringBuffer("Minimize\nobj: "+DNcoeff+" D + "+
                                 DLcoeff+" DL + ");
        final int N = Course.getNumCourses();
        for (int i=0; i<N; i++) {
            Course ci = Course.getCourseById(i);
            double ival = ci.getCredits()*Crcoeff;
            // designated program codes to maximize as last resort
            Set<ProgramCodeStruct> designated_program_codes = 
                    _params.getPrograms2Maximize();
            for (ProgramCodeStruct pcs : designated_program_codes) {
                /*
                if (ci.getCode().startsWith("ITC") || 
                    (ci.getCode().startsWith("MA") && 
                     CourseGroup.getCourseGroupByName("LE-core-stat").
                                   getGroupCodes().
                                     contains(ci.getCode())==false))
                    ival += _DOMAIN_COEFF_INCR;
                */
                if (ci.getCode().startsWith(pcs.getProgramCode())) {
                    if (pcs.getException()!=null && 
                        pcs.getException().length()>0) {
                        CourseGroup cg = 
                          CourseGroup.getCourseGroupByName(pcs.getException());
                        if (cg.getGroupCodes().contains(ci.getCode())==false) {
                            ival += _DOMAIN_COEFF_INCR;
                            break;  // the increment applies only once
                        }
                    }  // if there is exception group, ensure it's not in there
                    else {
                        ival += _DOMAIN_COEFF_INCR;  // if no exception, ensure
                                                     // ci's obj is incremented
                        break;  // the increment applies only once
                    }
                }
            }
            // add to the ival the value of the estimated grade multiplied by
            // the Grcoeff for the expected-GPA-max objective
            if (ci.getEstimatedGrade()>=_params.getMinGradeThres()) {
                ival += Grcoeff * ci.getEstimatedGrade();
            }
            prob.append(ival).append(" x_").append(i);
            if (i<N-1) prob.append(" + ");
            else prob.append("\n");
        }
        prob.append("\nSubject To\n");
        // 2. now set the constraints
        // 2.1 first, the D constraints
        prob.append("\\ 1. D constraints\n");
        int ccount = 1;  // constraint counter
        final int Smax = _params.getSmax();
        for (int i=0; i<N; i++) {
            for (int s=1; s<=Smax; s++) {
                prob.append("c").append(ccount).append(": ").
                    append(s).
                        append(" x_").append(i).append("_").append(s).
                            append(" - D <= 0\n");
                ++ccount;
            }
        }
        // 2.1 continued, the DL constraints
        prob.append("\\ 1. DL constraints\n");
        for (int s=1; s<=Smax; s++) {
            prob.append("c").append(ccount).append(": ");
            for (int i=0; i<N; i++) {
                Course ci = Course.getCourseById(i);
                final int dli = ci.getDifficultyLevel();
                prob.append(dli).append(" x_").append(i).append("_").append(s);
                if (i<N-1) prob.append(" + ");
                else prob.append(" - DL <= 0\n");
            }
        }
        // 2.2 second, the class availability constraints
        prob.append("\\ 2. class availability constraints\n");
        for (int i=0; i<N; i++) {
            List<Integer> terms_offered = 
                    Course.getCourseById(i).getTermsOffered(Smax);
            // itc: HERE debug
            prob.append("\\ course-"+i+" terms: ");
            for (Integer t : terms_offered) prob.append(t+" ");
            prob.append("\n");
            // itc: HERE debug up to here
            for (int s=1; s<=Smax; s++) {
                final int ois = terms_offered.contains(s) ? 1 : 0;
                prob.append("c").append(ccount).
                    append(": x_").append(i).append("_").append(s).
                        append(" <= ").
                            append(ois).
                                append("\n");
                ++ccount;
            }
        }
        // 2.3 third, prerequisite constraints
        prob.append("\\ 3a. PREREQ constraints\n");
        for (int i=0; i<N; i++) {
            Course ci = Course.getCourseById(i);
            Set<Set<String>> prereq_codes = ci.getPrereqs();
            if (prereq_codes.size()>=1) {
                for (int s=1;s<=Smax; s++) {
                    int ks = Course.isSummerTerm(s) ? 3 : 1;
                    if (s-ks<0 || prereq_codes.isEmpty()) continue;
                    for (Set<String> ps : prereq_codes) {
                        if (ps.isEmpty()) continue;
                        prob.append("c"+ccount+": x_"+i+"_"+s); ++ccount;
                        prob.append(" - ");
                        Iterator<String> ps_it = ps.iterator();
                        while (ps_it.hasNext()) {
                            String crsi = ps_it.next();
                            Course cj = Course.getCourseByCode(crsi);
                            int j = 0;
                            try {
                                j = cj.getId();
                            }
                            catch (Exception e) {
                                System.err.println("course w/ code "+crsi+
                                                   " doesn't exist "+
                                                   "(prereqs for "+ci+")");
                                throw e;
                            }
                            for (int t=0; t<=s-ks; t++) {
                                prob.append("x_"+j+"_"+t);
                                if (t<s-ks) prob.append(" - ");
                            }
                            if (ps_it.hasNext()) prob.append(" - ");
                            else prob.append(" <= 0\n");
                        }
                    }
                }
            }
        }
        // 2.3 third continued, co-requisite constraints
        prob.append("\\ 3b. COREQ constraints\n");
        for (int i=0; i<N; i++) {
            Course ci = Course.getCourseById(i);
            Set<String> coreq_codes = ci.getCoreqs();
            if (coreq_codes.size()>=1) {
                for (int s=1; s<=Smax; s++) {
                    final int ks = Course.isSummerTerm(s) ? 3 : 1;
                    if (coreq_codes.isEmpty()) continue;
                    prob.append("c"+ccount+": x_"+i+"_"+s+" - "); ++ccount;
                    Iterator<String> coreq_it = coreq_codes.iterator();
                    while (coreq_it.hasNext()) {
                        String codej = coreq_it.next();
                        Course cj = Course.getCourseByCode(codej);
                        int j = cj.getId();
                        prob.append("x_"+j+"_"+s);
                        if (s-ks>=0) prob.append(" - ");
                        for (int t=0; t<=s-ks; t++) {
                            prob.append("x_"+j+"_"+t);
                            if (t<s-ks) prob.append(" - ");
                        }
                        if (coreq_it.hasNext()) prob.append(" - ");
                        else prob.append(" <= 0\n");
                    }
                }
            }
        }
        // 2.4 fourth, LEVEL constraints
        CourseGroup level4 = CourseGroup.getCourseGroupByName("L4");
        CourseGroup level5 = CourseGroup.getCourseGroupByName("L5");
        CourseGroup level6 = CourseGroup.getCourseGroupByName("L6");
        List<String> l4codes = level4.getGroupCodes();
        List<String> l5codes = level5.getGroupCodes();
        List<String> l6codes = level6.getGroupCodes();
        // L-5 constraints: at least 4 level-4 courses must be passed
        // before taking a level-5 course
        prob.append("\\ 4a. L-5 constraints\n");
        for (String l5cc : l5codes) {
            Course l5crs = Course.getCourseByCode(l5cc);
            int i = l5crs.getId();
            for (int s=1; s<=Smax; s++) {
                int ks = Course.isSummerTerm(s) ? 3 : 1;
                prob.append("c"+ccount+": ");
                ++ccount;
                prob.append("4 x_"+i+"_"+s+" - ");
                Iterator<String> l4codesit = l4codes.iterator();
                while (l4codesit.hasNext()) {
                    String js = l4codesit.next();
                    Course l4crs = Course.getCourseByCode(js);
                    int j = l4crs.getId();
                    for (int t=0; t<=s-ks; t++) {
                        prob.append("x_"+j+"_"+t);
                        if (t<s-ks) prob.append(" - ");
                        else {
                            if (l4codesit.hasNext()) prob.append(" - ");
                            else prob.append(" <= 0\n");
                        }
                    }
                }
            }
        }
        // OTHER L-5 constraints: level-5 constraints for non-ITC level-5 
        // classes
        prob.append("\\ 4b. OTHER L-5 constraints\n");
        Iterator<String> cgs_it = CourseGroup.getCourseGroupNameIterator();
        Set<String> other_l5_cgs = new HashSet<>();
        while (cgs_it.hasNext()) {
            String cgs = cgs_it.next();
            if (cgs.startsWith("L5-")) other_l5_cgs.add(cgs);
        }
        for (String ocgl5 : other_l5_cgs) {
            final List<String> ol5codes = 
                    CourseGroup.getCourseGroupByName(ocgl5).getGroupCodes();
            for (String l5cc : ol5codes) {
                Course l5crs = Course.getCourseByCode(l5cc);
                int i = l5crs.getId();
                for (int s=1; s<=Smax; s++) {
                    int ks = Course.isSummerTerm(s) ? 3 : 1;
                    prob.append("c"+ccount+": ");
                    ++ccount;
                    prob.append("4 x_"+i+"_"+s+" - ");
                    Iterator<String> l4codesit = l4codes.iterator();
                    while (l4codesit.hasNext()) {
                        String js = l4codesit.next();
                        Course l4crs = Course.getCourseByCode(js);
                        int j = l4crs.getId();
                        for (int t=0; t<=s-ks; t++) {
                            prob.append("x_"+j+"_"+t);
                            if (t<s-ks) prob.append(" - ");
                            else {
                                if (l4codesit.hasNext()) prob.append(" - ");
                                else prob.append(" <= 0\n");
                            }
                        }
                    }
                }
            }            
        }
        // L-6 constraints
        // first, ALL level-4 courses must be passed before taking a level-6
        // course
        prob.append("\\ 5a. L-6 constraints about L-4\n");
        final int l4_num = l4codes.size();
        for (String l6cc : l6codes) {
            Course l6crs = Course.getCourseByCode(l6cc);
            if (l6crs==null) {
                System.err.println("L-6 course w/ code "+l6cc+" doesn't exist");
                throw new NullPointerException();
            }
            int i = l6crs.getId();
            for (int s=1; s<=Smax; s++) {
                int ks = Course.isSummerTerm(s) ? 3 : 1;
                prob.append("c"+ccount+": ");
                ++ccount;
                prob.append(l4_num+" x_"+i+"_"+s+" - ");
                Iterator<String> l4codesit = l4codes.iterator();
                while (l4codesit.hasNext()) {
                    String js = l4codesit.next();
                    Course l4crs = Course.getCourseByCode(js);
                    int j = l4crs.getId();
                    for (int t=0; t<=s-ks; t++) {
                        prob.append("x_"+j+"_"+t);
                        if (t<s-ks) prob.append(" - ");
                        else {
                            if (l4codesit.hasNext()) prob.append(" - ");
                            else prob.append(" <= 0\n");
                        }
                    }
                }
            }
        }
        // second, at least 4 level-5 courses must be passed before taking
        // a level-6 course
        prob.append("\\ 5b. L-6 constraints about L-5\n");
        for (String l6cc : l6codes) {
            Course l6crs = Course.getCourseByCode(l6cc);
            int i = l6crs.getId();
            for (int s=1; s<=Smax; s++) {
                int ks = Course.isSummerTerm(s) ? 3 : 1;
                prob.append("c"+ccount+": ");
                ++ccount;
                prob.append("4 x_"+i+"_"+s+" - ");
                Iterator<String> l5codesit = l5codes.iterator();
                while (l5codesit.hasNext()) {
                    String js = l5codesit.next();
                    Course l5crs = Course.getCourseByCode(js);
                    int j = l5crs.getId();
                    for (int t=0; t<=s-ks; t++) {
                        prob.append("x_"+j+"_"+t);
                        if (t<s-ks) prob.append(" - ");
                        else {
                            if (l5codesit.hasNext()) prob.append(" - ");
                            else prob.append(" <= 0\n");
                        }
                    }
                }
            }
        }
        // done with LEVEL constraints
        // 2.5 fifth, credit constraint
        prob.append("\\ 6. total credit constraints\n");
        final int Tc = _params.getMinReqdTotalCredits();
        prob.append("c"+ccount+": ");
        ++ccount;
        for (int i=0; i<N; i++) {
            final Course ci = Course.getCourseById(i);
            final int credit_i = ci.getCredits();
            prob.append(credit_i+" x_"+i);
            if (i<N-1) prob.append(" + ");
            else prob.append(" >= "+Tc+"\n");
        }
        // 2.6 sixth, LE constraint specifies the latest term number by which 
        //     all LE course requirements must be met.
        prob.append("\\ 6.5 LE upper term limit constraints\n");
        CourseGroup legroup = CourseGroup.getCourseGroupByName("LE");
        List<String> lecodes = legroup.getGroupCodes();
        int maxleterm = _params.getMaxLETerm();
        for (int s=maxleterm+1; s<=Smax; s++) {
            for (String lecode : lecodes) {
                Course lec = Course.getCourseByCode(lecode);
                prob.append("c"+ccount+": "); ++ccount;
                prob.append(" x_"+lec.getId()+"_"+s+" = 0\n");
            }
        }
        // 2.7 seventh, semester credits constraint
        prob.append("\\ 7. term credit constraints\n");
        final int max_sem_cr = _params.getCmax(isHonorStudent);
        final int max_summer_cr = _params.getSummerCmax(isHonorStudent);
        for (int s=1; s<=Smax; s++) {
            if (Course.happensDuringSummer(s) && max_summer_cr>0) {
                // create constraint for all courses during summer months
                // and skip the "normal" term credit constraint
                prob.append("c"+ccount+": "); ++ccount;
                int s2max = Math.min(Smax, s+2);
                for (int s2=s; s2<=s2max; s2++) {
                    for (int i=0; i<N; i++) {
                        int cicr = Course.getCourseById(i).getCredits();
                        prob.append(cicr+" x_"+i+"_"+s2);
                        if (i<N-1 || (i==N-1 && s2<s2max)) prob.append(" + ");
                        else prob.append(" <= "+max_summer_cr+"\n");
                    }
                }
                s = s2max;
            }
            else if (!Course.happensDuringSummer(s)) {
                // the "normal" term credit constraint
                prob.append("c"+ccount+": "); ++ccount;
                for (int i=0; i<N; i++) {
                    int cicr = Course.getCourseById(i).getCredits();
                    prob.append(cicr+" x_"+i+"_"+s);
                    if (i<N-1) prob.append(" + ");
                    else prob.append(" <= "+max_sem_cr+"\n");
                }
            }
        }
        // semester max #courses constraint (freshman only)
        if (_passed.size()<_params.getMinNumCourses4Sophomore()) {
            // ignore this value if there is a specific value in the 
            // outputs pane
            String cons = numCoursesPerTrm2StrMap.get(1);
            if (cons==null || cons.length()==0) { 
                final int maxnumcoursesperterm = 
                    _params.getMaxNumCoursesPerTerm4Freshmen();
                final int maxterm = 1;  // constraint only applies to the 1st 
                                        // upcoming term
                prob.append("\\ 7.0 term #courses freshman constraints\n");
                for (int s=1; s<=maxterm; s++) {
                    prob.append("c"+ccount+": "); ++ccount;
                    for (int i=0; i<N; i++) {
                        prob.append(" x_"+i+"_"+s);
                        if (i<N-1) prob.append(" + ");
                        else prob.append(" <= "+maxnumcoursesperterm+"\n");
                    }
                }            
            }
        }
        // semester max #courses constraint (student imposed)
        prob.append("\\ 7.1 term #courses student desire constraints\n");
        for (int s=1; s<=Smax; s++) {
            // ignore this value if there is a specific value in the outputs 
            // pane
            String cons = numCoursesPerTrm2StrMap.get(1);
            if (cons!=null && cons.length()>0) continue;             
            prob.append("c"+ccount+": "); ++ccount;
            for (int i=0; i<N; i++) {
                prob.append(" x_"+i+"_"+s);
                if (i<N-1) prob.append(" + ");
                else prob.append(" <= "+maxNumCrsPerSem+"\n");
            }
        }
        // add constraints for #courses on terms the student specified
        Iterator<Integer> tit = numCoursesPerTrm2StrMap.keySet().iterator();
        while (tit.hasNext()) {
            int tno = tit.next();
            String cons = numCoursesPerTrm2StrMap.get(tno);
            if (cons==null || cons.length()==0) continue;
            cons = cons.trim();
            int numc = Integer.MIN_VALUE;
            try {
                numc = Integer.parseInt(cons);
                cons = " = "+numc;
            }
            catch (NumberFormatException e) {
                // no-op
            }
            // cannot send string as is to the constraint as strict inequalities
            // "<" or ">" are not supported.
            if (cons.startsWith("<") && !cons.startsWith("<=")) {
                numc = Integer.parseInt(cons.substring(1));
                cons = "<= " + (numc-1);
            }
            else if (cons.startsWith(">") && !cons.startsWith(">=")) {
                numc = Integer.parseInt(cons.substring(1));
                cons = ">= " + (numc+1);
            }
            prob.append("c"+ccount+": "); ++ccount;
            for (int i=0; i<N; i++) {
                prob.append(" x_"+i+"_"+tno);
                if (i<N-1) prob.append(" + ");
                else prob.append(" " + cons + "\n");
            }
        }
        // thesis semester max #courses constraint (student imposed):
        // Σ_{i!=θ} x_{i,s} <= σ x_{θ,s} + M(1-x_{θ,s})  forall s=1...Smax
        --maxNumCrsDurThesis;
        final int Mms = _params.getCmax(isHonorStudent) - maxNumCrsDurThesis;
        Course thesis = Course.getCourseByCode(_params.getThesisCode());
        int thesis_id = thesis.getId();
        prob.append("\\ 7.2 THESIS term #courses student desire constraints\n");
        for (int s=1; s<=Smax; s++) {
            prob.append("c"+ccount+": "); ++ccount;
            for (int i=0; i<N; i++) {
                if (i!=thesis_id) prob.append(" x_"+i+"_"+s);
                else prob.append(Mms + " x_"+i+"_"+s);
                if (i<N-1) prob.append(" + ");
                else prob.append(" <= "+_params.getCmax(isHonorStudent)+"\n");
            }
        }
        prob.append("\\ 7.5 summer max #concurrent-courses constraint\n");
        final int nmax = _params.getSummerConcNMax();
        for (int s=1; s<=Smax; s++) {
            if (Course.happensDuringSummer(s) && nmax>=0 && s+2<=Smax) {
                prob.append("c"+ccount+": "); ++ccount;
                // create constraint for all courses during S1+ST
                for (int i=0; i<N; i++) {
                    prob.append("x_"+i+"_"+s+" + ");
                }
                int st = s+2;
                for (int i=0; i<N; i++) {
                    prob.append("x_"+i+"_"+st);
                    if (i<N-1) prob.append(" + ");
                    else prob.append(" <= "+nmax+"\n");
                }
                // create constraint for all courses during S2+ST
                int s2 = s+1;
                prob.append("c"+ccount+": "); ++ccount;
                for (int i=0; i<N; i++) {
                    prob.append("x_"+i+"_"+s2+" + ");
                }
                for (int i=0; i<N; i++) {
                    prob.append("x_"+i+"_"+st);
                    if (i<N-1) prob.append(" + ");
                    else prob.append(" <= "+nmax+"\n");
                }
                s += 2;
            }
        }
        // 2.8 eightth, x_i definition
        prob.append("\\ 8. x_i variable constraints\n");
        for (int i=0; i<N; i++) {
            prob.append("c"+ccount+": ");
            ++ccount;
            for (int s=0; s<=Smax; s++) {
                prob.append("x_"+i+"_"+s);
                if (s<Smax) prob.append(" + ");
                else prob.append(" - x_"+i+" = 0\n");
            }
        }
        // 2.9 ninth, group credits and min num course definitions
        //     Notice that groups representing concentration areas are treated
        //     differently.
        Iterator<String> gnamesit = CourseGroup.getCourseGroupNameIterator();
        while (gnamesit.hasNext()) {
            final String groupname = gnamesit.next();
            final CourseGroup cg = CourseGroup.getCourseGroupByName(groupname);
            if (cg.isConcentrationArea()) continue;  // don't do anything here
            if (cg.isCapstoneProjectGroup()) continue;  // don't do anything now
            if (cg.isSoftOrderPrecedenceConstraint()) continue;  // same here
            if (cg.isOUConstraint()) continue;  // again!
            prob.append("\\ group "+groupname+" constraints\n");
            final int cgc = cg.getMinNumCreditsReqd();
            int cgn = cg.getMinNumCoursesReqd();
            if (cgn>=0) {
                if (!cg.isCoursesReqdExact() && cgn>0) {  // normal constraint
                    prob.append("c"+ccount+": ");
                    ++ccount;
                    List<String> crss = cg.getGroupCodes();
                    Iterator<String> crss_it = crss.iterator();
                    while (crss_it.hasNext()) {
                        String crscode = crss_it.next();
                        Course crs = Course.getCourseByCode(crscode);
                        int crs_id = crs.getId();
                        prob.append("x_"+crs_id);
                        if (crss_it.hasNext()) prob.append(" + ");
                        else prob.append(" >= "+cgn+"\n");
                    }
                }
                else if (cg.isCoursesReqdExact()) {  // XOR-type constraint
                    // remove from course-group every course that is already
                    // passed, and for the remaining courses, make their sum 
                    // equal to the remaining cgn_{+} number.
                    Set<String> crss = new HashSet(cg.getGroupCodes());
                    Iterator<String> crss_it = crss.iterator();
                    while (crss_it.hasNext()) {
                        String crscode = crss_it.next();
                        if (_passed.contains(crscode)) { 
                            crss_it.remove();
                            --cgn; 
                        }
                    }
                    if (cgn<0) cgn = 0;
                    if (crss.size()>0) {
                        // for the remaining courses in crss, act as original
                        prob.append("c"+ccount+": ");
                        ++ccount;
                        crss_it = crss.iterator();
                        while (crss_it.hasNext()) {
                            String crscode = crss_it.next();
                            Course crs = Course.getCourseByCode(crscode);
                            int crs_id = crs.getId();
                            prob.append("x_"+crs_id);
                            if (crss_it.hasNext()) prob.append(" + ");
                            else prob.append(" = "+cgn+"\n");
                        }                        
                    }
                }
                else if (cg.isHoldsPerSemester()) {  // MAX-type constraint
                    // constraint holds for per every semester
                    Set<String> crss = new HashSet(cg.getGroupCodes());
                    int s2max = -1;
                    for (int s=1; s<=Smax; s++) {
                        prob.append("c").append(ccount).append(": ");
                        ++ccount;
                        if (Course.happensDuringSummer(s)) {  
                            // this code assumes that s=1 is NEVER "S2" or "ST"
                            // terms.
                            s2max = Math.min(Smax, s+2);
                            for (int s2=s; s2<=s2max; s2++) {
                                Iterator<String> crss_it = crss.iterator();
                                while (crss_it.hasNext()) {
                                    String crs = crss_it.next();
                                    Course c = Course.getCourseByCode(crs);
                                    prob.append(" x_"+c.getId()+"_"+s);
                                    if (!crss_it.hasNext() && s2==s2max) 
                                        prob.append(" <= "+cgn+"\n");
                                    else prob.append(" + ");
                                }
                            }
                            s = s2max;
                            continue;
                        }
                        Iterator<String> crss_it = crss.iterator();
                        while (crss_it.hasNext()) {
                            String crs = crss_it.next();
                            Course c = Course.getCourseByCode(crs);
                            prob.append(" x_"+c.getId()+"_"+s);
                            if (crss_it.hasNext()) prob.append(" + ");
                            else prob.append(" <= "+cgn+"\n");
                        }
                    }
                }
            }
            else {  // cgn < 0 implies constraint: x_i_1 +...+ x_j_Smax <= -cgn
                cgn = -cgn;  // reverse sign
                // remove from course-group every course that is already
                // passed, and for the remaining courses, make their sum 
                // less than or equal to the remaining cgn_{+} number.
                Set<String> crss = new HashSet(cg.getGroupCodes());
                Iterator<String> crss_it = crss.iterator();
                while (crss_it.hasNext()) {
                    String crscode = crss_it.next();
                    if (_passed.contains(crscode)) { 
                        crss_it.remove();
                        --cgn; 
                    }
                }
                if (cgn<0) cgn = 0;
                if (!cg.isCoursesReqdExact() && !cg.isHoldsPerSemester() && 
                    cgn>0) {  // constraint asks for a maximum to be respected
                    prob.append("c"+ccount+": ");
                    ++ccount;
                    crss_it = crss.iterator();
                    while (crss_it.hasNext()) {
                        String crscode = crss_it.next();
                        Course crs = Course.getCourseByCode(crscode);
                        int crs_id = crs.getId();
                        prob.append("x_"+crs_id);
                        if (crss_it.hasNext()) prob.append(" + ");
                        else prob.append(" <= "+cgn+"\n");
                    }
                }
            }
            if (cgc>0) {
                prob.append("c"+ccount+": ");
                ++ccount;
                List<String> crss = cg.getGroupCodes();
                Iterator<String> crss_it = crss.iterator();
                while (crss_it.hasNext()) {
                    String crscode = crss_it.next();
                    Course crs = Course.getCourseByCode(crscode);
                    if (crs==null) {
                        throw new IllegalStateException("for group constraint "+
                                                        groupname+" course "+
                                                        crscode+" not found");
                    }
                    int crs_id = crs.getId();
                    int crs_cr = crs.getCredits();
                    prob.append(crs_cr+" x_"+crs_id);
                    if (crss_it.hasNext()) prob.append(" + ");
                    else prob.append(" >= "+cgc+"\n");
                }                
            }
        }
        // 2.10 tenth, the passed courses
        prob.append("\\ passed courses constraints\n");
        Iterator<String> passed_it = _passed.getPassedCourseCodesIterator();
        while (passed_it.hasNext()) {
            String pcode = passed_it.next();
            prob.append("c"+ccount+": ");
            ++ccount;
            Course pc = Course.getCourseByCode(pcode);
            int id = pc.getId();
            prob.append("x_"+id+"_0 = 1\n");
        }
        prob.append("\\ forbid other x_i_0 <- 1 constraints\n");
        for (int i=0; i<N; i++) {
            Course ci = Course.getCourseById(i);
            String ic = ci.getCode();
            if (!passed.contains(ic)) {
                prob.append("c"+ccount+": "); ++ccount;
                prob.append("x_"+i+"_0 = 0\n");
            }
        }
        // 2.11 eleventh, the desired courses
        prob.append("\\ desired courses constraints\n");
        Iterator<String> desired_it = _desired.getDesiredCourseCodesIterator();
        while (desired_it.hasNext()) {
            String dcode = desired_it.next();
            Course dc = Course.getCourseByCode(dcode);
            int cid = dc.getId();
            int curTrm = 0;
            if (_cid2tnoMap!=null) curTrm = _cid2tnoMap.getOrDefault(cid, 0);
            Set<Integer> allowed_terms = _desired.getAllowedTerms4Course(dcode, 
                                                                         curTrm,
                                                                         Smax);
            if (allowed_terms.size()==Smax) {  // all terms allowed
                prob.append("c"+ccount+": ");
                ++ccount;
                int id = dc.getId();
                prob.append("x_"+id+" = 1\n");
            }
            else if (allowed_terms.size()==0) {  // course is "disallowed"
                prob.append("c"+ccount+": ");
                ++ccount;
                int id = dc.getId();
                prob.append("x_"+id+" = 0\n");                
            } 
            else {  // to only allow course on the specified terms, disallow
                    // every term not specified, and request x_id = 1
                final int id = dc.getId();
                prob.append("c"+ccount+": ");
                ++ccount;
                prob.append("x_"+id+" = 1\n"); 
                // disallow not allowed semesters
                for (int i=1; i<=Smax; i++) {
                    if (!allowed_terms.contains(i)) {
                        prob.append("c"+ccount+": "); ++ccount;
                        prob.append("x_"+id+"_"+i+" = 0\n");
                    }
                }
            }
        }
        // 2.12 twelfth, summer-terms off constraints
        prob.append("\\ summer terms off constraints\n");
        if (s1off) {
            for (int s=1; s<=Smax; s++) {
                if (Course.isSummerTerm(s+2)) {
                    for (int i=0; i<N; i++) {
                        prob.append("c").append(ccount).append(": ").
                            append("x_").append(i).append("_").append(s).
                                append(" = 0\n");
                        ++ccount;
                    }
                }
            }
        }
        if (s2off) {
            for (int s=1; s<=Smax; s++) {
                if (Course.isSummerTerm(s+1)) {
                    for (int i=0; i<N; i++) {
                        prob.append("c").append(ccount).append(": ").
                            append("x_").append(i).append("_").append(s).
                                append(" = 0\n");
                        ++ccount;
                    }
                }
            }
        }
        if (stoff) {
            for (int s=1; s<=Smax; s++) {
                if (Course.isSummerTerm(s)) {
                    for (int i=0; i<N; i++) {
                        prob.append("c").append(ccount).append(": ").
                            append("x_").append(i).append("_").append(s).
                                append(" = 0\n");
                        ++ccount;
                    }
                }
            }
        }
        // 2.13 thirteenth, the concentration area constraints can be split in 
        //      more than one CourseGroup, all starting with the same name -in 
        //      particular, the name of the "concentration" argument of the 
        //      method.
        prob.append("\\ concentration "+concentration+" area constraints\n");
        Iterator<String> conc_groups = CourseGroup.getCourseGroupNameIterator();
        while (conc_groups.hasNext()) {
            String conc_name = conc_groups.next();
            if (conc_name.startsWith(concentration)) {  // enforce constraint
                CourseGroup ccg = CourseGroup.getCourseGroupByName(conc_name);
                if (!ccg.isConcentrationArea()) continue;  // bad name choice
                int cgn = ccg.getMinNumCoursesReqd();
                if (cgn>0) {
                    List<String> ccodes = ccg.getGroupCodes();
                    prob.append("c"+ccount+": "); ++ccount;
                    Iterator<String> codes_it = ccodes.iterator();
                    while (codes_it.hasNext()) {
                        String code = codes_it.next();
                        Course cc = Course.getCourseByCode(code);
                        prob.append(" x_"+cc.getId()+" ");
                        if (codes_it.hasNext()) prob.append("+ ");
                        else prob.append(">= "+cgn+"\n");
                    }
                }
                int cgc = ccg.getMinNumCreditsReqd();
                if (cgc>0) {
                    List<String> ccodes = ccg.getGroupCodes();
                    prob.append("c"+ccount+": "); ++ccount;
                    Iterator<String> codes_it = ccodes.iterator();
                    while (codes_it.hasNext()) {
                        String code = codes_it.next();
                        Course cc = Course.getCourseByCode(code);
                        prob.append(cc.getCredits()+" x_"+cc.getId()+" ");
                        if (codes_it.hasNext()) prob.append("+ ");
                        else prob.append(">= "+cgc+"\n");
                    }                    
                }
            }
        }
        // 2.14 fourteenth, the capstone project group constraints
        prob.append("\\ capstone project constraints\n");
        gnamesit = CourseGroup.getCourseGroupNameIterator();
        while (gnamesit.hasNext()) {
            String gname = gnamesit.next();
            CourseGroup cg = CourseGroup.getCourseGroupByName(gname);
            if (cg.isCapstoneProjectGroup()) {
                // first the total credits constraint for the capstone project
                final int ncredits = cg.getMinNumCreditsReqd();
                for (int s=1; s<=Smax; s++) {
                    final int ks = Course.isSummerTerm(s) ? 3 : 1;
                    if (s-ks<0) continue;
                    prob.append("c").append(ccount).append(": ").
                        append(ncredits).append(" x_");
                    ++ccount;
                    final Course c = Course.getCourseByCode(cg.getGroupCodes().
                                                             iterator().next());
                    final int cid = c.getId();
                    prob.append(cid);
                    prob.append("_").append(s).append(" - ");
                    for (int t=0; t<=s-ks; t++) {
                        for (int j=0; j<N; j++) {
                            if (j==cid) continue;
                            Course cj = Course.getCourseById(j);
                            prob.append(cj.getCredits()).
                                  append(" x_").append(j).append("_").append(t);
                        }
                        if (t<s-ks) prob.append(" - ");
                        else prob.append(" <= 0\n");
                    }
                }
                // finally, the min number of concentration area courses 
                // constraint for the capstone project
                final int ncourses = cg.getMinNumCoursesReqd();
                Set<String> conc_courses = new HashSet<>();
                Iterator<String> groups_it = 
                        CourseGroup.getCourseGroupNameIterator();
                while (groups_it.hasNext()) {
                  String gs_name = groups_it.next();
                  if (gs_name.startsWith(concentration)) {
                      CourseGroup cg2 = 
                              CourseGroup.getCourseGroupByName(gs_name);
                      conc_courses.addAll(cg2.getGroupCodes());
                  }
                }
                for (int s=1; s<=Smax; s++) {
                    final int ks = Course.isSummerTerm(s) ? 3 : 1;
                    if (s-ks<0) continue;
                    prob.append("c").append(ccount).append(": ").
                        append(ncourses).append(" x_");
                    ++ccount;
                    final Course c = Course.getCourseByCode(cg.getGroupCodes().
                                                             iterator().next());
                    final int cid = c.getId();
                    prob.append(cid);
                    prob.append("_").append(s).append(" - ");
                    for (int t=0; t<=s-ks; t++) {
                        Iterator<String> cs_it = 
                                conc_courses.iterator();
                        while(cs_it.hasNext()) {
                            String cs = cs_it.next();
                            Course cc = Course.getCourseByCode(cs);
                            final int j = cc.getId();
                            if (j==cid) continue;
                            prob.append(" x_").append(j).append("_").append(t);
                            if (cs_it.hasNext() || t<s-ks) prob.append(" - ");
                            else prob.append(" <= 0\n");
                        }
                    }
                }
            }
        }
        // 2.15 fifteenth, the soft-order precedence constraints
        // notice that we don't enforce the "summer-term peculiarity" 
        // that would normally ask for course cj not to be taken during
        // ST if ci was taken on S1 or S2 of the same year, as it doesn't
        // appear to be significant to impose this as well.
        // Notice that for soft-order constraints, the number cn of minimum 
        // courses has the meaning that it is the maximum distance in terms
        // between ci and cj, so that if student takes both courses and 
        // takes ci in term t, they must take course cj by t + cn
        prob.append("\\ soft-order precedence constraints\n");
        gnamesit = CourseGroup.getCourseGroupNameIterator();
        while (gnamesit.hasNext()) {
            String gname = gnamesit.next();
            CourseGroup cg = CourseGroup.getCourseGroupByName(gname);
            if (cg.isSoftOrderPrecedenceConstraint()) {
                List<String> codes = cg.getGroupCodes();
                final int cn = cg.getMinNumCoursesReqd();
                Course ci = Course.getCourseByCode(codes.get(0));
                Course cj = Course.getCourseByCode(codes.get(1));
                for (int s=1; s<=Smax; s++) {
                    int cn2 = cn;
                    if (cn==0) cn2 = s;  // if cn is zero, there is no limit
                                         // in the time-distance between the 
                                         // two courses
                    prob.append("c"+ccount+": "); ++ccount;
                    prob.append("x_"+cj.getId()+"_"+s+" ");
                    for (int t=s-cn2; t<=s-1; t++) {
                        prob.append(" - x_"+ci.getId()+"_"+t);
                    }
                    prob.append(" + x_"+ci.getId());
                    prob.append(" <= 1\n");
                }
            }
        }
        // 2.16 sixteenth, the OU constraints that ask for an upper limit of 
        // OU courses taken every academic year (starting on a Fall term.)
        prob.append("\\ OU max #courses per academic year constraint\n");
        gnamesit = CourseGroup.getCourseGroupNameIterator();
        while (gnamesit.hasNext()) {
            String gname = gnamesit.next();
            CourseGroup cg = CourseGroup.getCourseGroupByName(gname);
            if (cg.isOUConstraint()) {
                List<String> codes = cg.getGroupCodes();
                int cnmax = cg.getMinNumCoursesReqd();  // this is a max value
                for (int s=1; s<=Smax; s++) {
                    if (Course.isFallTerm(s)) {
                        // for the min(s+4,Smax) terms, OU courses must be 
                        // no more than cnmax
                        int s_up_to = Math.min(s+4, Smax);
                        prob.append("c"+ccount+": "); ++ccount;
                        for (int s2 = s; s2<=s_up_to; s2++) {
                            for (int j=0; j<codes.size(); j++) {
                                Course c = Course.getCourseByCode(codes.get(j));
                                int cid = c.getId();
                                prob.append("x_"+cid+"_"+s2+" ");
                                if (j<codes.size()-1 || s2<s_up_to)
                                    prob.append(" + ");
                            }
                        }
                        prob.append(" <= "+cnmax+"\n");
                    }
                    else if (s==1) {  // constraints for current academic year
                        int cnmax2 = cnmax - num_OU_cur_academic_year;
                        int s_next_ST = Course.nextFallTerm(s)-1;
                        prob.append("c"+ccount+": "); ++ccount;
                        for (int s2 = s; s2<=s_next_ST; s2++) {
                            for (int j=0; j<codes.size(); j++) {
                                Course c = Course.getCourseByCode(codes.get(j));
                                int cid = c.getId();
                                prob.append("x_"+cid+"_"+s2+" ");
                                if (j<codes.size()-1 || s2<s_next_ST)
                                    prob.append(" + ");
                            }
                        }
                        prob.append(" <= "+cnmax2+"\n");                        
                    }
                }
            }
        }        
        // 2.17 seventeenth, the honor-student constraints -only for non-honor 
        //      students
        if (!isHonorStudent) {
            CourseGroup honor_cg = 
                    CourseGroup.getCourseGroupByName("HonorGroup");
            if (honor_cg!=null) {
                prob.append("\\ Honor Course constraints\n");
                Iterator<String> cs_it = honor_cg.getGroupCodes().iterator();
                while (cs_it.hasNext()) {
                    String cs = cs_it.next();
                    if (_passed.contains(cs)) continue;  // somehow, course has
                                                         // been passed already
                    Course ci = Course.getCourseByCode(cs);
                    prob.append("x_"+ci.getId()+" = 0\n");
                }
            }
        }
        // 2.18 eightteenth, the variable constraints
        prob.append("Binary\n");
        for (int i=0; i<N; i++) {
            prob.append("x_"+i+" ");
            for (int s=0; s<=Smax; s++) {
                prob.append("x_"+i+"_"+s+" ");
            }
            prob.append("\n");
        }
        // variable "Dx" and "G" can be continuous, so it's not declared at all
        // 2.19 finally, the END delimiter of all LP files
        prob.append("\nEnd");
        
        // 3. write the problem as an LP format file called "schedule.lp".
        try (PrintWriter pw = new PrintWriter(new FileWriter("schedule.lp"))) {
            pw.print(prob);
            pw.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // 4. the end!.
    }
    
    
    /**
     * solves the model in file "schedule.lp" and returns the results in a 
     * String to be displayed in the output area. Variable values get written
     * in file "schedule_result_vars.out".
     * @return String
     * @throws GRBException if GUROBI fails to solve the problem
     * @throws IOException if some I/O error occurs
     */
    public String optimizeSchedule() throws GRBException, IOException {
        _cid2tnoMap.clear();
        long start = System.currentTimeMillis();
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env, "schedule.lp");
        model.optimize();
        int optimstatus = model.get(GRB.IntAttr.Status);
        if (optimstatus!=GRB.Status.OPTIMAL) {
            return "Model infeasible (or could not be solved)";
        }
        long dur = System.currentTimeMillis()-start;
        String dstr = "Schedule computed in "+dur+" msecs.\n";
        int num_credits_taken = 0;
        int num_credits_to_take = 0;
        int total_credits = 0;
        HashMap<Integer, List<String>> sem_courses_map = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        sb.append(dstr);
        PrintWriter pwr = 
                new PrintWriter(new FileWriter("schedule_result_vars.out"));
        // get the names of all variables of the form "x_i" that are set to 1
        Set<Integer> all_sol_varids = new HashSet<>();
        for (GRBVar v : model.getVars()) {
            String vname = v.get(GRB.StringAttr.VarName);
            int vval = (int) v.get(GRB.DoubleAttr.X);
            if (vval==1) {
                String[] xcomps = vname.split("_");
                if (xcomps.length==2 && vname.startsWith("x")) 
                    all_sol_varids.add(Integer.parseInt(xcomps[1]));
            }
        }
        for (GRBVar v : model.getVars()) {
            String vname = v.get(GRB.StringAttr.VarName);
            int vval = (int) v.get(GRB.DoubleAttr.X);
            pwr.println(vname+"="+vval);
            if (vval==1) {
                // parse name
                String[] xcomps = vname.split("_");
                if (xcomps.length<3) 
                    continue;  // it's not the x_i_s vars that we want 
                int vid = Integer.parseInt(xcomps[1].trim());
                int termno = Integer.parseInt(xcomps[2].trim());
                _cid2tnoMap.put(vid, termno);  // add variable to solution map
                Course cv = Course.getCourseById(vid);
                if (termno>=1) num_credits_to_take += cv.getCredits();
                else num_credits_taken += cv.getCredits();
                total_credits += cv.getCredits();
                String course_descr = cv.getScheduleDisplayName();
                if (course_descr==null || course_descr.length()<=1 ||
                    _desired.contains(cv.getCode()) || 
                    cv.isRequired4Desired(_desired, all_sol_varids))  
                    // if user selected course or if course is needed for such
                    // course, show it with full name in schedule
                    course_descr = cv.toString();
                // String term_desc = Course.getTermNameByTermNo(termno);
                //sb.append(term_desc).append(course_descr).append("\n");
                List<String> sem_courses = sem_courses_map.get(termno);
                if (sem_courses==null) {
                    sem_courses = new ArrayList<>();
                    sem_courses_map.put(termno, sem_courses);
                }
                sem_courses.add(course_descr);
            }
        }
        pwr.flush();
        pwr.close();
        final int Smax = _params.getSmax();
        sb.append("\n----- Credits Taken So Far\t: "+num_credits_taken);
        sb.append("\n----- Credits To Take Yet\t: "+num_credits_to_take);
        sb.append("\n----- TOTAL CREDITS OVERALL\t: "+total_credits+"\n");
        for (int s=1; s<=Smax; s++) {
            List<String> crs_lst = sem_courses_map.get(s);
            if (crs_lst!=null) {
                String sem_descr="     --- "+Course.getTermNameByTermNo(s)+
                                 " ---\n";
                sb.append(sem_descr);
                for (String c : crs_lst) sb.append(c+"\n");
            }
        }
        sb.append("End\n");
        return sb.toString();
    }
    
    
    /**
     * return a copy of the last computed solution. The solution is returned as
     * a map from course-id (not course-code), to the term-number during which
     * the course is to be taken; if a course-id does not appear in the map keys
     * the course is not part of the optimal schedule. See the static method
     * <CODE>Course.getTermNameByTermNo()</CODE> for a method to obtain the 
     * full name of a term (such as "FA2022") given its term number.
     * @return HashMap&lt;Integer, Integer&gt;
     */
    public HashMap<Integer, Integer> getLastOptimalSolution() {
        return new HashMap<>(_cid2tnoMap);
    }
}

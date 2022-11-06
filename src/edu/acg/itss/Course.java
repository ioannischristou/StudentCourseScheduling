package edu.acg.itss;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.*;
import javax.swing.JOptionPane;


/**
 * class is responsible for maintaining all relevant course data. Estimated 
 * grades for individual students (from QARMA) may be recorded in a file called
 * "estimated_grades.txt" whose lines are comma separated 
 * &lt;course-code&gt;,&lt;estimated-value&gt;
 * pairs of the estimates on how the student will do on some (usually not all) 
 * courses (one course per line).
 * The <CODE>CourseEditor</CODE> class provides the GUI for editing (adding,
 * modifying and/or deleting) courses in the appropriate "cls.csv" file where 
 * courses are stored.
 * @author itc
 */
public class Course implements Comparable {
    /**
     * needed to retrieve a Course based on its code. Iterators return keys 
     * sorted in alphabetical order.
     */
    private static final TreeMap<String, Course> _allCoursesMap=new TreeMap<>();
    /**
     * needed to retrieve a Course based on its id (contiguous value starting at
     * zero).
     */
    private static final HashMap<Integer, Course> _id2CrsMap = new HashMap<>();
    /**
     * id counter starting at zero.
     */
    private static int _curId = 0;
    
    /**
     * needed as index for the variables x_{i,s} and x_i respectively.
     */
    private final int _id;  
    /**
     * the course code is a string such as "ITC3160" or "MA1088" etc.
     */
    private final String _code;
    /**
     * the name of the course is a string such as "Object Oriented Programming".
     */
    private final String _name;
    /**
     * the name that is supposed to appear in the final result schedule, if it
     * is not null nor empty. This will be true only for LE-type courses, which 
     * will have display names such as "Humanities LE course" etc.
     */
    private final String _scheduleDisplayName;
    /**
     * the number of credits for this course, usually 3, sometimes 4, less often
     * other number.
     */
    private final int _credits;
    /**
     * this should normally be empty.
     */
    private final Set<String> _synonymCodes;
    /**
     * the pre-requisites are in CNF, meaning they are a CONJUNCTION of 
     * DISJUNCTIONS. Each disjunction is a set of codes, and every such set in 
     * the set of sets of codes must be satisfied.
     */
    private final Set<Set<String>> _prereqs;
    /**
     * the co-requisites are just a set of courses that must be taken already
     * or otherwise in the same term as this course. They do not have the 
     * complex structure of pre-requisites.
     */
    private final Set<String> _coreqs;
    /**
     * a string representing the terms when the course is offered, that can be
     * "alltimes" or "everyfall" or "S12022 FA2022" etc. This data member is
     * what is read from the "cls.csv" file when the method 
     * readAllCoursesFromFile() executes.
     */
    private final String _toff;
    /**
     * the numbers in this list are from the set {s_1, s_2,...S_{max}}
     * where s_0 is the current term. This list remains null until needed.
     */
    private List<Integer> _termsOffered=null;
    /**
     * optional difficulty level of the course is an integer assumed to be in 
     * the range [0,10] (10=MAX_DIFFICULTY).
     */
    private final int _difficultyLevel;
    
    /**
     * optional estimated grade from the results of QARMA is a float in [0,4.0].
     * For this field only, <CODE>setEstimatedGrade(value)</CODE> setter method
     * exists.
     */
    private float _estimatedGrade = 0.0f;
    
    
    /**
     * class constructor is private, and main way to create <CODE>Course</CODE> 
     * objects is through the static method <CODE>createCourse()</CODE> which 
     * adds the constructed object in the appropriate static maps as well. Also,
     * the only other way to create such objects is <CODE>modifyCourse()</CODE>
     * that is used by the <CODE>CourseEditor</CODE> class.
     * @param code String
     * @param name String
     * @param synonyms Set&lt;String&gt;
     * @param credits int
     * @param prereqs Set&lt;Set&lt;String&gt;&gt;
     * @param coreqs Set&lt;String&gt;
     * @param toff String the terms when the course is offered
     * @param scheduleDisplayName String may be null
     * @param difficulty_level int default is 0
     */
    private Course(String code, 
                  String name, Set<String> synonyms,
                  int credits, 
                  Set<Set<String>> prereqs, Set<String> coreqs, 
                  String toff,
                  String scheduleDisplayName,
                  int difficulty_level) {
        _code = code;
        _name = name;
        _credits = credits;
        _synonymCodes = new HashSet<>(synonyms);
        _prereqs = new HashSet<>(prereqs);
        _coreqs = new HashSet<>(coreqs);
        _toff = toff;
        _scheduleDisplayName = scheduleDisplayName;
        _difficultyLevel = difficulty_level;
        _id = getNextId();
    }
    
    
    /**
     * private constructor used only by <CODE>modifyCourse()</CODE> method, 
     * which in turn is only needed by the <CODE>CourseEditor</CODE> class.
     * @param id String such as "34"
     * @param code String such as "MA2010"
     * @param name String such as "Statistics for Business"
     * @param synonyms String such as "ITC4188 ITC4088"
     * @param credits String such as "3"
     * @param prereqs String such as "ITC2070+ITC1080,ITC3160"
     * @param coreqs String such as "ITC4188 ITC4053"
     * @param termsOffered String such as "FA2023 everyspring" or "next2terms"
     * @param scheduleDisplayName String such as "LE in Humanities"
     * @param difficulty_level String such as "0"
     * @param Smax int usually between 15 and 25
     */
    private Course(String id, String code, String name, String synonyms, 
                   String credits,
                   String prereqs, String coreqs, String termsOffered,
                   String scheduleDisplayName, String difficulty_level, 
                   int Smax) {
        _id = Integer.parseInt(id);
        _code = code;
        _name = name;
        if (synonyms.trim().length()>0) {
            String[] synsarr = synonyms.split(" ");
            _synonymCodes = new HashSet<>(Arrays.asList(synsarr));
        } else _synonymCodes = new HashSet<>();
        _credits = Integer.parseInt(credits);
        _prereqs = new HashSet<>();
        if (prereqs.trim().length()>0) {
            String[] prearr = prereqs.split(",");
            for (String pr : prearr) {
                String[] ors = pr.split("\\+");
                Set<String> ps = new HashSet<>(Arrays.asList(ors));
                _prereqs.add(ps);
            }
        }
        _coreqs = new HashSet<>();
        if (coreqs.trim().length()>0) {
            String[] coarr = coreqs.split(" ");
            if (coarr.length>0) _coreqs.addAll(Arrays.asList(coarr));
        }
        _toff = termsOffered;
        _scheduleDisplayName = scheduleDisplayName;
        _difficultyLevel = Integer.parseInt(difficulty_level);
        // finally, update _curId
        if (_curId<=_id) _curId = _id+1;
    }
    
    
    /**
     * reads all courses from a text (CSV) file, and stores them in the class 
     * data. The file is normally named "cls.csv", and lives in an appropriate
     * sub-directory of the root directory of the app (which must be passed as
     * input argument from the cmd-line of the appropriate class program, such 
     * as <CODE>MainGUI</CODE> or <CODE>CourseEditor</CODE>.
     * The file read must have the following format: 
     * There will be one line for each course, and the line will have the 
     * following format:
     * <PRE>
     * &lt;code&gt; ; &lt;name&gt; ; [synonymcode ]* ; &lt;credits&gt; ;
     * [prereqcodesCNF[,]]* ; [coreqcode ]* ; &lt;[termOffered ]* | - &gt;
     * [;schedulename][;difficultylevel]
     * </PRE>
     * where prereqcodesCNF is a Conjunctive Normal Form formula written as
     * [&lt;code&gt;[+]]*. Here is an example:
     * <PRE>
     * ITC2088+ITC2197,ITC2197+ITC3234
     * </PRE>
     * which means that two constraints must hold: student must have taken
     * (ITC2088 OR ITC2197), AND student must have taken (ITC2197 OR ITC3234).
     * The terms offered are strings of the following format:
     * <PRE>
     * &lt;FA | SP | S1 | S2 | ST&gt;&lt;YYYY&gt;
     * </PRE>
     * Alternatively, the terms offered can be simply the string "alltimes" in
     * which case the course is available every term from s=1...Smax, or the 
     * string "everyfall" or "everyspring" or "everysummerterm", with the 
     * obvious meaning of the words. It can also be "next2terms" or "next4terms"
     * Finally, if the course is not offered at all, the character "-" must be 
     * provided in that space. Otherwise, given the current term (which is 
     * computed from the current date-time), the number of each term is derived 
     * accordingly.
     * <p>Notice that any line starting with the hash-sign (#) is a comment line
     * and is ignored. The first line will always start with the # sign and will
     * contain the header of the file.
     * @param filename String the name of the file to read all course data 
     * @param Smax int the maximum term number the student has in front of them
     */
    public static void readAllCoursesFromFile(String filename, int Smax) {
        // course description in a CSV file (semi-column separated values) with
        // the following format:
        // <code>;<name>;[aka ]*;<credits>;[prereqCNF[,]]*;[coreq ]*;[term ]* |-
        // [;schedulename][;difficultylevel]
        String line=null;
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while (true) {
                line = br.readLine();
                if (line==null) break;  // EOF
                if (line.startsWith("#")) continue;  // comment line
                String[] linearr = line.split(";");
                String code = linearr[0];
                String name = linearr[1];
                String syns = linearr[2];
                Set<String> synset = new HashSet<>();
                String[] synsarr = syns.split(" ");
                if (synsarr.length>1 || synsarr[0].length()>0) {
                    synset.addAll(Arrays.asList(synsarr));
                }
                int c = Integer.parseInt(linearr[3]);
                Set<Set<String>> prereqs = new HashSet<>();
                String[] pres = linearr[4].split(",");
                if (pres.length>1 || pres[0].length()>0) {
                    for (String df : pres) {
                        String[] dfs = df.split("\\+");
                        Set<String> ps = new HashSet<>();
                        ps.addAll(Arrays.asList(dfs));
                        prereqs.add(ps);
                    }
                }
                Set<String> coreqs = new HashSet<>();
                String[] cores = linearr[5].split(" ");
                if (cores.length>1 || cores[0].length()>0) {
                    coreqs.addAll(Arrays.asList(cores));
                }
                String toff = "-";
                if (linearr.length>6) {
                    toff = linearr[6];
                }
                String schedule_display_name = null;
                if (linearr.length>7) {
                    schedule_display_name = linearr[7];
                }
                int diff_lvl = 0;
                if (linearr.length>8) {
                    try {
                        diff_lvl = Integer.parseInt(linearr[8]);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("FOR Line="+line);
                        System.err.println("couldn't parse difficulty level,"+
                                           " will stay at zero");
                    }
                }
                Course crs = Course.createCourse(code, name, synset, c, 
                                                 prereqs, coreqs, toff,
                                                 schedule_display_name,
                                                 diff_lvl);
                if (crs.getId()+1!=_allCoursesMap.size()) {
                    throw new IllegalStateException("in line="+line+
                                                    " counts don't add up");
                }
            }
            System.err.println("Created a total of "+_curId+" courses");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("offending line="+line);
            System.exit(-1);
        }        
    }
    
    
    /**
     * call this method instead of calling the private constructor to create
     * a Course object. It provides an id for the created object and it adds it
     * to the static maps by which the object can be retrieved. 
     * It does NOT add to the map <CODE>_allCoursesMap</CODE> the key-value pair 
     * (synonym,this) for each synonym the course has.
     * @param code String
     * @param name String
     * @param synonyms Set&lt;String&gt;
     * @param credits int
     * @param prereqs Set&lt;Set&lt;String&gt;&gt; every element of the prereqs
     * set is a set of strings representing course codes, at least one of which
     * must be passed by the student before the student can take the course we
     * are creating
     * @param coreqs Set&lt;String&gt; every element of the coreqs set must have
     * been passed or must be taken simultaneously with the course we are 
     * creating
     * @param termsOffered String may describe more than one term, separated by
     * space, such as "FA2022 SP2023" or "FA2022 everyspring"; "-" string 
     * means the course is not offered.
     * @param scheduleDisplayName String may be null if course is not an LE
     * type course
     * @param difficulty_level int
     * @return Course
     */
    public static Course createCourse(String code, 
                                      String name, Set<String> synonyms, 
                                      int credits, 
                                      Set<Set<String>> prereqs, 
                                      Set<String> coreqs, 
                                      String termsOffered,
                                      String scheduleDisplayName,
                                      int difficulty_level) {
        Course c = new Course(code, name, synonyms, credits, prereqs, coreqs, 
                              termsOffered, scheduleDisplayName, 
                              difficulty_level);
        _allCoursesMap.put(code, c);
        /*
        for (String s : synonyms) {
            _allCoursesMap.put(s, c);
        }
        */
        _id2CrsMap.put(c._id, c);
        return c;
    }
    
    
    /**
     * package-access method allows to modify a Course object, which is only
     * required by the <CODE>CourseEditor</CODE>.
     * @param id String
     * @param code String
     * @param name String
     * @param synonyms String
     * @param credits String
     * @param prereqs String
     * @param coreqs String
     * @param termsoffered String
     * @param scheduleDisplayName String
     * @param difficulty_level String
     * @param Smax int
     * @return Course the new object created and stored for the given id to the
     * hash-maps storing Course objects. If any errors in the arguments exist,
     * it returns null, and leaves the static maps intact
     */
    static Course modifyCourse(String id, String code, String name,
                                      String synonyms,
                                      String credits, 
                                      String prereqs,
                                      String coreqs,
                                      String termsoffered,
                                      String scheduleDisplayName,
                                      String difficulty_level,
                                      int Smax) {
        try {
            Course c = new Course(id, code, name, synonyms, credits,
                                  prereqs, coreqs, 
                                  termsoffered,
                                  scheduleDisplayName,
                                  difficulty_level,
                                  Smax);
            _allCoursesMap.put(code, c);
            _id2CrsMap.put(c._id, c);
            return c;
        }
        catch (Exception e) {
            JOptionPane.showConfirmDialog(null, "at least one argument wrong");
            return null;
        }
    }
    
    
    /**
     * deletes the course identified by the number represented by the string
     * idstr. All courses with higher ids have their id decremented by 1.
     * @param idstr String such as "5"
     * @param Smax int the maximum number of terms a schedule can be created for
     */
    static void deleteCourse(String idstr, int Smax) {
        int id = Integer.parseInt(idstr);
        Course c = Course.getCourseById(id);
        _allCoursesMap.remove(c._code);
        _id2CrsMap.remove(id);
        // decrement the ids of all courses above it by 1
        final int last_id = getLastId();
        for (int i=id+1;i<=last_id; i++) {
            Course ci = Course.getCourseById(i);
            // create the synonyms
            String synonyms = "";
            for (String s : ci._synonymCodes) {
                synonyms += s +" ";
            }
            // create the prereqs
            String prereqs = "";
            Iterator<Set<String>> ss_it = ci._prereqs.iterator();
            while (ss_it.hasNext()) {
                Set<String> ss = ss_it.next();
                Iterator<String> sit = ss.iterator();
                while (sit.hasNext()) {
                    prereqs += sit.next();
                    if (sit.hasNext()) prereqs += "+";
                }
                if (ss_it.hasNext()) prereqs += ",";
            }
            String coreqs = "";
            for (String s : ci._coreqs) {
                coreqs += s +" ";
            }
            String termsoffered = "";
            for (int to : ci._termsOffered) {
                termsoffered += Course.getTermNameByTermNo(to) + " ";
            }
            Course ci_new = 
                    Course.modifyCourse(Integer.toString(i-1), 
                                        ci._code, ci._name,
                                        synonyms,
                                        Integer.toString(ci._credits),
                                        prereqs, coreqs,
                                        termsoffered,
                                        ci._scheduleDisplayName,
                                        Integer.toString(ci._difficultyLevel),
                                        Smax);          
            // finally, update _curId
            _curId = Course.getNumCourses();
        }
    }
    
    
    /**
     * returns the next id to assign to a Course object.
     * @return int
     */
    private static int getNextId() {
        return _curId++;
    }
    
    
    /**
     * returns the last id handed out by the system. This may not be the same as
     * <CODE>Course.getNumCourses()</CODE> in the <CODE>CourseEditor</CODE>
     * program.
     * @return int maybe -1 if no course has been constructed yet
     */
    static int getLastId() {
        return _curId-1;
    }
    
    
    /**
     * check if given id is the first one corresponding to a course in the class
     * maps.
     * @param id int
     * @return true iff the given id exists in the map and there is no smaller 
     * id in the maps
     */
    static boolean isFirst(int id) {
        if (Course.getCourseById(id)==null) return false;
        for (int i=0; i<id; i++) {
            if (Course.getCourseById(i)!=null) return false;
        }
        return true;
    }
    
    
    /**
     * check if given id is the last one corresponding to a course in the class
     * maps.
     * @param id int
     * @return true iff the given id exists in the map and there is no larger id 
     * in the maps
     */
    static boolean isLast(int id) {
        if (Course.getCourseById(id)==null) return false;
        for (int i=id+1; i<_curId; i++) {
            if (Course.getCourseById(i)!=null) return false;
        }
        return true;
    }
    
    
    /**
     * retrieve a Course object by its unique integer identifier.
     * @param n int the object id
     * @return Course may be null if the number n is less than zero or higher
     * than the total number of courses created so far.
     */
    public static Course getCourseById(int n) {
        return _id2CrsMap.get(n);
    }
    
    
    /**
     * retrieve a Course object by its unique String code (eg "ITC3234"). 
     * Notice that in this version, providing a synonym for a course won't work
     * unless the synonym is also provided in the list of courses as a separate
     * course.
     * @param code String
     * @return Course may be null
     */
    public static Course getCourseByCode(String code) {
        return _allCoursesMap.get(code);
    }
    
    
    /**
     * return an iterator over the course codes encountered so far. Notice that
     * synonym codes for courses are not going to appear unless they appear as
     * courses in the list of courses created.
     * @return Iterator&lt;String&gt;
     */
    public static Iterator<String> getAllCodesIterator() {
        return _allCoursesMap.keySet().iterator();
    }
    
    
    /**
     * get the total number of courses created so far.
     * @return int
     */
    public static int getNumCourses() {
        return _allCoursesMap.size();
    }
    
    
    /**
     * return the id of this Course.
     * @return int
     */
    public int getId() { return _id; }
    
    
    /**
     * return the title of this course, eg "Object Oriented Programming".
     * @return String
     */
    public String getName() { return _name; }
    
    
    /**
     * return the code of this course, eg "ITC3234".
     * @return String
     */
    public String getCode() { return _code; }
    
    
    /**
     * return the number of credits of this course.
     * @return int
     */
    public final int getCredits() { return _credits; }
    
    
    /**
     * return the synonym codes of this course.
     * @return Set&lt;String&gt;
     */
    public final Set<String> getSynonymCodes() { return _synonymCodes; }
    
    
    /**
     * check whether this course is "equivalent" to the course with the given 
     * code.
     * @param code String
     * @return boolean true iff code is in the synonyms of this Course.
     */
    public final boolean isSynonym4Course(String code) {
        return _synonymCodes.contains(code);
    }
    
    
    /**
     * return the set of pre-requisites for this Course in CNF: each set in the 
     * returned set is comprised of courses, at least one of which must be 
     * passed; this must be true of every set in the returned set. As an example
     * if the result is the set {{"ITC2088","ITC1077"}, {"ITC3234"}} the 
     * prerequisites for this course are (ITC2088 OR ITC1077) AND (ITC3234).
     * @return Set&lt;Set&lt;String&gt;&gt;
     */
    public final Set<Set<String>> getPrereqs() { return _prereqs; }
    
    
    /**
     * return the set of co-requisite course codes for this Course.
     * @return Set&lt;String&gt;
     */
    public final Set<String> getCoreqs() { return _coreqs; }
    
    
    /**
     * return the list of terms that this course is offered. The numbers in the
     * returned list are all greater than zero, and less than Smax (the maximum
     * number of semesters the student may still register for). The number 1 
     * indicates the course is offered in the immediate next term whatever this 
     * term may be, so that if now is Spring 2023, the number 1 indicates the 
     * course is offered in Summer1 2023.
     * Note: the CurrentDate struct must have been correctly set before calling
     * this method.
     * @param Smax int the maximum number of semesters the student may register
     * for still
     * @return List&lt;Integer&gt;
     */
    public final List<Integer> getTermsOffered(int Smax) {
        //if (_termsOffered!=null) return _termsOffered;
        // the line above cannot work correctly when CurrentDate is modified
        // from the GUI right before hitting "Run", as the change in date will
        // not affect at all the numeric terms that the course is offered.
        // For this reason, we cannot have a cache for the termsOffered.
        _termsOffered = new ArrayList<>();
        String tot = _toff.trim();  // termsOffered cannot be null
        if (!"-".equals(tot)) { 
            String[] toarr = tot.split(" ");
            for (String to : toarr) {
                to = to.trim();
                if ("alltimes".equals(to)) {
                    _termsOffered.clear();
                    for (int s=1; s<=Smax; s++) _termsOffered.add(s);
                }
                else if ("everyfall".equals(to)) {
                    for (int s=1; s<=Smax; s++) {
                        String tstr = Course.getTermNameByTermNo(s);
                        if (tstr.startsWith("FA")) _termsOffered.add(s);
                    }
                }
                else if ("everyspring".equals(to)) {
                    for (int s=1; s<=Smax; s++) {
                        String tstr = Course.getTermNameByTermNo(s);
                        if (tstr.startsWith("SP")) _termsOffered.add(s);
                    }
                }
                else if ("everysummerterm".equals(to)) {
                    for (int s=1; s<=Smax; s++) {
                        String tstr = Course.getTermNameByTermNo(s);
                        if (tstr.startsWith("ST")) _termsOffered.add(s);
                    }
                }
                else if ("next2terms".equals(to)) {
                    for (int s=1; s<=2; s++) {
                        _termsOffered.add(s);
                    }
                }
                else if ("next4terms".equals(to)) {
                    for (int s=1; s<=4; s++) {
                        _termsOffered.add(s);
                    }
                }
                else {  // to must be like "FA2020"
                    int t = Course.getTermNo(to);
                    if (t>0) _termsOffered.add(t);
                }
            }
        }  // termsOffered
        return _termsOffered;
    }
    
    
    /**
     * return the display name of this course in the final result schedule. If 
     * it is null, the original name of the course must be used.
     * @return String may be null
     */
    public final String getScheduleDisplayName() {
        return _scheduleDisplayName;
    }
    
    
    /**
     * get the difficulty level of this Course (to be used in min-max assignment
     * criteria for courses in any term).
     * @return int
     */
    public final int getDifficultyLevel() {
        return _difficultyLevel;
    }
    
    
    /**
     * get the estimated grade of a particular student for this Course. This 
     * number is read from the file "estimated_grades.txt" when it exists.
     * @return float number in [0,4] eg 3.5.
     */
    public final float getEstimatedGrade() {
        return _estimatedGrade;
    }
    
    
    /**
     * set the estimated grade of a particular student for this Course. This 
     * number is read from the file "estimated_grades.txt" when it exists.
     * @param v float number in [0,4] eg 3.5.
     */
    public final void setEstimatedGrade(float v) {
        if (v<0 || v>4.0f) 
            throw new IllegalArgumentException("float value must be in [0,4]");
        _estimatedGrade = v;
    }
    
    
    /**
     * check if the course with given code is required for this Course (if it is
     * an "ancestor", prerequisite-wise, even if it is only inside a disjunction
     * of possible courses to take.) Takes co-reqs into account too.
     * @param code String such as "ITC3234"
     * @return boolean true iff the course with the code passed in as argument
     * is a direct prerequisite for this course, or if it is a requirement for
     * another course that is eventually required by this course.
     */
    public final boolean requiresCourse(String code) {
        for (Set<String> ss : _prereqs) {
            if (ss.contains(code)) return true;
            // else
            for (String s : ss) {
                Course cs = Course.getCourseByCode(s);
                if (cs.requiresCourse(code)) return true;
            }
        }
        // handle co-requisites also!
        for (String ss : _coreqs) {
            if (code.equals(ss)) return true;
            // else
            Course cs = Course.getCourseByCode(ss);
            if (cs.requiresCourse(code)) return true;
        }
        return false;
    }
    
    
    /**
     * checks if code is required so that this course is in the solution, taking
     * into account the actual solution plan. The requirement here is taken in 
     * the strict sense, so for example if this course is ITC2205 whose 
     * prerequisites are ITC2088 AND (ITC2197 OR ITC3234), then ITC2088 always
     * returns true (in any schedule), but ITC2197 will return true only if 
     * ITC3234 is missing from the schedule, and false otherwise, and vice-versa
     * for ITC3234.
     * @param code String such as "ITC3160"
     * @param solVarIds Set&lt;Integer&gt; containing the ids of the courses in
     * the current solution
     * @return boolean true iff code is actually required for this course to be
     * in the solution
     */
    private final boolean scheduleRequiresCourse(String code, 
                                                 Set<Integer> solVarIds) {
        // include co-requisites
        Set<Set<String>> allreqs = new HashSet<>(_prereqs);
        for (String cr : _coreqs) {
            HashSet<String> crset = new HashSet<>();
            crset.add(cr);
            allreqs.add(crset);
        }
        for (Set<String> ss : allreqs) {
            if (ss.contains(code)) {
                // make sure ss is either just 1 course, or the other courses
                // in ss are not in solution
                if (ss.size()==1) return true;
                boolean needed = true;
                for (String s : ss) {
                    if (s.equals(code)) continue;
                    Course cs = Course.getCourseByCode(s);
                    if (solVarIds.contains(cs.getId())) {
                        needed = false;
                        break;
                    }
                }
                if (needed) return true;
            }
            // we can't say yet that code is required
            // check if every course in ss requires code:
            // if so, code is needed
            int num_needing = 0;
            for (String s : ss) {
                Course cs = Course.getCourseByCode(s);
                if (cs.scheduleRequiresCourse(code, solVarIds)) ++num_needing;
            }
            if (num_needing==ss.size()) return true;
        }
        return false;        
    }
    
    
    /**
     * checks if this course is actually required (in the strict sense according
     * to the current solution) by any of the desired courses selected by the 
     * student.
     * @param desired DesiredCourses
     * @param solVarIds Set&lt;Integer&gt; the ids of the courses in the current
     * solution
     * @return boolean true if this course is needed by any of the desired 
     * courses in the solution
     */
    public final boolean isRequired4Desired(DesiredCourses desired, 
                                          Set<Integer> solVarIds) {
        Iterator<String> dcodesit = desired.getDesiredCourseCodesIterator();
        while (dcodesit.hasNext()) {
            String dcode = dcodesit.next();
            Course d = Course.getCourseByCode(dcode);
            if (!solVarIds.contains(d.getId())) continue;  // d is undesired
            if (d.scheduleRequiresCourse(getCode(), solVarIds)) return true;
        }
        return false;
    }
        
    
    /**
     * return a string of the following format:
     * &lt;code&gt; [(aka [code ]*)] &lt;name&gt;
     * @return String
     */
    @Override
    public String toString() { 
        String result = _code;
        if (_synonymCodes.size()>=1) {
            result += " (aka";
            result = _synonymCodes.stream().
                                     map(s -> " "+s).
                                       reduce(result, String::concat);
            result += ")";
        }
        result += " "+_name;
        return result;
    }
    
    
    /**
     * return a representation of this <CODE>Course</CODE> object that matches
     * precisely its representation in the "cls.csv" file that stores courses.
     * @param Smax int needed for computing the terms the courses are offered as
     * numbers (1...Smax).
     * @return String
     */
    public String getFullDetailsString(int Smax) {
        if (CurrentDate._curYear==0) {  // need to initialize the CurrentDate
            LocalDate now = LocalDate.now();
            CurrentDate._curDay = now.getDayOfMonth();
            CurrentDate._curMonth = now.getMonthValue();
            CurrentDate._curYear = now.getYear();
        }
        StringBuffer sb = new StringBuffer();
        sb.append(_code).append(";").append(_name).append(";");
        // now the synonyms
        for (String s : _synonymCodes) sb.append(s).append(" ");
        sb.append(";");
        // now the credits
        sb.append(_credits).append(";");
        // now the prerequisites
        Iterator<Set<String>> ps_it = _prereqs.iterator();
        while (ps_it.hasNext()) {
            Set<String> ps = ps_it.next();
            Iterator<String> cit = ps.iterator();
            while (cit.hasNext()) {
                String code = cit.next();
                sb.append(code);
                if (cit.hasNext()) sb.append("+");
            }
            if (ps_it.hasNext()) sb.append(",");
        }
        sb.append(";");
        // now the corequisites
        Iterator<String> cit = _coreqs.iterator();
        while (cit.hasNext()) {
            String code = cit.next();
            sb.append(code);
            if (cit.hasNext()) sb.append(" ");
        }
        sb.append(";");
        // now the terms offered: first call the method getTermsOffered to 
        // get the list of integers, then run the rest of the code
        getTermsOffered(Smax);
        if (_termsOffered!=null && _termsOffered.size()>0) {
            for (Integer i : _termsOffered) {
                String code = getTermNameByTermNo(i);
                sb.append(code).append(" ");
            }
        }
        else sb.append("-");
        sb.append(";");
        // now the display schedule name if it exists
        if (_scheduleDisplayName!=null && _scheduleDisplayName.length()>1) {
            sb.append(_scheduleDisplayName);
        }
        sb.append(";");
        if (_difficultyLevel>0) {
            sb.append(_difficultyLevel);
        }
        return sb.toString();
    }
    

    /**
     * overrides the <CODE>equals()</CODE> method.
     * @param other Object must be a Course object.
     * @return boolean true iff the two Course objects have the same id
     */
    public boolean equals(Object other) {
        if (!(other instanceof Course)) return false;
        Course o = (Course) other;
        return _id == o._id;
    }
    
    
    /**
     * Joshua Bloch's recommended method for computing effective hash-codes.
     * In this implementation we're only using the <CODE>_id</CODE> field, as 
     * it's the only one used in the <CODE>equals()</CODE> method too.
     * @return int
     */
    public int hashCode() {
        int result = 17;
	int c = (int)(_id ^ (_id >>> 32));
	result = 31*result + c;
        return result;     
    }
    
    
    /**
     * compares courses according to their code (alphabetical order).
     * @param other Object must be a <CODE>Course</CODE> object
     * @return int the result of the comparison of the two objects' codes.
     */
    public int compareTo(Object other) {
        if (other instanceof Course) {
            Course o = (Course) other;
            return _code.compareTo(o.getCode());
        }
        throw new IllegalArgumentException("argument not a Course object");
    }
    
    
    /**
     * extract the first letters before the number of the course, that signify
     * the "discipline" the course belongs to (eg "ITC" or "PS"). Notice that 
     * any appearances of the '/' character is ignored as LP-format names cannot
     * have such characters.
     * @param course String such as "ITC3234"
     * @return String such as "ITC"
     */
    public static String getProgramCode(String course) {
        StringBuffer sb = new StringBuffer("");
        for (int i=0; i<course.length(); i++) {
            final char c = course.charAt(i);
            if (Character.isDigit(course.charAt(i))) break;
            if (c=='/') continue;
            sb.append(c);
        }
        return sb.toString();
    }
    
    
    /**
     * reset all course data to prepare re-loading.
     */
    public static void reset() {
        Course._allCoursesMap.clear();
        Course._id2CrsMap.clear();
        Course._curId = 0;
    }

    
    /**
     * return an int representing the term (semester) from now that the course
     * will be offered.
     * @param term String must be in the format "S12023" meaning "Summer-1" of
     * 2023.
     * @return int &ge; 0
     */
    public static int getTermNo(String term) {
        // 1. get the current date
        int cur_day = CurrentDate._curDay;
        int cur_mon = CurrentDate._curMonth;
        int cur_year = CurrentDate._curYear;
        // 2. compute current term
        //    Fall (5) starts on Sep. 1
        //    Spring (1) starts on Jan. 6
        //    S1 (2) starts on Jun 1
        //    S2 (3) starts on Jul 1
        //    ST (4) starts on Jun 1
        int cur_term;
        if (cur_mon==1) {
            if (cur_day<6) cur_term = 5;
            else cur_term = 1;
        }
        else if (cur_mon < 6) cur_term = 1;
        else if (cur_mon < 9) cur_term = 4;
        else if (cur_mon <= 12) cur_term = 5;
        else throw new Error("cur_term not computable???");
        // 3. compute term number based on season and year
        String season = term.substring(0, 2);
        int year = Integer.parseInt(term.substring(2));
        if (year<cur_year) return 0;
        else if (year==cur_year) {
            if ("FA".equals(season)) {
                if (cur_mon<6) return 4;
                else if (cur_mon<9) return 1;
                else return 0;
            }
            else if ("SP".equals(season)) {
                return 0;
            }
            else if ("S1".equals(season)) {
                if (cur_mon<6) return 1;
                else return 0;
            }
            else if ("S2".equals(season)) {
                if (cur_mon<7) return 2;
                else return 0;
            }
            else {  // season=="ST"
                if (cur_mon<6) return 3;
                else return 0;
            }
        }
        else {  // year>cur_year
            int dif = year - cur_year;  // >= 1
            int termno = getTermNumberByTermName(season);
            return dif*5 + termno - cur_term;
        }
    }
    
    
    /**
     * check whether a given term in the range [0, ..., Smax] is a summer term
     * (ST). Notice that Summer Term is not the same as Summer1 or Summer2.
     * @param termno int
     * @return boolean true iff termno corresponds to ST
     * @throws IllegalArgumentException if termno &lt; 0
     */
    public static boolean isSummerTerm(int termno) {
        if (termno < 0)
            throw new IllegalArgumentException("termno="+termno+
                                               " must be non-negative...");
        // 1. get the current date
        int cur_day = CurrentDate._curDay;
        int cur_mon = CurrentDate._curMonth;
        int cur_year = CurrentDate._curYear;
        // 2. compute current term
        //    Fall (5) starts on Sep. 1
        //    Spring (1) starts on Jan. 6
        //    S1 (2) starts on Jun 1
        //    S2 (3) starts on Jul 1
        //    ST (4) starts on Jun 1
        int cur_term;
        if (cur_mon==1) {
            if (cur_day<6) cur_term = 5;
            else cur_term = 1;
        }
        else if (cur_mon < 6) cur_term = 1;
        else if (cur_mon < 9) cur_term = 4;
        else if (cur_mon <= 12) cur_term = 5;
        else throw new Error("cur_term not computable???");
        //if (termno < cur_term) return false;
        if (termno + cur_term < 4) return false;
        // I want cur_term + termno = 5k + 4, k=0, 1, 2, ...
        return (cur_term+termno-4) % 5 == 0;
    }
    
    
    /**
     * check whether a given term in the range [1, ..., Smax] is a fall term
     * (FA).
     * @param termno int
     * @return boolean true iff termno corresponds to FA
     * @throws IllegalArgumentException if termno &le; 0
     */
    public static boolean isFallTerm(int termno) {
        return isSummerTerm(termno-1);
    }
    
    
    /**
     * return the first fall (FA) term after termno.
     * @param termno int
     * @return int
     * @throws IllegalArgumentException if termno &lt; 0
     */
    public static int nextFallTerm(int termno) {
        if (termno < 0)
            throw new IllegalArgumentException("termno="+termno+
                                               " must be non-negative...");
        int s = termno+1;
        while(true) {
            if (Course.isFallTerm(s)) return s;
            else ++s;
        }
    }
    
    
    /**
     * check if the term indexed by the input argument occurs during summer, ie
     * whether the term is "S1", "S2" or "ST" term.
     * @param termno int
     * @return boolean true iff the term happens during summer months
     */
    public static boolean happensDuringSummer(int termno) {
        if (isSummerTerm(termno)) return true;  // ST
        if (isSummerTerm(termno+1)) return true;  // S2
        if (isSummerTerm(termno+2)) return true;  // S1
        return false;
    }
    
    
    /**
     * reverse functionality of the <CODE>getTermNo(String term)</CODE> method.
     * @param termno int
     * @return String such as "FA2022".
     */
    public static String getTermNameByTermNo(int termno) {
        // 1. get the current date
        int cur_day = CurrentDate._curDay;
        int cur_mon = CurrentDate._curMonth;
        int cur_year = CurrentDate._curYear;
        // 2. compute current term
        //    Fall (5) starts on Sep. 1
        //    Spring (1) starts on Jan. 6
        //    S1 (2) starts on Jun 1
        //    S2 (3) starts on Jul 1
        //    ST (4) starts on Jun 1
        int cur_term;
        if (cur_mon==1) {
            if (cur_day<6) cur_term = 5;
            else cur_term = 1;
        }
        else if (cur_mon < 6) cur_term = 1;
        else if (cur_mon < 9) cur_term = 4;
        else if (cur_mon <= 12) cur_term = 5;
        else throw new Error("cur_term not computable???");
        // figure out how many years ahead is the termno
        // if cur_term + termno%5 > 5, we add one more year (year change).
        // and of course, there is the year difference shown below
        int year_diff = termno / 5;
        if (cur_term + termno%5 > 5) ++year_diff;
        String result = "";
        // now figure out the name of the term
        int tt = cur_term + termno % 5;
        if (tt>5) tt = tt - 5*(tt/5);
        result += getTermNameByTermNumber(tt);
        result += Integer.toString(cur_year+year_diff);
        return result;
    }

    
    /**
     * return 1 for Spring ("SP"), 2 for Summer1 ("S1"), 3, for Summer2 ("S2"),
     * 4 for Summer Term ("ST"), and 5 for Fall ("FA").
     * @param season String
     * @return int
     */
    private static int getTermNumberByTermName(String season) {
        Map<String, Integer> map = Stream.of(new Object[][] { 
            { "SP", 1 }, 
            { "S1", 2 },
            { "S2", 3 },
            { "ST", 4 },
            { "FA", 5 },
        }).collect(
                Collectors.toMap(data -> (String) data[0], 
                                 data -> (Integer) data[1]));
        return map.get(season);
    }
    
    
    /**
     * return "SP" for 1, "S1" for 2, "S2" for 3, "ST" for 4, and "FA" for 5.
     * @param tno int
     * @return String
     */
    private static String getTermNameByTermNumber(int tno) {
        Map<Integer, String> map = Stream.of(new Object[][] {
            {1, "SP"},
            {2, "S1"},
            {3, "S2"},
            {4, "ST"},
            {5, "FA"},
        }).collect(Collectors.toMap(data -> (Integer) data[0],
                                    data -> (String) data[1]));
        return map.get(tno);
    }
    
}


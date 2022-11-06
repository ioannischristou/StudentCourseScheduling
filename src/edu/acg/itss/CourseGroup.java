package edu.acg.itss;

import java.util.*;
import java.io.*;

/**
 * class is responsible for describing groups from which student must take 
 * certain courses. 
 * It also models concentration areas, which however do not impose constraints 
 * on every student but only to those who choose a specific concentration area. 
 * <p>Further, it models honor student courses group, which also imposes no 
 * constraint on itself, but instead makes the courses unavailable to non-honor
 * students. Honor student course group is recognized by the name "HonorGroup".
 * <p>It can also model XOR constraints in the sense that if the "min-required 
 * number of courses" number is a string beginning with the equals sign ("=")
 * the constraint is no long interpreted as "at least this much" but instead as
 * "exactly this much". If the equals sign "=" is preceded by the "&lt;" char,
 * then the constraint is interpreted as taking place only during any particular
 * semester (term). For example, the value "&lt;=1" means that only up to 1 
 * course of all the courses in the group can be taken in the same semester. And
 * if the value is a negative number (eg "-1"), the constraint is interpreted as
 * "at most this much", taking into consideration courses already passed, so 
 * that if the constraint is that from the set {ITC0001, ITC0002, ITC0003} up to
 * 1 class must be taken, and ITC0001 and ITC0003 are already taken, there is no
 * constraint produced to be written in the schedule. For this to occur, there 
 * must be no preceding "&lt;" or "=" sign in the value. Finally, the value for 
 * the credits can also be a negative number; in this case, the absolute value 
 * is interpreted to mean the minimum number of different "disciplines" that 
 * must be represented in the selection of courses from the group described in 
 * the next line (a "discipline" being the two or three letter code that is in
 * front of the course code of any course.)
 * <p>Course groups can also model soft-order precedence constraints: a soft-
 * order precedence constraint is a constraint between 2 courses ci and cj, and 
 * asks that if both courses are to be taken, then ci must be taken before 
 * course cj. A soft order precedence constraint has name starting with 
 * "softorder". In such a course-group, the order in which the two courses are 
 * specified matters: it specifies the soft-order between them (first comes the
 * "soft pre-requisite" course). For soft-order constraints, the number for the
 * minimum number of courses represents the maximum distance in time (terms) 
 * that the courses must be taken (unless the number is zero, in which case it
 * is ignored).
 * <p>Finally, it models capstone project course constraints, which ask for a 
 * minimum number of credits before taking the capstone, and optionally, for a 
 * number of courses from their concentration area as well. Capstone project 
 * groups have names starting with "capstone".
 * <p>The <CODE>CourseGroupEditor</CODE> class is responsible for providing the 
 * GUI for editing course groups (each group is stored in their own "*.grp" 
 * file).
 * @author itc
 */
public class CourseGroup {
    
    /**
     * maintains a mapping from group names to CourseGroup objects.
     */
    private final static Map<String, CourseGroup> _allCourseGroupsMap = 
            new TreeMap<>();
    /**
     * if the name starts with "capstone", the group represents a capstone 
     * project group, and it must have a unique course in the group.
     */
    private final String _groupName;
    /**
     * denotes if the group is a concentration area or not.
     */
    private final boolean _isConcentrationArea;
    /**
     * all the courses in this group.
     */
    private final List<String> _allGroupCodes;
    /**
     * the minimum number of courses to take from this group (0 if not 
     * required). If this CourseGroup object is a Capstone project group, the 
     * number represents the minimum number of courses required from the 
     * student's chosen concentration area, before taking the single course in 
     * this group. If this object is a soft-order constraint, the number 
     * represents the maximum term separation distance between the two courses,
     * assuming they are both to be scheduled (unless the number is zero, in 
     * which case the value is ignored)
     */
    private final int _minNumCoursesReq;
    /**
     * the minimum number of credits to complete from this group (0 if not 
     * required). If this CourseGroup object is a Capstone project group, the
     * number represents the minimum number of total credits the student must
     * have completed before taking the single course in this group.
     */
    private final int _minNumCreditsReq;
    /**
     * when true, the _minNumCoursesReq number is interpreted as exact, but only
     * for the courses to be taken, not for courses already passed.
     */
    private final boolean _isExact;
    /**
     * when true, the _minNumCoursesReq number is interpreted as a maximum 
     * number of courses to be taken together during the SAME semester.
     */
    private final boolean _holdsPerSemester;
    /**
     * when different than 1, this number indicates the minimum number of 
     * different disciplines that must be present in the final selection of 
     * courses from this group of courses.
     */
    private final int _minNumDisciplines;
   
    
    /**
     * create a new CourseGroup (used by the <CODE>CourseGroupEditor</CODE>).
     * @param name String
     * @return CourseGroup
     */
    public static CourseGroup createCourseGroup(String name) {
        CourseGroup cg = new CourseGroup(name, false, 
                                         new ArrayList<>(), 
                                         0, false, false, 0, 1);
        _allCourseGroupsMap.put(name, cg);
        return cg;
    }
    
    
    /**
     * remove the CourseGroup object with given name from the map holding all
     * CourseGroup objects. Does not delete underlying file with extension .grp
     * @param name String the name of the group to delete from memory.
     */
    public static void removeCourseGroup(String name) {
        _allCourseGroupsMap.remove(name);
    }
    
    
    /**
     * read a course group from a text file and return the relevant group.
     * Method must be called after all courses are read in by invoking
     * <CODE>Course.readAllCourses(filename)</CODE>.
     * The file must contain just 2 lines with the following format (semi-column
     * separated):
     * <ul>
     * <li>&lt;groupname&gt;;&lt;is_concentration&gt;;
     * [[&lt;]=]&lt;minnumcoursesreqd&gt;;&lt;minnumcreditsreqd&gt;
     * <li>&lt;coursecode&gt;[;coursecode]*
     * </ul>
     * The 3rd field of the 1st line (&lt;minnumcoursesreqd&gt;) must be a 
     * number of course, but it may be preceded by either the symbol "=" in 
     * which case the constraint is interpreted to be an equality constraint,
     * or the number may be preceded by the string "&lt;=" in which case the
     * constraint reverses direction and the number is interpreted to be a MAX
     * value for the sum of the variables corresponding to the courses described
     * in the 2nd line of the file, holding on a PER-SEMESTER basis (essentially
     * indicating that no more than a maximum number from the courses described 
     * in the second line can be taken together in the same semester). If the
     * number is negative, the constraint is interpreted as an "at most this 
     * much" constraint, taking into account passed courses. For more, read the
     * overall class documentation.
     * The 4th field of the 1st line (&lt;minnumcreditsreqd&gt;) must also be a
     * number of course, but it may be a negative number (ie preceded by the "-"
     * sign) in which case, there is no minimum number of credits constraint but
     * instead, there is a minimum number of different disciplines constraint 
     * set forth. For more, read the overall class documentation.
     * The file may also contain any lines AFTER the first two lines that start 
     * with "#" that designate comments (they are never read).
     * @param filename String
     * @return CourseGroup
     */
    public static CourseGroup readCourseGroup(String filename) {
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            CourseGroup cg = null;
            String line1 = br.readLine();
            String[] data = line1.split(";");
            String name = data[0];
            boolean is_conc = Boolean.parseBoolean(data[1]);
            boolean isExact = false;
            boolean holdsPerSemester = false;
            int minnumcourses=0;
            try {
                minnumcourses = Integer.parseInt(data[2]);
            }
            catch (NumberFormatException e) {
                if (data[2].startsWith("=")) {
                    isExact = true;
                    minnumcourses = Integer.parseInt(data[2].substring(1));
                }
                else if (data[2].startsWith("<=")) {
                    holdsPerSemester = true;
                    minnumcourses = Integer.parseInt(data[2].substring(2));
                    // minnumcourses is now really MAXnumcourser(per semester)
                }
                else throw new IllegalArgumentException("couldn't read "+
                                                        "minnumcourses");
            }
            int minnumcredits = Integer.parseInt(data[3]);
            int minnumdisciplines = 1;
            if (minnumcredits<0) {
                minnumdisciplines = -minnumcredits;
                minnumcredits = 0;
            }
            data = br.readLine().split(";");
            List<String> codes = new ArrayList<>();
            codes.addAll(Arrays.asList(data));
            cg = new CourseGroup(name, is_conc, codes, 
                                 minnumcourses, isExact, holdsPerSemester,
                                 minnumcredits, minnumdisciplines);
            _allCourseGroupsMap.put(name, cg);
            return cg;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;  // never gets here
        }
    }
    
    
    /**
     * return an iterator to traverse all group names read so far.
     * @return Iterator&lt;String&gt; iterator over all course group names.
     */
    public static Iterator<String> getCourseGroupNameIterator() {
        return _allCourseGroupsMap.keySet().iterator();
    }
    
    
    /**
     * before invoking this method, the method 
     * <CODE>readCourseGroup(filename)</CODE> by the appropriate name must have
     * been called.
     * @param groupname String
     * @return CourseGroup will return null if no course group by the input
     * groupname is read.
     */
    public static CourseGroup getCourseGroupByName(String groupname) {
        return _allCourseGroupsMap.get(groupname);
    }
    
    
    /**
     * before invoking this method, the method 
     * <CODE>readCourseGroup(filename)</CODE> by the appropriate name must have
     * been called. The method will search over all course groups for those 
     * groups for which the method <CODE>isConcentrationArea()</CODE> returns 
     * true, and will search among them for groups with names that end with 
     * " Core", and will return what precedes this suffix.
     * @return Set&lt;String&gt;
     */
    public static Set<String> getAllConcentrationAreas() {
        Set<String> conc_area_names = new HashSet<>();
        Iterator<String> names_it = CourseGroup.getCourseGroupNameIterator();
        while (names_it.hasNext()) {
            String name = names_it.next();
            CourseGroup cg = CourseGroup.getCourseGroupByName(name);
            if (cg.isConcentrationArea() && name.endsWith(" Core")) {
                String con_name = name.substring(0, name.length()-5);
                conc_area_names.add(con_name);
            }
        }
        return conc_area_names;
    }
    
    
    /**
     * reset all course-groups to allow re-loading.
     */
    public static void reset() {
        CourseGroup._allCourseGroupsMap.clear();
    }
    
    
    /**
     * private single constructor is only called from the method 
     * <CODE>readCourseGroup(filename)</CODE> which is the only method that is
     * allowed to create <CODE>CourseGroup</CODE> objects, and adds them to the
     * appropriate static map for later retrieval by group name.
     * @param name String
     * @param isConcentrationArea boolean
     * @param groupCodes List&lt;String&gt;
     * @param minNumCoursesReq int may be zero
     * @param isExact boolean refers to the previous variable
     * @param holdsPerSemester boolean refers to the previous variable also
     * @param minCreditsReq int may be zero
     * @param minNumDisciplines int must always be &ge; 1
     */
    private CourseGroup(String name, boolean isConcentrationArea,
                        List<String> groupCodes, 
                        int minNumCoursesReq, 
                        boolean isExact, boolean holdsPerSemester, 
                        int minCreditsReq,
                        int minNumDisciplines) {
        _groupName = name;
        _isConcentrationArea = isConcentrationArea;
        _allGroupCodes = new ArrayList<>(groupCodes);
        _minNumCoursesReq = minNumCoursesReq;
        _minNumCreditsReq = minCreditsReq;
        _minNumDisciplines = minNumDisciplines;
        _isExact = isExact;
        _holdsPerSemester = holdsPerSemester;
    }
    
    
    /**
     * return the group name.
     * @return String
     */
    public final String getGroupName() { return _groupName; }
    
    
    /**
     * checks if given group is a concentration area.
     * @return boolean true iff this object represents a concentration area
     */
    public final boolean isConcentrationArea() { return _isConcentrationArea; }
    
    
    /**
     * checks if this given group represents a capstone project.
     * @return boolean true iff this object's group name starts with the prefix
     * "capstone".
     */
    public final boolean isCapstoneProjectGroup() {
        return _groupName.startsWith("capstone");
    }
    
    
    /**
     * checks if this given group represents the OU-constraint that asks for 
     * an upper limit on the number of OU courses taken every academic year (ie
     * from any Fall to the next SummerTerm). 
     * @return boolean true iff this object's group name starts with the prefix
     * "OU".
     */
    public final boolean isOUConstraint() {
        return _groupName.startsWith("OU");
    }
    
    
    /**
     * checks if the given group represents a soft-order precedence constraint.
     * @return boolean true iff this object's group name starts with the prefix
     * "softorder".
     */
    public final boolean isSoftOrderPrecedenceConstraint() {
        return _groupName.startsWith("softorder");
    }
    
    
    /**
     * checks if this given group represents the courses only available to honor
     * students.
     * @return  boolean true iff this object's group name is "HonorGroup".
     */
    public final boolean isHonorStudentCourseGroup() {
        return "HonorGroup".equals(_groupName);
    }
    
    
    /**
     * returns all course codes belonging to this group (synonyms are not 
     * included).
     * @return Set&lt;String&gt;
     */
    public List<String> getGroupCodes() {
        List<String> result = new ArrayList<>(_allGroupCodes);
        return result;
    }
    
    
    /**
     * returns all course codes belonging to this group, including synonyms for
     * each class.
     * @return Set&lt;String&gt;
     */
    public Set<String> getAllGroupCodes() {
        Set<String> result = new HashSet<>(_allGroupCodes);
        for (String c : _allGroupCodes) {
            Course crs = Course.getCourseByCode(c);
            Set<String> crs_syns = crs.getSynonymCodes();
            result.addAll(crs_syns);
        }
        return result;
    }
    
    
    /**
     * return the minimum number of courses required from this group.
     * @return int
     */
    public int getMinNumCoursesReqd() { return _minNumCoursesReq; }
    
    
    /**
     * check if the constraint is essentially an XOR type constraint for the 
     * courses to take (not courses already passed).
     * @return boolean
     */
    public boolean isCoursesReqdExact() { return _isExact; }
    
    
    /**
     * check if the constraint is essentially a constraint that holds per 
     * semester, and is a MAX number of courses constraint.
     * @return boolean
     */
    public boolean isHoldsPerSemester() {
        return _holdsPerSemester;
    }
    
    
    /**
     * return the minimum number of credits required from this group.
     * @return int always non-negative
     */
    public int getMinNumCreditsReqd() { return _minNumCreditsReq; }
    
    
    /**
     * return the minimum number of different disciplines that the selection of
     * courses from this group must represent.
     * @return int always positive number
     */
    public int getMinNumDisciplines() { return _minNumDisciplines; }
    
}

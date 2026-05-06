package edu.acg.itss;

import java.util.*;
import java.io.*;


/**
 * maintains the courses the student has declared they want to take. It also 
 * maintains courses the student doesn't want to take, and also possibly when
 * the student wants to take a course.
 * @author itc
 */
public class DesiredCourses {
    /**
     * every entry in this map is of the form:
     * {"ITC3160": {"FA2022", "SP2023"}} etc.
     */
    private HashMap<String, Set<String>> _desiredCourseCodes = new HashMap<>();
    
    
    /**
     * single class constructor is no-op.
     */
    public DesiredCourses() {
        // no-op
    }
    
    
    /**
     * reads desired courses from file, if it exists. The file consists of lines
     * that follow the format:
     * &lt;coursecode&gt;;[term ]*
     * The string "allterms" is allowed as "term" value.
     * @param filename String
     */
    public void readDesiredCoursesFromFile(String filename) {
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while(true) {
                String line = br.readLine();
                if (line==null) break;  // EOF
                String[] coursecodes = line.split(";");
                String ccode = coursecodes[0];
                Set<String> allowed_times = new HashSet<>();
                for (int i=1; i<coursecodes.length; i++) {
                    allowed_times.add(coursecodes[i]);
                }
                _desiredCourseCodes.put(ccode, allowed_times);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * add the argument to the desired courses.
     * @param code String
     */
    public void addCourse(String code) {
        Set<String> all_times = new HashSet<>();
        all_times.add("allterms");
        _desiredCourseCodes.put(code, all_times);
    }
    
    
    /**
     * add desired course for given terms. If terms is empty, course is NOT
     * desired!.
     * @param code String
     * @param allowed_terms Set&lt;String&gt; may be empty 
     */
    public void addCourse(String code, Set<String> allowed_terms) {
        Set<String> at = new HashSet<>(allowed_terms);
        _desiredCourseCodes.put(code, at);
    }


    /**
     * clear the set of courses.
     */
    public void clear() {
        _desiredCourseCodes.clear();
    }
    
    
    /**
     * add all course codes in the set.
     * @param codes Collection&lt;String&gt; each string may be just a code 
     * such as "ITC3160" which means we want the course and don't care when we 
     * register for it, or it may be "ITC3160;" which means we do NOT want the
     * course, or it may be "ITC3160;FA2022 SP2023" which means we want to take
     * the course either in FA2023 or in SP2023.
     */
    public void addAll(Collection<String> codes) {
        for (String code : codes) {
            String[] data = code.split(";");
            if (data.length==1) {
                if (code.endsWith(";")) {
                    Set<String> ts = new HashSet<>();
                    addCourse(data[0], ts);
                    continue;
                }
                else {
                    addCourse(data[0]);
                    continue;
                }
            }
            String ccode = data[0];
            String[] ts = data[1].split(" ");
            Set<String> tset = new HashSet<>();
            for (String t : ts) {
                tset.add(t);
            }
            addCourse(ccode, tset);
        }
    }
    
    
    /**
     * check if the course described by code is desired or not.
     * @param code String
     * @return boolean true iff code is in the <CODE>_desiredCourseCodes</CODE> 
     * set.
     */
    public boolean contains(String code) {
        Set<String> at = _desiredCourseCodes.get(code);
        return (at!=null && at.size()>0);
    }
    
    
    /**
     * get the preferred terms for the given course in terms of the term numbers
     * from the current date (for example {1, 2, 3}).
     * @param code String
     * @param cur_term int must be in [0, 1, ...Smax] with 0 indicating there is
     * no cur_term to which the course with the given code is assigned
     * @param Smax int
     * @return Set&lt;Integer&gt;
     */
    public Set<Integer> getPreferredTerms4Course(String code, 
                                                 int cur_term, 
                                                 int Smax) {
        Set<String> preferred_terms = _desiredCourseCodes.get(code);
        Set<Integer> res = new HashSet<>();
        for (String t : preferred_terms) {
            if ("allterms".equals(t)) {
                for (int i=1; i<=Smax; i++) res.add(i);
                return res;
            }
            else if ("allotherterms".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (s!=cur_term) res.add(s);
                }
                continue;
            }
            else if ("everyfall".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (Course.isFallTerm(s)) res.add(s);
                }
                continue;                
            }
            else if ("everyspring".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (Course.isSpringTerm(s)) res.add(s);
                }
                continue;                
            }
            else if ("everysummer1".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (Course.isSummer1Term(s)) res.add(s);
                }
                continue;                
            }
            else if ("everysummer2".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (Course.isSummer2Term(s)) res.add(s);
                }
                continue;                
            }
            else if ("everysummerterm".equals(t)) {
                for (int s=1; s<=Smax; s++) {
                    if (Course.isSummerTerm(s)) res.add(s);
                }
                continue;                
            }
            int tno = Course.getTermNo(t);
            res.add(tno);
        }
        return res;
    }
    
    
    /**
     * return an iterator to all desired course codes.
     * @return Iterator&lt;String&gt;
     */
    public Iterator<String> getDesiredCourseCodesIterator() {
        return _desiredCourseCodes.keySet().iterator();
    }        
    
    
    /**
     * return the number of desired courses that have specified only the given
     * term as acceptable for when to take the course.
     * @param term String e.g. "FA2022"
     * @return int  // must be non-negative
     */
    public int getNumDesiredCoursesForTerm(String term) {
        int res = 0;
        for (String code : this._desiredCourseCodes.keySet()) {
            Set<String> terms = this._desiredCourseCodes.get(code);
            if (terms.size()!=1) continue;
            Iterator<String> itt = terms.iterator();
            if (term.equals(itt.next())) ++res;
        }
        return res;
    }
    
    
    /**
     * create and return a <CODE>CodeNameAllowedTerms</CODE> object, useful for
     * displaying in the GUI of desired courses.
     * @param code String
     * @param Smax int
     * @return CodeNameAllowedTerms  // may be null
     */
    public CodeNameAllowedTerms getCodeNameAllowedTerms(String code, int Smax) {
        if (!contains(code)) return null;
        Course c = Course.getCourseByCode(code);
        String title = c.getName();
        String at = "";
        Set<String> terms = _desiredCourseCodes.get(code);
        for (String t : terms) at += t+ " ";
        CodeNameAllowedTerms cnat = new CodeNameAllowedTerms(code, title, at);
        if (!CodeNameAllowedTerms.prefferedTermsAllowed(code, at, 0, Smax)) {
            cnat = new CodeNameAllowedTerms(code, title, "-");
        }
        return cnat;
    }
}

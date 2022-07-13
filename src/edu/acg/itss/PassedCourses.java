package edu.acg.itss;

import java.util.*;
import java.io.*;


/**
 * maintains the courses the student has already passed.
 * @author itc
 */
public class PassedCourses {
    private Set<String> _passedCourseCodes = new HashSet<>();
    
    
    /**
     * single class constructor is no-op.
     */
    public PassedCourses() {
        // no-op
    }
    
    
    /**
     * reads passed courses from file, if it exists. The file consists of lines
     * that contain course codes (if more than one in a line, they must be 
     * separated by semi-column.)
     * @param filename String
     */
    public void readPassedCoursesFromFile(String filename) {
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while(true) {
                String line = br.readLine();
                if (line==null) break;  // EOF
                String[] coursecodes = line.split(";");
                _passedCourseCodes.addAll(Arrays.asList(coursecodes));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * add the argument to the passed courses.
     * @param code String
     */
    public void addCourse(String code) {
        _passedCourseCodes.add(code);
    }
    
    
    /**
     * clear the set of courses.
     */
    public void clear() {
        _passedCourseCodes.clear();
    }
    
    
    /**
     * add all course codes in the set.
     * @param codes Collection&lt;String&gt;
     */
    public void addAll(Collection<String> codes) {
        _passedCourseCodes.addAll(codes);
    }
    
    
    /**
     * check if the course described by code is passed or not.
     * @param code String
     * @return boolean true iff code is in the <CODE>_passedCourseCodes</CODE> 
     * set.
     */
    public boolean contains(String code) {
       return _passedCourseCodes.contains(code);
    }
    
    
    /**
     * return an iterator to all passed course codes.
     * @return Iterator&lt;String&gt;
     */
    public Iterator<String> getPassedCourseCodesIterator() {
        return _passedCourseCodes.iterator();
    }        
    
    
    /**
     * return the total number of courses passed so far.
     * @return int
     */
    public int size() {
        return _passedCourseCodes.size();
    }
}

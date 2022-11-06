package edu.acg.itss;

import java.util.*;
import java.io.*;

/**
 * maintains scheduling problem parameters.
 * @author itc
 */
public class ScheduleParams {
    private Properties _props;
    
    /**
     * single constructor reads properties from given input file.
     * @param filename String
     */
    public ScheduleParams(String filename) {
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            _props = new Properties();
            _props.load(br);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    
    /**
     * return the minimum required total number of credits required for 
     * graduation. This corresponds to the value of the property "Tc" in the 
     * file.
     * @return int 
     */
    public int getMinReqdTotalCredits() {
        return Integer.parseInt(_props.getProperty("Tc"));
    }
    
    
    /**
     * return the maximum number of credits a student may take in a semester.
     * This corresponds to the value of the property "CmaxHonor" in the file if
     * the student is an honors' student, and to the value of the property 
     * "Cmax" otherwise.
     * @param isHonorStudent boolean this value is read from the GUI
     * @return int
     */
    public int getCmax(boolean isHonorStudent) {
        String c = isHonorStudent ? 
                    _props.getProperty("CmaxHonor") : 
                    _props.getProperty("Cmax");
        return Integer.parseInt(c);
    }
    
    
    /**
     * same as <CODE>getCmax(boolean)</CODE> but for the entire summer season
     * which includes Summer-1 ("S1"), Summer-2("S2") and Summer-Term ("ST").
     * The existence of these numbers makes the total number of credits students
     * are allowed to take during the summer months much less than what it is 
     * now.
     * @param isHonorStudent boolean
     * @return int
     */
    public int getSummerCmax(boolean isHonorStudent) {
        String c = isHonorStudent ? 
                    _props.getProperty("SummerCmaxHonor") : 
                    _props.getProperty("SummerCmax");
        return Integer.parseInt(c);        
    }
    
    
    /**
     * return the maximum number of semesters the student is allowed to register
     * for. This corresponds to the value of the property "Smax" in the file.
     * @return int
     */
    public int getSmax() {
        return Integer.parseInt(_props.getProperty("Smax"));
    }
    
    
    /**
     * return the maximum term number by which any LE-designated course must be
     * passed.
     * @return int 
     */
    public int getMaxLETerm() {
        return Integer.parseInt(_props.getProperty("MaxLETerm"));
    }
    
    
    /**
     * return the maximum number of courses a student may be attending at the 
     * same time during any of the summer seasons (S1, S2, ST).
     * @return int
     */
    public int getSummerConcNMax() {
        return Integer.parseInt(_props.getProperty("SummerConcNMax"));
    }
    
    
    /**
     * return the thesis code of the program.
     * @return String such as "ITC4979" or "ITC4949" (for the CYN program)
     */
    public String getThesisCode() {
        return _props.getProperty("ThesisCourseCode");
    }
    
    
    /**
     * return the maximum number of courses a freshman is allowed to take for
     * any given term. A freshman is someone with less than 
     * <CODE>getMinNumCourses4Sophomore()</CODE> courses taken.
     * @return int
     */
    public int getMaxNumCoursesPerTerm4Freshmen() {
        return Integer.parseInt(_props.
                                  getProperty("FreshmanMaxNumCoursesPerTerm"));
    }
    
    
    /**
     * return the minimum number of courses that a student must have taken to 
     * be considered a sophomore or more senior.
     * @return int
     */
    public int getMinNumCourses4Sophomore() {
        return Integer.parseInt(_props.getProperty("MinNumCourses4Sophomore"));
    }
    
    
    /**
     * return the set of program codes (eg "ITC", "MA" etc) whose number of 
     * courses we seek to maximize as last resort criterion. The property value 
     * must separate the program codes by semi-columns. Some program cores
     * may even designate exception groups eg "MA\LE-core-stat".
     * Optional.
     * @return Set&lt;String&gt;
     */
    public Set<ProgramCodeStruct> getPrograms2Maximize() {
        Set<ProgramCodeStruct> ret = new HashSet<>();
        String program_codes_str = _props.getProperty("ProgramCodes2Maximize");
        if (program_codes_str==null || program_codes_str.length()==0)
            return ret;
        String[] pcs = program_codes_str.split(";");
        for (String pc : pcs) {
            String[] pc2 = pc.split("\\\\");
            String code = pc2[0];
            String exc = null;
            if (pc2.length>1) exc = pc2[1];
            ProgramCodeStruct pcodestruct = new ProgramCodeStruct(code, exc);
            ret.add(pcodestruct);
        }
        return ret;
    }
    
    
    /**
     * return the program code ("ITC" for the B.Sc. in IT). This is the prefix
     * of the course codes that the list of desired courses is going to show,
     * that will be allowed to edit the time-slots in a schedule etc.
     * @return String
     */
    public String getProgramCode() {
        return _props.getProperty("ProgramCode");
    }
    
    
    /**
     * return the header for the "cls.csv" courses CSV file.
     * @return String
     */
    public String getCourseCSVFileHeader() {
        return _props.getProperty("CourseCSVFileHeader");
    }
    
    
    /**
     * return the value of the minimum estimated grade threshold required for 
     * a course to get a non-zero value regarding expected GPA maximization. If
     * the value is not found in the properties file, the default returned is 
     * 3.0.
     * @return float
     */
    public float getMinGradeThres() {
        return Float.parseFloat(_props.getProperty("MinGradeThres", "3.0"));
    }
    
    
    /**
     * return the value of the property "AllowEdit", or false if not found in
     * the properties file.
     * @return boolean
     */
    public boolean getAllowEdit() {
        return Boolean.parseBoolean(_props.getProperty("AllowEdit", "false"));
    }
    
    
    /**
     * returns the value of a parameter given its name.
     * @param paramName String
     * @return String may be null
     */
    public String getParameterValue(String paramName) {
        return _props.getProperty(paramName);
    }    
}

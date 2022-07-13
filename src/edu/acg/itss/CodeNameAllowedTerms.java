package edu.acg.itss;

import java.util.List;


/**
 * auxiliary class used only for the display of desired courses.
 * @author itc
 */
public class CodeNameAllowedTerms {
    public String _code;
    public String _title;
    public String _allowedTerms;
    
    /**
     * single constructor.
     * @param code String
     * @param title String
     * @param allowedTerms String such as "allterms" or "FA2022 SP2023" 
     */
    public CodeNameAllowedTerms(String code, String title, String allowedTerms){
        _code = code;
        _title = title;
        _allowedTerms = allowedTerms;
    }
    
    
    /**
     * checks if the course described by given code is offered in at least one
     * of the terms described in allowedTerms (preferred terms) string. This is
     * a helper method so that we can offer the following functionality: if the
     * student edits their proposed schedule by asking for a course to be taken
     * during a time that the course is not offered, then in the "desired 
     * courses" list in the GUI, the course will be selected and the preferred
     * time for when to take it will be shown as "(NOT TO TAKE)" which is a 
     * strong indication for the student that the times they chose are not 
     * feasible.
     * @param code String such as "ITC3160"
     * @param allowedTerms String such as "allterms", "allotherterms" or 
     * "FA2022 SP2023" or "-" (unwanted, overrides all other options)
     * @param currentTermNo int the termno when the course with given code is
     * scheduled in the current solution
     * @param Smax int the maximum allowed term remaining to complete studies
     * @return boolean true iff the allowedTerms contains at least one term 
     * when the course is offered
     * @throws IllegalArgumentException if code does not exist or if 
     * allowedTerms cannot be parsed.
     */
    public static boolean prefferedTermsAllowed(String code, 
                                                String allowedTerms,
                                                int currentTermNo,
                                                int Smax) {
        Course c = Course.getCourseByCode(code);
        if (c==null) throw new IllegalArgumentException("invalid course code");
        if (allowedTerms==null) 
            throw new IllegalArgumentException("null allowedTerms");
        String[] terms = allowedTerms.split(" ");
        List<Integer> off_terms = c.getTermsOffered(Smax);
        boolean ret = false;
        for (String term : terms) {
            if ("-".equals(term.trim())) return false;
            if ("allterms".equals(term.trim())) {
                ret = true;
                continue;
            }
            if ("allotherterms".equals(term.trim())) {
                for (int s=1; s<=Smax; s++) {
                    if (s!=currentTermNo && off_terms.contains(s)) ret = true;
                }
                continue;
            }
            if (term.length()>1) {
                int termno = Course.getTermNo(term);
                if (off_terms.contains(termno)) ret = true;
            }
        }
        return ret;
    }
    
    
    /**
     * string representation of CodeNameAllowedTerms objects.
     * @return String
     */
    @Override
    public String toString() {
        String ret = _code+" "+_title;
        if (_allowedTerms!=null && _allowedTerms.length()>1) { 
            if (!"allterms".equals(_allowedTerms))
                ret += " @ "+_allowedTerms;
        }
        else ret += " (NOT TO TAKE)";
        return ret;
    }
}

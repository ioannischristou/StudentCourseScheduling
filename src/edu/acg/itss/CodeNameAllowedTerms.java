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
     * of the terms described in the preferredTerms string parameter. This is
     * a helper method so that we can offer the following functionality: if the
     * student edits their proposed schedule by asking for a course to be taken
     * during a time that the course is not offered, then in the "desired 
     * courses" list in the GUI, the course will be selected and the preferred
     * time for when to take it will be shown as "(NOT TO TAKE)" which is a 
     * strong indication for the student that the times they chose are not 
     * feasible.
     * @param code String such as "ITC3160"
     * @param preferredTerms String such as "allterms", "everyfall",
     * "allotherterms" or "FA2022 SP2023" or "-" (unwanted, overrides all other 
     * options)
     * @param currentTermNo int the termno when the course with given code is
     * scheduled in the current solution
     * @param Smax int the maximum allowed term remaining to complete studies
     * @return boolean true iff the allowedTerms contains at least one term 
     * when the course is offered
     * @throws IllegalArgumentException if code does not exist or if 
     * preferredTerms cannot be parsed.
     */
    public static boolean prefferedTermsAllowed(String code, 
                                                String preferredTerms,
                                                int currentTermNo,
                                                int Smax) {
        System.err.println("preferredTermsAllowed("+code+"): called w/ Smax="+Smax);  // debug
        Course c = Course.getCourseByCode(code);
        if (c==null) throw new IllegalArgumentException("invalid course code");
        if (preferredTerms==null) 
            throw new IllegalArgumentException("null preferredTerms");
        String[] terms = preferredTerms.split(" ");
        List<Integer> off_terms = c.getTermsOffered(Smax);
        // debug from here
        System.err.print("CNAT.preferredTermsAllowed("+code+","+preferredTerms+
                         "...): offered in terms: ");
        for (int t : off_terms) {
            System.err.print(t+" ");
        }
        // debug up to here
        boolean ret = false;
        for (String term : terms) {
            term = term.trim();
            if ("-".equals(term)) {
                System.err.println("due to - return false");  // debug
                return false;
            }
            if ("allterms".equals(term)) {
                System.err.println("due to allterms return true");  // debug
                return true;
            }
            if ("allotherterms".equals(term)) {
                for (int s=1; s<=Smax; s++) {
                    if (s!=currentTermNo && off_terms.contains(s)) {
                        System.err.println("due to allotherterms return true");  // debug
                        return true;
                    }
                }
                continue;
            }
            if ("everyfall".equals(term)) {
                int s_start = Course.nextFallTerm(0);
                for (int s=s_start; s<=Smax; s++) {
                    if (off_terms.contains(s)) {
                        System.err.println("due to everyfall return true");  // debug
                        return true;                        
                    }
                }
            }
            if ("everyspring".equals(term)) {
                int s_start = Course.nextSpringTerm(0);
                for (int s=s_start; s<=Smax; s++) {
                    if (off_terms.contains(s)) {
                        System.err.println("due to everyspring return true");  // debug
                        return true;                        
                    }
                }
            }
            if ("everysummer1".equals(term)) {
                int s_start = Course.nextSummer1Term(0);
                for (int s=s_start; s<=Smax; s++) {
                    if (off_terms.contains(s)) {
                        System.err.println("due to everysummer1 return true");  // debug
                        return true;                        
                    }
                }
            }
            if ("everysummer2".equals(term)) {
                int s_start = Course.nextSummer2Term(0);
                for (int s=s_start; s<=Smax; s++) {
                    if (off_terms.contains(s)) {
                        System.err.println("due to everysummer2 return true");  // debug
                        return true;                        
                    }
                }
            }
            if ("everysummerterm".equals(term)) {
                int s_start = Course.nextSummerTerm(0);
                for (int s=s_start; s<=Smax; s++) {
                    if (off_terms.contains(s)) {
                        System.err.println("due to everysummerterm return true");  // debug
                        return true;                        
                    }
                }
            }
            if (term.length()>1) {
                int termno = Course.getTermNo(term);
                System.err.print("termno = "+termno+" ");
                if (off_terms.contains(termno)) {
                    System.err.println(" due to termno, return true");  // debug
                    return true;
                }
            }
        }
        System.err.println("nothing worked returns false");  // debug
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

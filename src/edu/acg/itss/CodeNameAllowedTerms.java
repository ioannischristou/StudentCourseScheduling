package edu.acg.itss;

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
    
    
    @Override
    /**
     * string representation of CodeNameAllowedTerms objects.
     */
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

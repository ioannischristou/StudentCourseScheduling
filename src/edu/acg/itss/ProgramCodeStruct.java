package edu.acg.itss;

/**
 * represents program codes to always attempt to maximize as last priority 
 * objective, and their possible exception groups. Such program code structs may
 * appear in the "params.props" file that the ScheduleParams class reads. 
 * @author itc
 */
public class ProgramCodeStruct {
    private String _programCode;
    private String _courseGroupException;
    
    /**
     * public constructor sets the exception group to null.
     * @param code String
     */
    public ProgramCodeStruct(String code) {
        assert(code!=null);
        _programCode = code;
    }
    
    
    /**
     * public constructor sets both the program code and the exception group.
     * @param code String eg "MA"
     * @param exception String eg "LE-core-stat"
     */
    public ProgramCodeStruct(String code, String exception) {
        this(code);
        _courseGroupException = exception;
    }
    
    
    /**
     * return the program code.
     * @return String
     */
    public String getProgramCode() {
        return _programCode;
    }
    
    
    /**
     * return the exception group if any.
     * @return String may be null
     */
    public String getException() {
        return _courseGroupException;
    }
}

package edu.acg.itss.tests;

import edu.acg.itss.*;
import java.time.LocalDate;


/**
 * tests the <CODE>Course</CODE> class.
 * @author itc
 */
public class CourseTests {
    public static void main(String[] args) {
        // 1. get the current date
        int cur_day=0, cur_mon=0, cur_year=0;
        if (args.length<=1) {
            LocalDate now = LocalDate.now();
            cur_day = now.getDayOfMonth();
            cur_mon = now.getMonthValue();
            cur_year = now.getYear();
        }
        else if (args.length > 1) {
            String[] dt = args[1].split("/");
            cur_day = Integer.parseInt(dt[0]);
            cur_mon = Integer.parseInt(dt[1]);
            cur_year = Integer.parseInt(dt[2]);
        }
        // set current date
        CurrentDate._curDay = cur_day;
        CurrentDate._curMonth = cur_mon;
        CurrentDate._curYear = cur_year;
        System.out.println("NOW is "+cur_day+"/"+cur_mon+"/"+cur_year);
        // 2. test Course.getTermNo(), Course.getTermNameByTermNo()
        if (args.length>0) {
            int termno = Course.getTermNo(args[0]);
            System.out.println(args[0]+" corresponds to term #"+termno);
            String term = Course.getTermNameByTermNo(termno);
            System.out.println(termno+" corresponds to full term name: "+term);
        }
        // 3. test Course.isSummer2Term(2)and s=1 being S2 term
        boolean issummer2 = Course.isSummer2Term(2);
        System.out.println("Course.isSummer2Term(2) returns "+issummer2);
        CurrentDate._curDay=30;
        CurrentDate._curMonth=6;
        System.out.println("s=1 corresponds to "+Course.getTermNameByTermNo(1));
        // 4. read courses, and print out 1st course
        Course.readAllCoursesFromFile("IT/cls.csv", 25);
        Course c = Course.getCourseByCode("WP1212");
        System.err.println(c.getFullDetailsString(25));
        System.err.println("Terms in c: ");
        for (Integer t : c.getTermsOffered(25)) {
            System.err.print(t+" ");
        }
        System.err.println("");
        
    }
}

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
        int cur_day, cur_mon, cur_year;
        if (args.length==1) {
            LocalDate now = LocalDate.now();
            cur_day = now.getDayOfMonth();
            cur_mon = now.getMonthValue();
            cur_year = now.getYear();
        }
        else {
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
        int termno = Course.getTermNo(args[0]);
        System.out.println(args[0]+" corresponds to term #"+termno);
        String term = Course.getTermNameByTermNo(termno);
        System.out.println(termno+" corresponds to full term name: "+term);
        
        // 3. read courses, and print out 1st course
        Course.readAllCoursesFromFile("ITC/cls.csv", 25);
        Course c = Course.getCourseByCode("WP1212");
        System.err.println(c.getFullDetailsString(25));
        System.err.println("Terms in c: ");
        for (Integer t : c.getTermsOffered(25)) {
            System.err.print(t+" ");
        }
        System.err.println("");
        
    }
}

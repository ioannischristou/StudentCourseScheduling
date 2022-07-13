# StudentCourseScheduling
The ACG Student Course Planner (SCORER).
The project allows the American College of Greece automatically create course plans for the students of its programs. 
The current set-up files are for the Information Technology and the Cybersecurity and Networks programs only, but any ACG program can be set-up using
appropriate set-up files. There are 3 separate desktop (swing) applications that comprise the ACG SCORER: the class
MainGUI that allows the user to create a course plan for a student, after providing the relevant student data (history
and preferences); the class CourseEditor that allows an admin to edit course data; and the class CourseGroupEditor 
that allows an admin to enter or modify course groups and the constraints they represent.
Notice that the program uses the GUROBI optimization solver for solving the MILP that models the objectives and 
constraints of the resulting problem, and to do that, uses the GUROBI Java API. The GUROBI library is not provided
in this project; the user of this program must separately download and install GUROBI (version 9.5+) and obtain a
valid license for the program. After these steps, the user must copy the GUROBI.jar from the installation directory
to the local project director /lib folder, for the program to compile and run.

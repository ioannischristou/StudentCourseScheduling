package edu.acg.itss.tests;

import gurobi.*;

/**
 * tests calling the GUROBI optimizer. Adapted from Gurobi examples for solving
 * LP problems.
 * @author itc
 */
public class GurobiTest {

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java GurobiTest <filename>");
      System.exit(1);
    }

    try {
      // Read model and determine whether it is a MIP
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);
      if (model.get(GRB.IntAttr.IsMIP) == 0) {
        System.out.println("The model is not a MIP");
        System.exit(1);
      }

      model.optimize();

      int status = model.get(GRB.IntAttr.Status);

      if (status == GRB.Status.INF_OR_UNBD ||
          status == GRB.Status.INFEASIBLE  ||
          status == GRB.Status.UNBOUNDED     ) {
        System.out.println("The model cannot be solved because it is "
            + "infeasible or unbounded");
        System.exit(1);
      }

      if (status != GRB.Status.OPTIMAL) {
        System.out.println("Optimization was stopped with status " + status);
        System.exit(0);
      }

      // Find the smallest variable value
      double minVal = GRB.INFINITY;
      GRBVar minVar = null;
      for (GRBVar v : model.getVars()) {
        double sol = v.get(GRB.DoubleAttr.X);
        if ((sol > 0.0001) && (sol < minVal) &&
            (v.get(GRB.DoubleAttr.LB) == 0.0)) {
          minVal = sol;
          minVar = v;
        }
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
 
}

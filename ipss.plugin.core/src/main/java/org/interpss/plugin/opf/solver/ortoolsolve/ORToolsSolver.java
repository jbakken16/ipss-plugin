package org.interpss.plugin.opf.solver.ortoolsolve;

import java.util.List;

import org.interpss.plugin.opf.common.OPFLogger;
import org.interpss.plugin.opf.constraint.OpfConstraint;
import org.interpss.plugin.opf.constraint.dc.ActivePowerEqnConstraintCollector;
import org.interpss.plugin.opf.constraint.dc.BusMinAngleConstraintCollector;
import org.interpss.plugin.opf.constraint.dc.GenMwOutputConstraintCollector;
import org.interpss.plugin.opf.constraint.dc.LineMwFlowConstraintCollector;
import org.interpss.plugin.opf.objectiveFunction.ORToolsObjectiveFunctionCollector;
import org.interpss.plugin.opf.solver.AbstractOpfSolver;
import org.interpss.plugin.opf.util.OpfDataHelper;

import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfNetwork;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class ORToolsSolver extends AbstractOpfSolver{

    // -----------------------------------------------------------------------------------
    // ============================= Class Variables =====================================
    // -----------------------------------------------------------------------------------
    private String solverID;
    private MPSolver solver;
    private MPObjective objective;
    private MPSolver.ResultStatus resultStatus;
    private MPVariable[] active;
    private MPVariable[] Pg;
    private MPVariable[] theta;
    private MPConstraint[] busPowerConstraint;
    private MPConstraint[] lineFlowConstraint;
    private MPConstraint[] genLimitConstraint;
    private int idxGenConstraints;
    private int idxAngleConstraints;
    private int idxPowerConstraints;
    private int idxLineConstraints;
    private final double infinity = java.lang.Double.POSITIVE_INFINITY;

    private final String defaultSolverID = "SCIP";
    /** solver_id is case insensitive, and the following names are supported:
    *   - CLP_LINEAR_PROGRAMMING or CLP
    *   - CBC_MIXED_INTEGER_PROGRAMMING or CBC
    *   - GLOP_LINEAR_PROGRAMMING or GLOP
    *   - BOP_INTEGER_PROGRAMMING or BOP
    *   - SAT_INTEGER_PROGRAMMING or SAT or CP_SAT
    *   - SCIP_MIXED_INTEGER_PROGRAMMING or SCIP
    *   - GUROBI_LINEAR_PROGRAMMING or GUROBI_LP
    *   - GUROBI_MIXED_INTEGER_PROGRAMMING or GUROBI or GUROBI_MIP
    *   - CPLEX_LINEAR_PROGRAMMING or CPLEX_LP
    *   - CPLEX_MIXED_INTEGER_PROGRAMMING or CPLEX or CPLEX_MIP
    *   - XPRESS_LINEAR_PROGRAMMING or XPRESS_LP
    *   - XPRESS_MIXED_INTEGER_PROGRAMMING or XPRESS or XPRESS_MIP
    *   - GLPK_LINEAR_PROGRAMMING or GLPK_LP
    *   - GLPK_MIXED_INTEGER_PROGRAMMING or GLPK or GLPK_MIP
    */

    // -----------------------------------------------------------------------------------
    // ============================== Constructors =======================================
    // -----------------------------------------------------------------------------------
    public ORToolsSolver(OpfNetwork opfNet, constraintHandleType constType, String solver_ID) {
        super(opfNet, constType);
        this.numOfVar = numOfGen + numOfBus;
        this.solverID = (solver_ID.equals("default")) ? defaultSolverID : solver_ID;
    }

    public ORToolsSolver(OpfNetwork opfNet, constraintHandleType constType) {
        this(opfNet, constType, "default");
    }

    // -----------------------------------------------------------------------------------
    // ================================= Builders ========================================
    // -----------------------------------------------------------------------------------
    @Override
    public void build(List<OpfConstraint> cstContainer) {
		
        solver = MPSolver.createSolver(solverID);
        if (solver == null) {
            System.out.println("Could not create solver " + solverID);
            return;
        }

        // Constraint Order: GenLims -> AngleLims -> Power Equality -> Line Inequality 
            idxGenConstraints = cstContainer.size();
        new GenMwOutputConstraintCollector(this.getNetwork(),cstContainer).collectConstraint();

            idxAngleConstraints = cstContainer.size();
        new BusMinAngleConstraintCollector(this.getNetwork(), cstContainer,	BusAngleLimit).collectConstraint();

            idxPowerConstraints = cstContainer.size();
        new ActivePowerEqnConstraintCollector(this.getNetwork(),cstContainer).collectConstraint();

            idxLineConstraints = cstContainer.size();
        new LineMwFlowConstraintCollector(this.getNetwork(),cstContainer).collectConstraint();

        buildVariables(cstContainer);
        buildObjective();
        buildConstraints(cstContainer);
    }

    private void buildVariables(List<OpfConstraint> cstContainer) {
        active = new MPVariable[numOfGen]; 	 // Generators on/off (binary)
        Pg = new MPVariable[numOfGen];       // Generator power outputs (MW)
        theta = new MPVariable[numOfBus];    // Voltage angles (Rad)
        
        for (int genID = 0; genID < numOfGen; genID++){

            double minGen = cstContainer.get(idxGenConstraints + 2*genID).getLowerLimit();
            double maxGen = cstContainer.get(idxGenConstraints + 2*genID + 1).getUpperLimit();

            Pg[genID] = solver.makeNumVar(minGen, maxGen, "Pg" + genID);
            active[genID] = solver.makeBoolVar("active"+ genID);
        }
        for (int busID = 0; busID < numOfBus; busID++) {

            double maxAngle = cstContainer.get(idxAngleConstraints + 2*busID).getUpperLimit();
            double minAngle = cstContainer.get(idxAngleConstraints + 2*busID + 1).getLowerLimit();

            theta[busID] = solver.makeNumVar(minAngle, maxAngle, "theta" + busID);   
        }
    }

    private void buildObjective() {
        objective = solver.objective();
        objective.setMinimization();
        ORToolsObjectiveFunctionCollector objBuilder = new ORToolsObjectiveFunctionCollector(this.getNetwork());
        double[] fixedCost = objBuilder.getFixedCost();
        double[] varCost = objBuilder.getVarCost();

        for (int genID = 0; genID < numOfGen; genID++){
            objective.setCoefficient(active[genID], fixedCost[genID]);
        	objective.setCoefficient(Pg[genID], varCost[genID]);
        }
    }

    private void buildConstraints(List<OpfConstraint> cstContainer) {

        // Bus Power Equality: generation + flow = demand
        busPowerConstraint = new MPConstraint[numOfBus];
        
        for (int busID = 0; busID < numOfBus; busID++) {

            OpfConstraint con = cstContainer.get(idxPowerConstraints + busID);
            IntArrayList varIDs = con.getColNo();
            DoubleArrayList vals = con.getVal();
            double busDemand = con.getUpperLimit();

        	busPowerConstraint[busID] = solver.makeConstraint(busDemand, busDemand, con.getDesc());
        	
            for (int j = 0; j < varIDs.size(); j++) {

                if (varIDs.get(j) < numOfGen) { 
                    // the variable is a generator
                    busPowerConstraint[busID].setCoefficient(Pg[varIDs.get(j)], 1.0);
                } else { 
                    // the variable is a bus angle
                    busPowerConstraint[busID].setCoefficient(theta[varIDs.get(j)-numOfGen], vals.get(j));
                }
            }
        }

        // Line Flow Inequality: flow <= thermal limits
        lineFlowConstraint = new MPConstraint[numOfBranch];

        for (int lineID = 0; lineID < numOfBranch; lineID++) {

            OpfConstraint con = cstContainer.get(idxLineConstraints + 2*lineID);
            IntArrayList varIDs = con.getColNo();
            DoubleArrayList vals = con.getVal();
            double limit = con.getUpperLimit() * Math.PI / 180;

            lineFlowConstraint[lineID] = solver.makeConstraint(-limit, limit, con.getDesc());

            for (int j = 0; j < varIDs.size(); j++) {
                lineFlowConstraint[lineID].setCoefficient(theta[varIDs.get(j)-numOfGen], vals.get(j));
            }
        }

        // Unit Commitment: Pg <= gen_limit * active 
        genLimitConstraint = new MPConstraint[numOfGen];

        for (int genID = 0; genID < numOfGen; genID++) {
            genLimitConstraint[genID] = solver.makeConstraint(-infinity, 0, "GenLimit"+genID);
            genLimitConstraint[genID].setCoefficient(Pg[genID], 1.0);
            genLimitConstraint[genID].setCoefficient(active[genID], -Pg[genID].ub());
        }
    }

    // -----------------------------------------------------------------------------------
    // ================================== Solution =======================================
    // -----------------------------------------------------------------------------------
    @Override
	public boolean solve() {
		OPFLogger.getLogger().info("Running DC Optimal Power Flow Using OPTools");
        OPFLogger.getLogger().info("Solving with " + solver.solverVersion() + " ...");
		Long startTime = System.currentTimeMillis();
		this.build(cstContainer);
		
		try{
            this.resultStatus = solver.solve();
			this.optimX = getOptimX();
			this.attachedResult();
			this.calLMP();			
			Long endTime = System.currentTimeMillis();
			Long duration = endTime - startTime;	
			OPFLogger.getLogger().info("Optimization terminated.");	
            if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
                OPFLogger.getLogger().info("The problem does not have an optimal solution!");
                if (resultStatus == MPSolver.ResultStatus.FEASIBLE) {
                    OPFLogger.getLogger().info("A potentially suboptimal solution was found");
                } else {
                    OPFLogger.getLogger().info("The solver could not solve the problem.");
                }
            } else {
                this.isSolved = true;
                OPFLogger.getLogger().info("Converged in " + OpfDataHelper.round(duration, 3) +" milliseconds.");	
            }
		}catch(Exception e){
			OPFLogger.getLogger().severe(e.toString());			
			return false;
		}	

		return this.isSolved;
	}

    // -----------------------------------------------------------------------------------
    //  ============================ Additional Methods ==================================
    // -----------------------------------------------------------------------------------
    private double[] getOptimX() {
        double[] x = new double[numOfVar];
        for (int genID = 0; genID < numOfGen; genID++) {
            x[genID] = Pg[genID].solutionValue();
        }
        for (int busID = 0; busID < numOfBus; busID++) {
            x[busID + numOfGen] = theta[busID].solutionValue() * 180 / Math.PI;
        }
        return x;
    }

    /**
	   * Computes and returns the minimized function value (f(x*))
	   * @return double
	   */
    @Override  
	public double getObjectiveFunctionValue(){
		return objective.value();
	}

	  /**
	   * Computes and returns nx1 solution vector (x)
	   * @return doulbe[]
	   */
    @Override
	public double[] getSolution(){
		return optimX;
	}

	@Override
	public boolean isSolved() {		
		return this.isSolved;
	}

	@Override
	public void calLMP() {					
		int cnt = 0;
		double baseMVA=this.getNetwork().getBaseKva()/1000.0;
		for(OpfBus bus: this.getNetwork().getBusList()){				
			bus.setLMP(busPowerConstraint[cnt++].dualValue()/baseMVA);
		}		
	}	
    
	@Override
	public long getIteration() {		
		return solver.iterations();
	}

	@Override
	public void printInputData(String fileName) {				
	}
	
	@Override
	public void debug(String file) {
		OPFLogger.getLogger().info("Running DCOPF debug mode for QP solver...");
		this.build(cstContainer);
		// try {
		// 	// writeMatlabInputFile(file,G,a,Ceq,beq,Ciq, biq);
		// 	OPFLogger.getLogger().info("Output file for debug purpose has been saved to: "+file);
		// } catch (IOException e) {
		// 	OPFLogger.getLogger().severe(e.toString());
		// 	e.printStackTrace();
		// }
		
	}

}

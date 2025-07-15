package org.interpss.plugin.opf.objectiveFunction;

import org.interpss.plugin.opf.common.OPFLogger;

import com.interpss.core.common.curve.NumericCurveModel;
import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfNetwork;

public class ORToolsObjectiveFunctionCollector extends BaseObjectiveFunctionCollector{	
	
    double[] fixedCost = null;
    double[] varCost = null;
	
	public ORToolsObjectiveFunctionCollector(OpfNetwork opfNet){
		super(opfNet);		
		this.fixedCost = new double[numOfVar];
		this.varCost = new double[numOfVar];
		this.setCost();
	}
	
	public double[] getFixedCost(){					
		return fixedCost;
	}
	
	public double[] getVarCost(){
		return varCost;
	}
	
	private void setCost() {		
		//double baseMVA=opfNet.getBaseKva()/1000.0;		
		int genIndex=0;
		try {
			for (OpfBus bus: opfNet.getBusList()){
				if(bus.isOpfGen()){
					NumericCurveModel incType = bus.getOpfGen().getIncCost().getCostModel();
					if(incType.equals(NumericCurveModel.QUADRATIC)){
						OPFLogger.getLogger().severe("MP solver cannot handle quadratic gen cost at bus: "
								+bus.getNumber());						
					}else{
						double fixedC = bus.getOpfGen().getFixedCost();
                        double varC = bus.getOpfGen().getCoeffB();		// is this the linear cost?	
						
						fixedCost[genIndex] = fixedC;						
						varCost[genIndex] = varC;	
						
						genIndex++;
					}
				}
			}		
		}catch(Exception e){
			OPFLogger.getLogger().severe(e.toString());
		}		
	}	

}
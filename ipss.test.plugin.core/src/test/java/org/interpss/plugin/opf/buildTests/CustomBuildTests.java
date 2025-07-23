package org.interpss.plugin.opf.buildTests;

import org.apache.commons.math3.complex.Complex;
import org.interpss.CorePluginTestSetup;
import org.interpss.numeric.datatype.LimitType;
import org.junit.Test;

import com.interpss.opf.OpfBranch;
import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfGen;
import com.interpss.opf.OpfNetwork;
import com.interpss.opf.OpfObjectFactory;
import com.interpss.opf.datatype.OpfBusLimits;
import com.interpss.opf.datatype.OpfDatatypeFactory;

public class CustomBuildTests extends CorePluginTestSetup {

    @Test
    public void CustomBuildTest() throws Exception {

		OpfNetwork opfNet = OpfObjectFactory.createOpfNetwork();	

		OpfBus bus1 = OpfObjectFactory.createOpfBus("Bus1", opfNet);
		bus1.setLoadP(100);
		// bus1.setPGenLimit(new LimitType(200,0));

		OpfGen gen1 = OpfObjectFactory.createOpfGen("Gen1");
		OpfBusLimits gen1_limit = OpfDatatypeFactory.eINSTANCE.createOpfBusLimits();
		gen1_limit.setPLimit(new LimitType(200,0));
		gen1.setOpfLimits(gen1_limit);
		gen1.setFixedCost(0);
		gen1.setCoeffA(10);
		// gen1.setCapacityLimit(new LimitType(200,0));
		// gen1.setParentBus(bus1);

		bus1.getContributeGenList().add(0, gen1);

		OpfBus bus2 = OpfObjectFactory.createOpfBus("Bus2", opfNet);
		bus2.setLoadP(300);

		OpfGen gen2 = OpfObjectFactory.createOpfGen("Gen2");
		OpfBusLimits gen2_limit = OpfDatatypeFactory.eINSTANCE.createOpfBusLimits();
		gen2_limit.setPLimit(new LimitType(500,0));
		gen2.setFixedCost(0);
		gen2.setCoeffA(14);
		gen2.setOpfLimits(gen2_limit);

		bus2.getContributeGenList().add(0, gen2);

		OpfBranch line1 = OpfObjectFactory.createOpfBranch();
		line1.setFromBus(bus1);
		line1.setToBus(bus2);
		line1.setRatingMw1(50);
		line1.setZ(new Complex(0,0.3));

		opfNet.addBranch(line1);

		if (opfNet == null) {
			System.out.println("IEEE Build Error: OpfNetwork is null");
		} else {
			OpfBus bus = opfNet.getBus("Bus1");
			OpfGen gen = bus.getOpfGen();
			OpfBusLimits limits = gen.getOpfLimits();
			double load = bus1.getLoadP();
			System.out.println("Load: " + load);
			System.out.println("Max Gen Limit: " + limits.getPLimit().getMax());
			System.out.println("Min Gen Limit: " + limits.getPLimit().getMin());
			System.out.println(opfNet.net2String());
		}

		// OpfBus bus1 = opfNet.getBus("Bus1");
		// OpfBus bus2 = opfNet.getBus("Bus2");

		// double bus1_maxGen = bus1.getPGenLimit().getMax();
		// double bus2_load = bus2.getLoadP();

		// assertTrue("Bus1 MaxGen 232.4", NumericUtil.equals(bus1_maxGen, 232.4, 0.01));
		// assertTrue("Bus2 Load 21.7", NumericUtil.equals(bus2_load, 21.7, 0.01));

	}

}
    


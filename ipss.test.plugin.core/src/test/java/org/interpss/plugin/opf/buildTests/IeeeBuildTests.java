package org.interpss.plugin.opf.buildTests;

import org.interpss.CorePluginFactory;
import org.interpss.CorePluginTestSetup;
import org.interpss.fadapter.IpssFileAdapter;
import org.interpss.numeric.util.NumericUtil;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfNetwork;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;
import com.interpss.simu.SimuObjectFactory;

public class IeeeBuildTests extends CorePluginTestSetup {

    @Test
    public void ieeeBuildTest() throws Exception {

		String filepath = "testData/opf/ieee.Ieee14Bus.ieee";
		SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.OPF_NET);
		IpssFileAdapter adapter = CorePluginFactory.getFileAdapter(IpssFileAdapter.FileFormat.IEEECDF);
		adapter.load(simuCtx, filepath, false, null);

		System.out.println("SimuContext Description: " + simuCtx.getDesc());

		// AclfNetwork aclfNet = CorePluginFactory
		// 		.getFileAdapter(IpssFileAdapter.FileFormat.IEEECDF)
		// 		.load(filepath)
		// 		.getAclfNet();	

		// System.out.println(aclfNet.net2String());

		OpfNetwork opfNet = simuCtx.getOpfNet();	
		// System.out.println(opfNet.net2String());

		if (opfNet == null) {
			System.out.println("IEEE Build Error: OpfNetwork is null");
			return;
		}

		OpfBus bus1 = opfNet.getBus("Bus1");
		OpfBus bus2 = opfNet.getBus("Bus2");

		double bus1_maxGen = bus1.getPGenLimit().getMax();
		double bus2_load = bus2.getLoadP();

		assertTrue("Bus1 MaxGen 232.4", NumericUtil.equals(bus1_maxGen, 232.4, 0.01));
		assertTrue("Bus2 Load 21.7", NumericUtil.equals(bus2_load, 21.7, 0.01));

	}

}
    


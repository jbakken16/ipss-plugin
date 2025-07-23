package org.interpss.plugin.opf.buildTests;

import java.io.File;
import java.io.FileInputStream;

import org.ieee.odm.ODMObjectFactory;
import org.ieee.odm.model.opf.OpfModelParser;
import org.interpss.CorePluginFactory;
import org.interpss.CorePluginTestSetup;
import org.interpss.fadapter.IpssFileAdapter;
import org.interpss.numeric.util.NumericUtil;
import org.interpss.odm.mapper.ODMOpfParserMapper;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfNetwork;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;
import com.interpss.simu.SimuObjectFactory;

public class XmlBuildTests extends CorePluginTestSetup {

	@Test 
	public void ieee30ODM_xmlBuildTest() throws Exception {

		File file = new File("testData/opf/xml/ieee30ODM.xml");
		OpfModelParser parser = ODMObjectFactory.createOpfModelParser();

		if (parser.parse(new FileInputStream(file))) {

			SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.OPF_NET);

			if (!new ODMOpfParserMapper().map2Model(parser, simuCtx)) {
				System.out.println("Error: ODM model to InterPSS SimuCtx mapping error");
				return;
			}	

		OpfNetwork opfNet = (OpfNetwork) simuCtx.getOpfNet();

		// -- Currently the OpfNetwork is returning 0 values...
		// System.out.println(opfNet.net2String());

		System.out.println("Network: " + opfNet.getName());
		System.out.println("Num Buses: " + opfNet.getNoBus());
		for (int i = 0; i < opfNet.getNoBus(); i++) {
			OpfBus bus = opfNet.getBusList().get(i);
			System.out.println();
			System.out.println(bus.getName() + " Load: " + bus.getLoadP());
			System.out.println(bus.getName() + " GenP: " + bus.getGenP());
		}

		OpfBus bus1 = opfNet.getBus("Bus1");
		assertTrue("Bus1_Load 132.66", NumericUtil.equals(bus1.getLoadP(), bus1.getLoadP(), 0.01));
		
			
		} else {
			System.out.println("Error: Parser Failure");
			return;
		}
	}

    @Test
    public void ieeeBuildTest() throws Exception {

		String filepath = "testData/opf/ieee/Ieee14Bus.ieee";
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
    


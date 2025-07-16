package org.interpss.plugin.opf;

import java.io.File;
import java.io.FileInputStream;

import org.interpss.CorePluginTestSetup;
import org.interpss.numeric.util.NumericUtil;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


import com.interpss.opf.OpfNetwork;
import org.interpss.plugin.opf.solver.ortoolsolve.ORToolsSolver;

import org.ieee.odm.ODMObjectFactory;
import org.ieee.odm.model.opf.OpfModelParser;
import org.interpss.odm.mapper.ODMOpfParserMapper;

public class opf_3bus_Test extends CorePluginTestSetup {

	@Test 
	public void ORToolsTest() throws Exception {

		File file = new File("testData/opf/opf_3bus_test.xml");

		OpfModelParser parser = ODMObjectFactory.createOpfModelParser();
		ODMOpfParserMapper mapper = new ODMOpfParserMapper();
		parser.parse(new FileInputStream(file));

		OpfNetwork opfNet = (OpfNetwork) mapper.map2Model(parser).getOpfNet();

		// OpfNetwork opfNet = CorePluginFactory.getFileAdapter(IpssFileAdapter.FileFormat.IEEECDF)
		// 		.load("testData/adpter/ieee_format/Ieee14Bus.ieee")
		// 		.getOpfNet();

		ORToolsSolver solver = new ORToolsSolver(opfNet, null);
		solver.build(null);
		solver.solve();
		double objective = solver.getObjectiveFunctionValue();
		double[] solution = solver.getSolution();
		double Pg1 = solution[0];


		//System.out.println(DclfOutFunc.dclfResults(dclfAlgo, false));
		/*
		   Bud Id       VoltAng(deg)     Gen     Load    ShuntG
		=========================================================
		    Bus1           0.000       225.43     0.00     0.00 
		    Bus2          -0.092        40.00    21.70     0.00 
		    Bus3          -0.233         0.00    94.20     0.00 		
		 */


		assertTrue("Aclf 232.393", NumericUtil.equals(objective, 225.43, 0.01));
		assertTrue("Pg1 232.393", NumericUtil.equals(Pg1, 225.43, 0.01));
		// assertTrue("", NumericUtil.equals(angle, -0.092, 0.001));		
	}
}


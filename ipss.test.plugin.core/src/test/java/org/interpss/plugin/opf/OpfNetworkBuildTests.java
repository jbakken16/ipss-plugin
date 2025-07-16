package org.interpss.plugin.opf;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.ieee.odm.ODMObjectFactory;
import org.ieee.odm.model.opf.OpfModelParser;
import org.interpss.CorePluginFactory;
import org.interpss.CorePluginTestSetup;
import org.interpss.fadapter.IpssFileAdapter;
import org.interpss.numeric.util.NumericUtil;
import org.interpss.odm.mapper.ODMOpfParserMapper;
import org.junit.Test;

import com.interpss.opf.OpfBus;
import com.interpss.opf.OpfNetwork;

public class OpfNetworkBuildTests extends CorePluginTestSetup {

	@Test 
	public void xmlBuildTest() throws Exception {

		File file = new File("testData/opf/opf_3bus_test.xml");

		OpfModelParser parser = ODMObjectFactory.createOpfModelParser();
		ODMOpfParserMapper mapper = new ODMOpfParserMapper();
		parser.parse(new FileInputStream(file));

		OpfNetwork opfNet = (OpfNetwork) mapper.map2Model(parser).getOpfNet();

		OpfBus bus1 = opfNet.getBus("Bus1");
        double bus1_load = bus1.getLoadP();

		assertTrue("Bus1_Load 132.66", NumericUtil.equals(bus1_load, 132.66, 0.01));

	}

    @Test
    public void ieeeBuildTest() throws Exception {

        OpfNetwork opfNet = CorePluginFactory
				.getFileAdapter(IpssFileAdapter.FileFormat.IEEECDF)
				.load("testData/adpter/ieee_format/Ieee14Bus.ieee")
				.getOpfNet();	

        OpfBus bus1 = opfNet.getBus("Bus1");
        OpfBus bus2 = opfNet.getBus("Bus2");

        double bus1_maxGen = bus1.getPGenLimit().getMax();
        double bus2_load = bus2.getLoadP();

        assertTrue("Bus1 MaxGen 232.4", NumericUtil.equals(bus1_maxGen, 232.4, 0.01));
        assertTrue("Bus2 Load 21.7", NumericUtil.equals(bus2_load, 21.7, 0.01));

            

    }
    
}

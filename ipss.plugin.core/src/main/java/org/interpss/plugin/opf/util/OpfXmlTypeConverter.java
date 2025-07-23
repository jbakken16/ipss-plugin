package org.interpss.plugin.opf.util;

import org.ieee.odm.schema.OpfNetworkXmlType;

import com.interpss.opf.OpfNetwork;
import com.interpss.opf.OpfObjectFactory;

public class OpfXmlTypeConverter {

    public OpfNetwork convertOpfNetworkXmlType (OpfNetworkXmlType net) {

        OpfNetwork opfNet = OpfObjectFactory.createOpfNetwork();	

        int numBuses = net.getBusList().getBus().size();


        return opfNet;
    }
    
}

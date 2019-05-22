package etb.etbDL.services;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import etb.etbDL.utils.Expr;
import etb.etbDL.utils.Rule;

public abstract class externPred2Service {
    List<String> outParams = new ArrayList();
    String evidence = "";
    
    public abstract genericWRP getGroundParams(String serviceName, ArrayList<String> args, String mode);

    public Expr invoke(String serviceName, ArrayList<String> args, String mode, String signature) {
        genericWRP genWRP = getGroundParams(serviceName, args, mode);
        if (genWRP == null) return null;
        
        genWRP.invoke(mode, args);
        outParams = genWRP.getOutParams();
        evidence = genWRP.getEvidence();
        
        return (new Expr(serviceName, outParams, signature, mode));
    }

    public String getEvidence() {
        return this.evidence;
    }
    
}

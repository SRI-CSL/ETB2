package etb.etbCS.utils;

import java.util.Map;
import java.util.HashMap;
import etb.etbDL.utils.Expr;

public class queryResult {

    Expr resultExpr = null;
    String evidence = null;
    Map<String, String> bindings = new HashMap();
    
    public queryResult() {
        
    }
    
    public queryResult(Expr resultExpr, String evidence) {
        this.resultExpr = resultExpr;
        this.evidence = evidence;
    }
    
    public queryResult(Expr resultExpr, String evidence, Map<String, String> bindings) {
        this.resultExpr = resultExpr;
        this.evidence = evidence;
        this.bindings = bindings;
    }

    public Expr getResultExpr(){
        return resultExpr;
    }
    
    public String getEvidence() {
        return evidence;
    }
    
    public Map<String, String> getBindings(){
        return bindings;
    }
    
    public void print() {
        System.out.println("result: " + resultExpr.toString());
        System.out.println("evidence: " + evidence);
    }
    
}


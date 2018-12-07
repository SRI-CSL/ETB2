package etb.etbCS.utils;

import etb.etbDL.utils.Expr;

public class queryResult {

    Expr resultExpr = null;
    String evidence = null;
    
    
    public queryResult() {
        
    }
    
    public queryResult(Expr resultExpr, String evidence) {
        this.resultExpr = resultExpr;
        this.evidence = evidence;
    }

    public Expr getResultExpr(){
        return resultExpr;
    }
    
    public String getEvidence() {
        return evidence;
    }
    
    public void print() {
        System.out.println("result: " + resultExpr.toString());
        System.out.println("evidence: " + evidence);
    }
    
}


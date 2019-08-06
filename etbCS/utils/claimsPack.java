package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbCS.etbNode;

import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class claimsPack {
    
    Map<Integer, claimSpec> claims = new HashMap();
    
    public claimsPack() {
        claims = new HashMap();
    }
    
    public claimsPack(JSONArray claimsJSON) {
        Iterator<JSONObject> claimsIter = claimsJSON.iterator();
        while (claimsIter.hasNext()) {
            JSONObject claimObj = (JSONObject) claimsIter.next();
            claimSpec claim = new claimSpec(claimObj);
            this.claims.put(claim.getID(), claim);
        }
    }
    
    public JSONArray toJSONObject() {
        JSONArray claimsJSON = new JSONArray();
        for (Integer claimID : claims.keySet()) {
            claimsJSON.add(claims.get(claimID).toJSONObject());
        }
        return claimsJSON;
    }

    public void checkStatus(servicePackage servicePack, Map<String, workFlowSpec> workflows, String repoDirPath) {
        for (Integer claimID : claims.keySet()) {
            switch (claims.get(claimID).checkStatus(servicePack, repoDirPath, workflows)) {
                case 0: System.out.println("-> \u001B[32mclaim is up-to-date\u001B[30m");
                    break;
                case 1: System.out.println("-> \u001B[31mclaim is outdated\u001B[30m (please maintain claim)");
                    //claims.get(claimID).maintain(updatedServiceIDs);
                    break;
                case 2: System.out.println("-> \u001B[31mclaim is outdated\u001B[30m (please upgrade/recreat the claim)");
                    break;
                case 3:  System.out.println("-> \u001B[31mclaim is outdated\u001B[30m (please upgrade/recreat the claim)");
                    break;
                default: System.out.println("-> \u001B[31munknown claim status\u001B[30m");
                    break;
            }
        }
    }

    public void add(claimSpec claim) {
        claims.put(claim.getID(), claim);
    }
    
    //for terminal query input
    private Expr readQuery(String inputQuery, String repoDirPath) {
        try {
            Reader reader = new StringReader(inputQuery);
            StreamTokenizer scan = new StreamTokenizer(reader);
            scan.ordinaryChar('.');
            scan.commentChar('%');
            scan.quoteChar('"');
            scan.quoteChar('\'');
            Expr query = etbDLParser.parseExpr(scan, repoDirPath);
            System.out.println("=> valid query found: " + query.toString());
            //reader.close();
            return query;
        }
        catch (IOException | DatalogException e){
            e.printStackTrace();
            System.out.println("=> invalid claim format \u001B[31m(operation not successful)\u001B[30m");
            return null;
        }
        
    }

    public void add(String claimStr, Map<String, serviceSpec> services, workFlowsPackage wfPack, String repoDirPath, etbNode etcSS) {
        
        Expr claimExpr = readQuery(claimStr, repoDirPath);
        if (claims.containsKey(claimExpr.hashCode())) {
            System.out.println("=> claim already exists \u001B[31m(operation not successful)\u001B[30m");
            return;
        }
        //grabbing a matching workflow(s)
        Map<String, workFlowSpec> workflows = wfPack.getWorkflows();
        List<String> applWorkflows = wfPack.getWorkflows(claimExpr.queryHashCode());
        System.out.println("=> number of matching applicable workflows: " + applWorkflows.size());
        for (String workFlowID : applWorkflows) {
            etbDatalog dlPack = new etbDatalog();
            dlPack.parseDatalogScript(workflows.get(workFlowID).getScriptPath(), repoDirPath);
            etbDatalogEngine dlEngine = new etbDatalogEngine(claimExpr);
            Collection<Map<String, String>> answers;
            if ((answers = dlEngine.run(etcSS, dlPack)) == null) {
                System.out.println("=> \u001B[31mclaim addition not successful\u001B[30m (workflow: " + workFlowID + ")");
            }
            else {
                claimSpec claim = new claimSpec(claimExpr, answers, workFlowID, workflows.get(workFlowID).getScriptID(repoDirPath), repoDirPath);
                claim.setDerivationRules(dlEngine.getDerivationRules());
                claim.setDerivationFacts(dlEngine.getDerivationFacts());
                claim.setDerivationServices(dlEngine.getDerivationServices());
                claims.put(claimExpr.hashCode(), claim);
                System.out.println("=> claim added successfully");
                break;
            }
        }
    }
    
    public void remove(Integer claimID) {
        //checking if workflow already exists with the same name
        if (claims.keySet().contains(claimID)) {
            claims.remove(claimID); //TODO: chain of reactions for claim removal
            System.out.println("=> claim removed successfully");
        }
        else {
            System.out.println("=> a claim with ID '" + claimID + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n==> total number of claims: " + claims.size());
        int count = 1;
        for (Integer claimID : claims.keySet()) {
            sb.append("\n[claim " + count++ + "]");
            sb.append(claims.get(claimID));
        }
        return sb.toString();
    }
    
    public void print() {
        System.out.println("==> total number of claims: " + claims.size());
        int count = 1;
        for (Integer claimID : claims.keySet()) {
            System.out.println("[claim " + count++ + "] ");
            claims.get(claimID).print();
        }
    }
    
    public void update(Integer claimID, servicePackage servicePack, etbNode etcSS) {
        if (claims.containsKey(claimID)) {
            claims.get(claimID).update(servicePack, etcSS);
        }
        else {
            System.out.println("ERROR. unknown claimID '" + claimID + "'");
        }
    }
    
}


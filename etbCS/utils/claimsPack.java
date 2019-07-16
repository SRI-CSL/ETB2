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
    
    Map<String, claimSpec> claims = new HashMap();
    
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
        for (String claimID : claims.keySet()) {
            claimsJSON.add(claims.get(claimID).toJSONObject());
        }
        return claimsJSON;
    }

    public void checkStatus(Map<String, workFlowSpec> workflows, String repoDirPath) {
        for (String claimID : claims.keySet()) {
            if (claims.get(claimID).checkStatus(repoDirPath, workflows) != 0) {
                System.out.println("-> \u001B[31mclaim is outdated\u001B[30m (please update/upgrade/recreat the claim)");
            }
            else {
                System.out.println("-> \u001B[32mclaim is up-to-date\u001B[30m");
            }
        }
        
    }

    public void add(claimSpec claim) {
        claims.put(claim.getID(), claim);
    }
        
    public void add(String claimStr, Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        try {
            Reader reader = new StringReader(claimStr);
            StreamTokenizer scan = new StreamTokenizer(reader);
            scan.ordinaryChar('.');
            scan.commentChar('%');
            scan.quoteChar('"');
            scan.quoteChar('\'');
            Expr claimExpr = etbDLParser.parseExpr(scan, repoDirPath);
            claimExpr.print();
            reader.close();
            
            //grabbing a matching workflow(s)
            Set<String> wfNames = workflows.keySet();
            Iterator<String> wfNamesIter = wfNames.iterator();
            
            int matchingWorkflowsCount = 0;
            boolean validClaim = false;
            
            while (wfNamesIter.hasNext()) {
                String wfName = wfNamesIter.next();
                //workflows.get(wfName).print();
                System.out.println("=> claimExpr: " + claimExpr.toString());
                if (workflows.get(wfName).containsQuery(claimExpr)) {
                    matchingWorkflowsCount++;
                    try {
                        etbDatalog dlPack = new etbDatalog();
                        String wfScriptPath = workflows.get(wfName).getScriptPath();
                        dlPack.parseToDatalog(wfScriptPath, repoDirPath);
                        dlPack.setGoal(claimExpr);
                        etbDatalogEngine dlEngine = new etbDatalogEngine();
                        Collection<Map<String, String>> answers = dlEngine.run(etcSS, dlPack);//TODO: single claim derivation
                        
                        if (answers != null) {
                            claimSpec claim = new claimSpec(repoDirPath, claimExpr, answers, wfName, utils.getSHA1(utils.getFilePathInDirectory(wfScriptPath, repoDirPath)), dlEngine.getDerivation(), claims.size());
                            claims.put(claim.getID(), claim);
                            validClaim = true;
                            break; //more claims per run??
                        }
                        
                    }
                    catch (DatalogException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (validClaim == true) {
                System.out.println("=> claim added successfully");
            }
            else {
                System.out.println("=> \u001B[31mclaim addition not successful\u001B[30m (number of matching workflows: " + matchingWorkflowsCount + ")");
            }
            
        }
        catch (IOException | DatalogException e){
            e.printStackTrace();
            System.out.println("=> invalid claim format \u001B[31m(operation not successful)\u001B[30m");
        }
        
    }

    public void remove(String claimID) {
        //checking if workflow already exists with the same name
        if (claims.keySet().contains(claimID)) {
            claims.remove(claimID); //TODO: chain of reactions for claim removal
            System.out.println("=> claim removed successfully");
        }
        else {
            System.out.println("=> a claim with ID '" + claimID + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
        }
    }

    public void print() {
        System.out.println("==> total number of claims: " + claims.size());
        int count = 1;
        for (String claimID : claims.keySet()) {
            System.out.println("==> [claim " + count++ + "] ID : " + claimID);
            claims.get(claimID).print();
        }
    }
    
    public void update(String claimID, Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        if (claims.containsKey(claimID)) {
            claims.get(claimID).update(workflows, repoDirPath, etcSS);
        }
        else {
            System.out.println("ERROR. Unknown claimID '" + claimID + "'");
        }
    }

    public void upgrade(String claimID, Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        if (claims.containsKey(claimID)) {
            claims.get(claimID).upgrade(workflows, repoDirPath, etcSS);
        }
        else {
            System.out.println("ERROR. Unknown claimID '" + claimID + "'");
        }
    }

    public void recreate(String claimID, Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        if (claims.containsKey(claimID)) {
            claims.get(claimID).recreate(workflows, repoDirPath, etcSS);
        }
        else {
            System.out.println("ERROR. Unknown claimID '" + claimID + "'");
        }
    }

}


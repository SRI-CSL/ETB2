package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class workFlowsPackage {
    
    String repoDirPath;
    Map<String, workFlowSpec> workflows = new HashMap();
    
    public workFlowsPackage() {
        workflows = new HashMap();
    }
    
    public workFlowsPackage(String repoDirPath, JSONArray workflowsJSON) {
        this.repoDirPath = repoDirPath;
        Iterator<JSONObject> wfIter = workflowsJSON.iterator();
        while (wfIter.hasNext()) {
            JSONObject workFlowSpecObj = (JSONObject) wfIter.next();
            this.workflows.put((String) workFlowSpecObj.get("ID"), new workFlowSpec(workFlowSpecObj));
        }
    }
    
    public JSONArray toJSONObject() {
        JSONArray workflowsJSON = new JSONArray();
        for (String workflowID : workflows.keySet()) {
            workflowsJSON.add(workflows.get(workflowID).toJSONObject());
        }
        return workflowsJSON;
    }
    
    public void add(String specFilePath) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject workFlowSpecJSON = (JSONObject) parser.parse(new FileReader(specFilePath));
            
            String ID = (String) workFlowSpecJSON.get("ID");
            if ((ID = ID.trim()) == null) {
                System.out.println("=> no workflow ID given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            else if (workflows.containsKey(ID)) {
                System.out.println("=> a workflow with ID '" + ID + "' exists \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            
            String scriptPath, scriptPath0 = (String) workFlowSpecJSON.get("script");
            if (scriptPath0 == null) {
                System.out.println("=> no workflow script found \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            else if ((scriptPath = utils.getFilePathInDirectory(scriptPath0.trim(), repoDirPath)) == null) {
                System.out.println("=> script file '" + scriptPath0 + "' does not exist \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            
            JSONArray queriesJSON = (JSONArray) workFlowSpecJSON.get("queries");
            Iterator<JSONObject> queryIter = queriesJSON.iterator();
            Map<String, querySpec> wfQueryList = new HashMap();
            while (queryIter.hasNext()) {
                JSONObject queryJSON = queryIter.next();
                querySpec wfQuery = new querySpec(queryJSON);
                if (wfQuery.isValid()) {
                    wfQueryList.put(wfQuery.getID(), wfQuery);
                }
                else {
                    System.out.println("=> invalid query '" + queryJSON.toString() + "'' \u001B[31m(operation not successful)\u001B[30m");
                    return;
                }
            }
            
            workflows.put(ID, new workFlowSpec(ID, wfQueryList, scriptPath));
            System.out.println("=> workflow added successfully");
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
    }

    public void remove(String workFlowID) {
        if (!workflows.keySet().contains(workFlowID)) {
            System.out.println("=> workflow with the name '" + workFlowID + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
            return;
        }
        workflows.remove(workFlowID);
        System.out.println("=> tool removed successfully");
    }
    
    public Map<String, workFlowSpec> getWorkflows() {
        return workflows;
    }
    
    public void print() {
        System.out.println("==> total number of workflows: " + workflows.size());
        int count = 1;
        for (String workflowID : workflows.keySet()) {
            System.out.println("==> [workflow " + count++ + "] ID : " + workflowID);
            workflows.get(workflowID).print();
        }
    }
    
}


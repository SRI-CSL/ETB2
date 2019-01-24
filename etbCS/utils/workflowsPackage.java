package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbCS.etcServer;

import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class workflowsPackage {
    
    String repoDirPath;
    Map<String, workFlowSpec> workflows = new HashMap();
    
    public workflowsPackage() {
        workflows = new HashMap();
    }
    
    public workflowsPackage(String repoDirPath, JSONArray workflowsJSON) {
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

    public void add() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            String wfScriptFilePath = null, workFlowName;
            boolean soFarChecked = false;
            
            while (true) {
                System.out.print("--> workflow name : ");
                workFlowName = in.nextLine();
                
                if (workflows.keySet().contains(workFlowName)) {
                    System.out.println("=> a workflow with the name '" + workFlowName + "' exists \u001B[31m(operation not successful)\u001B[30m");
                    System.out.print("=> try again? [y] to try again : ");
                    if (in.nextLine().equals("y"))
                        continue;
                    break;
                }
                soFarChecked = true;
                break;
            }
            
            if (soFarChecked) {
                soFarChecked = false;
                while (true) {
                    System.out.print("--> workflow script path : ");
                    wfScriptFilePath = in.nextLine();
                    if ((wfScriptFilePath = utils.getFilePathInDirectory(wfScriptFilePath, repoDirPath)) == null) {
                        System.out.println("=> script file does not exist \u001B[31m(operation not successful)\u001B[30m");
                        System.out.print("=> try again? [y] to try again : ");
                        if (in.nextLine().equals("y"))
                            continue;
                        break;
                    }
                    //TODO: more sanity checks on workflow
                    soFarChecked = true;
                    break;
                }
                
            }
            
            if (soFarChecked) {
                while (true) {
                    Map<String, querySpec> wfQueryList = new HashMap();
                    System.out.print("--> query list (see help menu for syntax) : "); //{<pred; (file, string); ++>}
                    String queryListStr = in.nextLine();
                    wfQueryList = getWorkFlowQueryList(queryListStr);
                    
                    if (wfQueryList == null) {
                        System.out.println("=> invalid query list \u001B[31m(operation not successful)\u001B[30m");
                        System.out.print("=> try again? [y] to try again : ");
                        if (in.nextLine().equals("y"))
                            continue;
                        break;
                    }
                    
                    workflows.put(workFlowName, new workFlowSpec(workFlowName, wfQueryList, wfScriptFilePath));
                    System.out.println("=> workflow added successfully");
                    break;
                }
            }
            
            System.out.print("=> add more workflows? [y] to add more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        //save();
    }

    public void remove() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            
            while (true) {
                System.out.print("--> name : ");
                String name = in.nextLine();
                
                if (!workflows.keySet().contains(name)) {
                    System.out.println("=> workflow with the name '" + name + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
                    System.out.print("=> try again? [y] to try again : ");
                    if (!in.nextLine().equals("y"))
                        break;
                    continue;
                }
                workflows.remove(name);
                System.out.println("=> tool removed successfully");
                break;
            }
            
            System.out.print("=> remove more tools? [y] to remove more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        //save();
    }

    private Map<String, querySpec> getWorkFlowQueryList(String queryListStr) {
        
        //ArrayList<querySpec> wfQueryList = new ArrayList();
        Map<String, querySpec> wfQueryList = new HashMap();
        
        int queryListLen = queryListStr.length() - queryListStr.replace("<", "").length();
        
        String queryPat = "[<]\\s*(\\w+)\\s*[;]\\s*([(].+[)])\\s*[;]\\s*([+-]+)\\s*[>]";
        String queryListPat = "\\s*" + queryPat + "\\s*";
        for (int i=1; i < queryListLen; i++) {
            queryListPat += "[,]\\s*" + queryPat + "\\s*";
        }
        queryListPat = "\\s*[{]\\s*" + queryListPat + "\\s*[}]\\s*";
        
        Pattern p1 = Pattern.compile(queryListPat);
        Matcher m1 = p1.matcher(queryListStr);
        if (m1.find()) {
            String pred, signStr, mode;
            for( int i=1; i <= m1.groupCount(); i=i+3) {
                //System.out.println("m1.group(" + i + ") : " + m1.group(i));
                pred = m1.group(i);
                signStr = m1.group(i+1);
                mode = m1.group(i+2);
                
                String typeListPat = "(string|file|string_list|file_list)";
                for (int j=1; j < mode.length(); j++) {
                    typeListPat += "\\s*[,]\\s*(string|file|string_list|file_list)";
                }
                typeListPat = "[(]\\s*" + typeListPat + "\\s*[)]";
                
                String encodSignStr = "";
                Pattern p2 = Pattern.compile(typeListPat);
                Matcher m2 = p2.matcher(signStr);
                if (m2.find()) {
                    for( int j=1; j <= m2.groupCount(); j++) {
                        //System.out.println("m2.group(" + j + ") : " + m2.group(j));
                        switch(m2.group(j)){
                            case "string":
                                encodSignStr += "1";
                                break;
                            case "file":
                                encodSignStr += "2";
                                break;
                            case "string_list":
                                encodSignStr += "3";
                                break;
                            case "file_list":
                                encodSignStr += "4";
                                break;
                            default:
                                System.out.println("unknown type '" + m2.group(j) + "'");
                                return null;
                        }
                    }
                    //System.out.println("encodSignStr: " + encodSignStr);
                    //wfQueryList.add(new querySpec(pred, encodSignStr, mode));
                    wfQueryList.put(pred, new querySpec(encodSignStr, mode));
                }
                else {
                    System.out.println("invalid query signature '" + signStr + "'");
                    return null;
                }
            }
            return wfQueryList;
        }
        else {
            System.out.println("invalid workflow query list");
            return null;
        }
        
    }

    public Map<String, workFlowSpec> getWorkflows() {
        return workflows;
    }
    
    public void print() {
        if (workflows.size() == 0) {
            System.out.println("==> no workflows found");
        }
        for (String workflowID : workflows.keySet()) {
            System.out.println("==> ID : " + workflowID);
            workflows.get(workflowID).print();
        }
    }
    
}


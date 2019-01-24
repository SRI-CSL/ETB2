package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import etb.etbDL.utils.*;

public class workFlowSpec {

    String ID;
    Map<String, querySpec> queryList = new HashMap();
    String scriptPath;
    
    public workFlowSpec(String ID, Map<String, querySpec> queryList, String scriptPath) {
        this.ID = ID;
        this.queryList = queryList;
        this.scriptPath = scriptPath;
    }

    public workFlowSpec(JSONObject workFlowSpecJSON) {
        
        this.ID = (String) workFlowSpecJSON.get("ID");
        JSONArray queryListJSON = (JSONArray) workFlowSpecJSON.get("queryList");
        for (int i = 0; i < queryListJSON.size(); i++) {
            JSONObject queryJSON = (JSONObject) queryListJSON.get(i);
            String queryPred = (String) queryJSON.get("pred");
            this.queryList.put(queryPred, new querySpec(queryJSON));
        }
        this.scriptPath = (String) workFlowSpecJSON.get("scriptPath");
    }

    public String getScriptPath() {
        return this.scriptPath;
    }
    
    public JSONObject toJSONObject() {
        
        JSONObject NewObj = new JSONObject();
        
        NewObj.put("ID", ID);
        
        JSONArray queryListJSON = new JSONArray();
        for (String queryPred : queryList.keySet()) {
            queryListJSON.add(queryList.get(queryPred).toJSONObj(queryPred));
        }
        NewObj.put("queryList", queryListJSON);
        NewObj.put("scriptPath", scriptPath);
        
        return NewObj;
        
    }
    
    public void print(String indent, String repoDirPath) {
        System.out.println(indent + "ID : " + ID);
        System.out.println(indent + "scriptPath : " + scriptPath);
        System.out.println(indent + "queryList: " + queryList.toString());
    }
    
    
    public void print() {
        //System.out.println("--> ID: " + ID);
        System.out.println("--> scriptPath: " + scriptPath);
        System.out.println("--> queryList: " + queryList.toString());
    }
    
    
    public boolean containsQuery(Expr query) {
        if (this.queryList.containsKey(query.getPredicate())) {
            querySpec qSpec = this.queryList.get(query.getPredicate());
            if (qSpec.getMode().equals(query.getMode())) {
                for (int i=0; i<query.getMode().length(); i++) {
                    if (query.getMode().charAt(i) == '+' && qSpec.getSignature().charAt(i) != query.getSignature().charAt(i))
                        return false;
                }
                return true;
            }
            //return false;
        }
        return false;
    }
    
    public String getSHA1(String repoDirPath) {
        return utils.getSHA1(utils.getFilePathInDirectory(scriptPath, repoDirPath));
    }
    
    public String getID() {
        return ID;
    }
    
}

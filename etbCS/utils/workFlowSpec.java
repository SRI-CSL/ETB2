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
        JSONArray queryListJSON = (JSONArray) workFlowSpecJSON.get("queries");
        for (int i = 0; i < queryListJSON.size(); i++) {
            querySpec qSpec = new querySpec((JSONObject) queryListJSON.get(i));
            this.queryList.put(qSpec.getID(), qSpec);
        }
        this.scriptPath = (String) workFlowSpecJSON.get("script");
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
        NewObj.put("queries", queryListJSON);
        NewObj.put("script", scriptPath);
        return NewObj;
    }
    
    public void print() {
        System.out.println("--> ID: " + ID);
        System.out.println("--> scriptPath: " + scriptPath);
        System.out.println("--> queryList: " + queryList.toString());
    }

    public boolean containsQuery(Expr query) {
        return (this.queryList.containsKey(query.getPredicate()) &&
                this.queryList.get(query.getPredicate()).equals(query));
    }

    public String getSHA1(String repoDirPath) {
        return utils.getSHA1(utils.getFilePathInDirectory(scriptPath, repoDirPath));
    }
    
    public String getID() {
        return ID;
    }
    
}

package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import etb.etbDL.utils.*;

public class workFlowSpec {

    String ID;
    String scriptPath; //TODO: hashing scriptfile
    Map<Integer, querySpec> queries = new HashMap();
    
    public workFlowSpec(String ID, Map<Integer, querySpec> queries, String scriptPath) {
        this.ID = ID;
        this.queries = queries;
        this.scriptPath = scriptPath;
    }

    public workFlowSpec(JSONObject workFlowSpecJSON) {
        ID = (String) workFlowSpecJSON.get("ID");
        JSONArray queriesJSON = (JSONArray) workFlowSpecJSON.get("queries");
        for (int i = 0; i < queriesJSON.size(); i++) {
            querySpec qSpec = new querySpec((JSONObject) queriesJSON.get(i));
            this.queries.put(qSpec.hashCode(), qSpec);
        }
        this.scriptPath = (String) workFlowSpecJSON.get("script");
    }
    
    public JSONObject toJSONObject() {
        JSONObject NewObj = new JSONObject();
        NewObj.put("ID", ID);
        JSONArray queriesJSON = new JSONArray();
        for (Integer queryHash : queries.keySet()) {
            queriesJSON.add(queries.get(queryHash).toJSONObj());
        }
        NewObj.put("queries", queriesJSON);
        NewObj.put("script", scriptPath);
        return NewObj;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==> [workflow : " + ID + "]");
        sb.append("\n--> scriptPath: " + scriptPath);
        sb.append("\n--> queries: " + queries.values());
        return sb.toString();
    }

    public String getScriptPath() {
        return this.scriptPath;
    }
        
    //TODO: needed?
    public boolean containsQuery(Expr query) {
        return (this.queries.containsKey(query.getPredicate()) &&
                this.queries.get(query.getPredicate()).equals(query));
    }

    public boolean containsQuery(Integer queryID) {
        return queries.containsKey(queryID);
    }

    //TODO: currently tracks any change to the datalog script
    public String getScriptID(String repoDirPath) {
        return utils.getSHA1(utils.getFilePathInDirectory(scriptPath, repoDirPath));
    }
    
    public String getID() {
        return ID;
    }
    
    Map<Integer, querySpec> getQueries() {
        return queries;
    }
}

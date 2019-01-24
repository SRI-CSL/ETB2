package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class serviceSpec {
    //String name;
    String signature;
    ArrayList<String> modes = new ArrayList();
    
    public serviceSpec(String signature, ArrayList<String> modes) {
        //this.name = name;
        this.signature = signature;
        this.modes = modes;
    }
    
    public serviceSpec(JSONObject serverSpecJSON) {
        
        //this.name = (String) serverSpecJSON.get("name");
        this.signature = (String) serverSpecJSON.get("signature");
        JSONArray modesJSON = (JSONArray) serverSpecJSON.get("modes");
        Iterator<String> modeIterator = modesJSON.iterator();
        while (modeIterator.hasNext()) {
            this.modes.add(modeIterator.next());
        }
    }
    
    public String getSignature() {
        return signature;
    }
    
    public ArrayList<String> getModes() {
        return modes;
    }
    
    public JSONObject toJSONObject(String ID) {
        
        JSONObject NewObj = new JSONObject();
        
        NewObj.put("ID", ID);
        NewObj.put("signature", signature);
        
        JSONArray modesJSON = new JSONArray();
        for (int i=0; i < modes.size(); i++) {
            modesJSON.add(modes.get(i));
        }
        NewObj.put("modes", modesJSON);
        
        return NewObj;
        
    }
    
    public void print(String indent, String name) {
        System.out.println(indent + "name : " + name);
        System.out.println(indent + "signature: " + signature);
        System.out.println(indent + "modes: " + modes.toString());
    }

    public void print() {
        System.out.println("--> signature: " + signature);
        System.out.println("--> modes: " + modes.toString());
    }

}



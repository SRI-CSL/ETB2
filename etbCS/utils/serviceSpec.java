package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class serviceSpec {

    //String name;
    ArrayList<String> signature = new ArrayList();
    ArrayList<String> modes = new ArrayList();
    
    public serviceSpec(ArrayList<String> signature, ArrayList<String> modes) {
        //this.name = name;
        this.signature = signature;
        this.modes = modes;
    }

    public serviceSpec(JSONObject serverSpecJSON) {
        
        //this.name = (String) serverSpecJSON.get("name");
        
        JSONArray signatureJSON = (JSONArray) serverSpecJSON.get("signature");
        Iterator<String> signIterator = signatureJSON.iterator();
        while (signIterator.hasNext()) {
            this.signature.add(signIterator.next());
        }
        
        JSONArray modesJSON = (JSONArray) serverSpecJSON.get("modes");
        Iterator<String> modeIterator = modesJSON.iterator();
        while (modeIterator.hasNext()) {
            this.modes.add(modeIterator.next());
        }
    }

    public ArrayList<String> getSignature() {
        return signature;
    }
    
    public ArrayList<String> getModes() {
        return modes;
    }
    
    public JSONObject toJSONObj(String name) {
        
        JSONObject NewObj = new JSONObject();
        NewObj.put("name", name);
        
        JSONArray signJSON = new JSONArray();
        for (int i=0; i < signature.size(); i++) {
            signJSON.add(signature.get(i));
        }
        NewObj.put("signature", signJSON);
        
        JSONArray modesJSON = new JSONArray();
        for (int i=0; i < modes.size(); i++) {
            modesJSON.add(modes.get(i));
        }
        NewObj.put("modes", modesJSON);
        
        return NewObj;
        
    }
    
    public void print(String indent, String name) {
        System.out.println(indent + "name : " + name);
        System.out.println(indent + "signature: " + signature.toString());
        System.out.println(indent + "modes: " + modes.toString());
    }
    
}


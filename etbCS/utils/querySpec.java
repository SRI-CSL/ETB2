package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class querySpec {

    String signature, mode;
    
    public querySpec(String signature, String mode) {
        //this.pred = pred;
        this.signature = signature;
        this.mode = mode;
    }

    public querySpec(JSONObject serverSpecJSON) {
        
        //this.pred = (String) serverSpecJSON.get("pred");
        this.signature = (String) serverSpecJSON.get("signature");
        this.mode = (String) serverSpecJSON.get("mode");
    }

    public JSONObject toJSONObj(String pred) {
        
        JSONObject NewObj = new JSONObject();
        
        NewObj.put("pred", pred);
        NewObj.put("signature", signature);
        NewObj.put("mode", mode);
        
        return NewObj;
        
    }
    
    public String getSignature() {
        return this.signature;
    }
    
    public String getMode() {
        return this.mode;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("< [");
        for( int j=0; j < signature.length(); j++) {
            switch(signature.charAt(j)){
                case '1':
                    sb.append("string ");
                    break;
                case '2':
                    sb.append("file ");
                    break;
                case '3':
                    sb.append("string_list ");
                    break;
                case '4':
                    sb.append("file_list ");
                    break;
                default:
                    sb.append("(*unk*) ");
                    System.out.println("unknown type class '" + signature.charAt(j) + "'");
            }
        }
        sb.append("], " + mode + ">");
        return sb.toString();

    }
    
    public boolean isMatching(querySpec qSpec) {
        if (this.signature.equals(qSpec.getSignature()) && this.mode.equals(qSpec.getMode()))
            return true;
        return false;
    }
}


package etb.etbCS.utils;

import java.lang.ClassCastException;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import etb.etbDL.utils.Expr;


public class querySpec {

    String ID, mode; //TODO: more than one mode?
    signatureSpec signature;
    boolean valid = false;;
    
    
    public querySpec(String ID, signatureSpec signature, String mode) {
        this.ID = ID;
        this.signature = signature;
        this.mode = mode;
    }
    
    public querySpec(signatureSpec signature, String mode) {
        this.signature = signature;
        this.mode = mode;
    }
    
    public querySpec(JSONObject querySpecJSON) {
        this.ID = (String) querySpecJSON.get("ID");
        this.signature = new signatureSpec(querySpecJSON.get("signature"));
        this.mode = (String) querySpecJSON.get("mode");
        this.valid = signature.isValid();
    }
    
    public JSONObject toJSONObj(String ID) {
        JSONObject NewObj = new JSONObject();
        NewObj.put("ID", ID);
        NewObj.put("signature", "" + signature);
        NewObj.put("mode", mode);
        return NewObj;
    }
    
    public signatureSpec getSignature() {
        return this.signature;
    }
    
    public String getMode() {
        return this.mode;
    }
    
    public String getID() {
        return this.ID;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("<" + ID);
        sb.append(", [" + String.join(", ", signature.toList()) + "], " + mode + ">");
        return sb.toString();
    }

    public boolean isMatching(querySpec qSpec) {
        if (this.signature.equals(qSpec.getSignature()) && this.mode.equals(qSpec.getMode()))
            return true;
        return false;
    }
    
    public boolean equals(querySpec qSpec) {
        return (this.ID.equals(qSpec.getID()) &&
                this.signature.equals(qSpec.getSignature()) &&
                this.mode.equals(qSpec.getMode()));
    }
    
    public boolean equals2(Expr qSpec) {
        
        System.out.println("=> this.signature: " + this.signature);
        System.out.println("=> qSpec.getSignature(): " + qSpec.getSignature());
        
        
        
        return (this.ID.equals(qSpec.getPredicate()) &&
                this.signature.equals(qSpec.getSignature()) &&
                this.mode.equals(qSpec.getMode()));
    }
    
    public boolean equals(Expr qSpec) {
        
        return (this.ID.equals(qSpec.getPredicate()) &&
                this.signature.equals(qSpec.getSignature(), qSpec.getMode()) &&
                this.mode.equals(qSpec.getMode()));
    }
    
    
    public boolean isValid() {
        return valid;
    }
}


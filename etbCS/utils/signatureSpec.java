package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class signatureSpec {

    List<String> typesTuple = new ArrayList();
    String typesStr;
    boolean valid = false;

    public signatureSpec(Object signatureJSON) {
        try {//signature from new
            initSignature((JSONArray) signatureJSON);
        }
        catch (ClassCastException e) {
            try {//signature from existing
                initSignature((String) signatureJSON);
            }
            catch (ClassCastException e2) {
                e2.printStackTrace();
            }
        }
    }
    
    private void initSignature(JSONArray signatureJSON) {
        Iterator<String> typesIter = signatureJSON.iterator();
        while (typesIter.hasNext()) {
            typesTuple.add(((String) typesIter.next()).trim());
        }
        Map<String, String> typesEncodeMap = new HashMap<String, String>();
        typesEncodeMap.put("string", "1");
        typesEncodeMap.put("file", "2");
        typesEncodeMap.put("string_list", "3");
        typesEncodeMap.put("file_list", "4");
        
        if (typesEncodeMap.keySet().containsAll(typesTuple)) {
            this.typesStr = String.join("", Arrays.asList(typesTuple.stream().map(inType -> typesEncodeMap.get(inType)).toArray(String[]::new)));
            this.valid = true;
        }
    }

    private void initSignature(String signatureJSON) {
        this.typesStr = signatureJSON;
        Map<String, String> toTypesMap = new HashMap<String, String>();
        toTypesMap.put("1", "string");
        toTypesMap.put("2", "file");
        toTypesMap.put("3", "string_list");
        toTypesMap.put("4", "file_list");
        List<String> xx = new ArrayList(Arrays.asList(typesStr.split("")));
        this.typesTuple = Arrays.asList(xx.stream().map(typeCode -> toTypesMap.get(typeCode)).toArray(String[]::new));
        this.valid = true;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String toString() {
        return typesStr;
    }

    public List<String> toList() {
        return typesTuple;
    }
    
    public boolean equals(signatureSpec signature) {
        return (this.typesStr.equals(signature.toString()));
    }
    
    public boolean equals(String signature, String mode) {
        List<String> modesList = Arrays.asList(mode.split("\\s*"));
        for (int i = 0; i< modesList.size(); i++) {
            if (modesList.get(i).equals("+") && typesStr.charAt(i) != signature.charAt(i))
                return false;
        }
        return true;
    }
    
}


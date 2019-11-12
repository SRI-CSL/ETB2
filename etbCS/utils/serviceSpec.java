/*
 This class specifies an ETB service.
 An ETB service is specified by:
    (1) a unique identifier ID,
    (2) a signature, which is a tuple of ETB types
    (3) a list of modes
 */

package etb.etbCS.utils;

import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.io.FileWriter;
import java.io.IOException;
import etb.etbDL.utils.utils;

public class serviceSpec {
    String ID;
    String signature;
    List<String> modes = new ArrayList();
    String version = "1.0";
    
    public serviceSpec(String ID, String signature, List<String> modes) {
        this.ID = ID;
        this.signature = signature;
        this.modes = modes;
    }
    
    public serviceSpec(String ID, String signature, List<String> modes, String version) {
        this.ID = ID;
        this.signature = signature;
        this.modes = modes;
        this.version = version;
    }
    
    public serviceSpec(JSONObject serviceSpecJSON) {
        ID = (String) serviceSpecJSON.get("ID");
        signature = (String) serviceSpecJSON.get("signature");
        modes = Arrays.asList(((String) serviceSpecJSON.get("modes")).split("\\s+"));
        version = (String) serviceSpecJSON.get("version");
    }
    
    public String getSignature() {
        return signature;
    }
    
    public String getID() {
        return ID;
    }
    
    public List<String> getModes() {
        return modes;
    }

    public JSONObject toJSONObject() {
        JSONObject NewObj = new JSONObject();
        NewObj.put("ID", ID);
        NewObj.put("signature", signature);
        NewObj.put("modes", String.join(" ", modes));
        NewObj.put("version", version);
        return NewObj;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isUpdated(String version) {
        return (!this.version.equals(version));
    }
    
    public void generateWrappers() {
        //preparing content of ETBWRP file
        String initBody = "\n\t\tserviceName = \"" + ID + "\";";
        //preparing runBody for user WRP file
        String runBody = "\n\t\tif (mode.equals(\"" + modes.get(0)  + "\")) {\n\t\t\t//do something\n\t\t}";
        for (int i=1; i < modes.size(); i++) {
            runBody += "\n\t\telse if (mode.equals(\"" + modes.get(i)  + "\")) {\n\t\t\t//do something\n\t\t}";
        }
        
        runBody += "\n\t\telse {\n\t\t\tSystem.out.println(\"unrecognized mode for " + ID + "\");\n\t\t}";
        runBody = "\n\n\t@Override\n\tpublic void run(){" + runBody + "\n\t}";
        
        initBody += "\n\t\tsignatureStr = \"" + signature + "\";" + "\n\t\tmodesStr = \"" + String.join(" ", modes) + "\";";
        
        String inVarsDecl = "\n\t//input variables declaration", outVarsDecl = "\n\t//output variables declaration", retMethods = "";
        String listRetMethods = "\n\n\t@Override\n\tpublic ArrayList<String> getListOutput(int pos) {";
        String strRetMethods = "\n\n\t@Override\n\tpublic String getStrOutput(int pos) {";
        String inVarsInst = "\n\t\t//input variables instantiation";
        String outVarsInst = "\n\t\t//output variables default instantiation";
        
        for (int i=1; i <= signature.length(); i++) {
            if (signature.charAt(i-1) == '2' || signature.charAt(i-1) == '4') {
                inVarsDecl += "\n\tprotected ArrayList<String> in" + i + ";";
                outVarsDecl += "\n\tprotected ArrayList<String> out" + i + ";";
                inVarsInst += "\n\t\tin" + i + " = datalog2JavaListConst(mode, argList, " + i + ");";
                listRetMethods += "\n\t\tif (pos == " + i + ") {\n\t\t\treturn this.out" + i + ";\n\t\t}";
            }
            else {
                inVarsDecl += "\n\tprotected String in" + i + ";";
                outVarsDecl += "\n\tprotected String out" + i + ";";
                inVarsInst += "\n\t\tin" + i + " = datalog2JavaStrConst(mode, argList, " + i + ");";
                strRetMethods += "\n\t\tif (pos == " + i + ") {\n\t\t\treturn this.out" + i + ";\n\t\t}";
            }
            outVarsInst += "\n\t\tout" + i + " = in" + i + ";";
        }
        
        initBody = "\n\n\t@Override\n\tpublic void initialise(){" + initBody + inVarsInst + outVarsInst + "\n\t}";
        listRetMethods += "\n\t\treturn null;\n\t}";
        strRetMethods += "\n\t\treturn null;\n\t}";
        
        String etbWrapperFilePath = System.getProperty("user.dir") + "/wrappers/" + ID + "ETBWRP.java";
        String etbWrapperClass = "/*\n an auto-generated ETB wrapper template for the service '" + ID + "'\n*/";
        etbWrapperClass += "\n\npackage etb.wrappers;\n\nimport java.util.ArrayList;\nimport etb.wrappers.genericWRP;";
        etbWrapperClass += "\n\npublic abstract class " + ID + "ETBWRP extends genericWRP {";
        etbWrapperClass += inVarsDecl + outVarsDecl + initBody + listRetMethods + strRetMethods + "\n}";
        
        String userWrapperFilePath = System.getProperty("user.dir") + "/wrappers/" + ID + "WRP.java";
        String userWrapperClass = "/*\n an auto-generated user wrapper template for the service '" + ID + "'\n*/";
        userWrapperClass += "\n\npackage etb.wrappers;";
        userWrapperClass += "\n\npublic class " + ID + "WRP extends " + ID + "ETBWRP {";
        userWrapperClass += runBody + "\n}";
        
        try {
            FileWriter etbWrapperFW = new FileWriter(etbWrapperFilePath);
            etbWrapperFW.write(etbWrapperClass);
            etbWrapperFW.flush();
            etbWrapperFW.close();
            
            FileWriter userWrapperFW = new FileWriter(userWrapperFilePath);
            userWrapperFW.write(userWrapperClass);
            userWrapperFW.flush();
            userWrapperFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  wrappers/" + ID + "WRP.java wrappers/" + ID + "ETBWRP.java");

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==> [service : " + ID + "]");
        sb.append("\n--> signature: " + signature);
        sb.append("\n--> modes: " + modes.toString());
        return sb.toString();
    }

}



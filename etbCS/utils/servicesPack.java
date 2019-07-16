package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbDL.services.*;
import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class servicesPack {
    
    Map<String, serviceSpec> services = new HashMap();

    public servicesPack() {
        services = new HashMap();
    }
    
    public servicesPack(JSONArray servicesJSON) {
        if (servicesJSON == null) {
            System.out.println("\u001B[31m(null services)\u001B[30m");
        }
        Iterator<JSONObject> serviceIter = servicesJSON.iterator();
        while (serviceIter.hasNext()) {
            JSONObject serviceSpecObj = (JSONObject) serviceIter.next();
            this.services.put((String) serviceSpecObj.get("ID"), new serviceSpec(serviceSpecObj));
        }
    }
    
    public JSONArray toJSONObject() {
        JSONArray servicesJSON = new JSONArray();
        for (String serviceID : services.keySet()) {
            servicesJSON.add(services.get(serviceID).toJSONObject(serviceID));
        }
        return servicesJSON;
    }
    
    public void clean() {
        services.clear();
    }
    
    public boolean containsService(String serviceID) {
        return services.containsKey(serviceID);
    }
    
    public void add(String specFilePath) {
        
        try {
            JSONParser parser = new JSONParser();
            Object serviceSpecObj = parser.parse(new FileReader(specFilePath));
            JSONObject serviceSpecJSON = (JSONObject) serviceSpecObj;
            
            String ID = (String) serviceSpecJSON.get("ID");
            if ((ID = ID.trim()) == null) {
                System.out.println("=> no service ID given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            else if (services.containsKey(ID)) {
                System.out.println("=> a service with ID '" + ID + "' exists \u001B[31m(operation not successful)\u001B[30m");
                return;
             }
            
            String signature, signature0 = (String) serviceSpecJSON.get("signature");
            if (signature0 == null) {
                System.out.println("=> no service signature given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            else if ((signature = encodeSignature(signature0)) == null) {
                System.out.println("=> invalid service signature given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            
            String modesStr = (String) serviceSpecJSON.get("modes");
            if (modesStr == null) {
                System.out.println("=> no service modes given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            List<String> modes = Arrays.asList(modesStr.split("\\s+"));
            
            serviceSpec toAdd = new serviceSpec(ID, signature, modes);
            toAdd.generateWrappers();
            services.put(ID, toAdd);
            
            updateExternPredBridgeFile();
            
            System.out.println("=> service added successfully");
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
    }
    
    private String encodeSignature(String signature){
        List<String> signList = Arrays.asList(signature.split("\\s+"));
        Map<String, String> typesEncodeMap = new HashMap<String, String>();
        typesEncodeMap.put("string", "1");
        typesEncodeMap.put("file", "2");
        typesEncodeMap.put("string_list", "3");
        typesEncodeMap.put("file_list", "4");
        
        if (typesEncodeMap.keySet().containsAll(signList)) {
            return String.join("", Arrays.asList(signList.stream().map(inType -> typesEncodeMap.get(inType)).toArray(String[]::new)));
        }
        return null;
    }

    public void remove(String ID) {
        if (!services.keySet().contains(ID)) {
            System.out.println("=> a service with the name '" + ID + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
            return;
        }
        services.remove(ID);
        removeWrappers(ID);
        System.out.println("=> service removed successfully");
        updateExternPredBridgeFile();
    }

    public String getNames() {
        if (services.isEmpty()) {
            return null;
        }
        /*
        Iterator<String> nameIter = services.keySet().iterator();
        String namesStr = nameIter.next();
        while (nameIter.hasNext()) {
            namesStr += " " + nameIter.next();
        }*/
        return String.join(" ", services.keySet());
    }
    
    public void print() {
        System.out.println("==> total number of local services: " + services.size());
        int count = 1;
        for (String serviceID : services.keySet()) {
            System.out.println("==> [service " + count++ + "] ID : " + serviceID);
            services.get(serviceID).print();
        }
    }
    
    public serviceSpec get(String serviceID) {
        return services.get(serviceID);
    }
    
    private void updateExternPredBridgeFile() {
        
        if (services.size() == 0) {
            generateDefaultBridgeFile();
            return;
        }
        
        String filePath = System.getProperty("user.dir") + "/etbDL/services/externPred2ServiceInstance.java";
        try {
            FileWriter NewFileFW = new FileWriter(filePath);
            NewFileFW.write("/*\n implements a mechanism for translating external predicates to corresponding tool invocation (auto-generated code)\n*/");
            NewFileFW.write("\n\npackage etb.etbDL.services;");
            NewFileFW.write("\nimport java.util.ArrayList;");
            NewFileFW.write("\nimport etb.wrappers.*;");
            NewFileFW.write("\n\npublic class externPred2ServiceInstance extends externPred2Service {");
            NewFileFW.write("\n\t@Override");
            NewFileFW.write("\n\tpublic genericWRP getGroundParams(String toolName, ArrayList<String> args, String mode) {");
            NewFileFW.write("\n\t\tgenericWRP genWRP = null;");
            
            Iterator<String> it = services.keySet().iterator();
            String toolName = it.next();
            NewFileFW.write("\n\t\tif(toolName.equals(\"" + toolName + "\")){");
            NewFileFW.write("\n\t\t\tgenWRP = new " + toolName + "WRP();");
            NewFileFW.write("\n\t\t}");
            
            while(it.hasNext()) {
                toolName = it.next();
                NewFileFW.write("\n\t\telse if(toolName.equals(\"" + toolName + "\")){");
                NewFileFW.write("\n\t\t\tgenWRP = new " + toolName + "WRP();");
                NewFileFW.write("\n\t\t}");
            }
            
            NewFileFW.write("\n\t\telse{");
            NewFileFW.write("\n\t\t\tSystem.out.println(\"no external service found with name: \" + toolName);");
            NewFileFW.write("\n\t\t}");
            NewFileFW.write("\n\t\treturn genWRP;");
            NewFileFW.write("\n\t}");
            NewFileFW.write("\n}");
            NewFileFW.flush();
            NewFileFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  etbDL/services/externPred2ServiceInstance.java");
    }
    
    public void generateDefaultBridgeFile() {
        String filePath = System.getProperty("user.dir") + "/etbDL/services/externPred2ServiceInstance.java";
        try {
            FileWriter NewFileFW = new FileWriter(filePath);
            NewFileFW.write("/*\n implements a mechanism for translating external predicates to corresponding tool invocation (auto-generated code)\n*/");
            NewFileFW.write("\n\npackage etb.etbDL.services;");
            NewFileFW.write("\nimport java.util.ArrayList;");
            NewFileFW.write("\n\npublic class externPred2ServiceInstance extends externPred2Service {");
            NewFileFW.write("\n\t@Override");
            NewFileFW.write("\n\tpublic genericWRP getGroundParams(String toolName, ArrayList<String> args, String mode) {");
            NewFileFW.write("\n\n\t\treturn null;");
            NewFileFW.write("\n\t}");
            NewFileFW.write("\n}");
            NewFileFW.flush();
            NewFileFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  etbDL/services/externPred2ServiceInstance.java");
    }

    private void removeWrappers(String serviceID) {
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/wrappers/ && rm -f " + serviceID + "WRP.java " + serviceID + "ETBWRP.java");
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/etb/wrappers/ && rm -f " + serviceID + "WRP.class " + serviceID + "ETBWRP.class");
    }

}


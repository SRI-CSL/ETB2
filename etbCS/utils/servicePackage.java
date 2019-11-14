package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class servicePackage {
    
    Map<String, serviceSpec> services = new HashMap();

    public servicePackage() {}

    public servicePackage(JSONArray servicesJSON) {
        Iterator<JSONObject> serviceIter = servicesJSON.iterator();
        while (serviceIter.hasNext()) {
            serviceSpec service = new serviceSpec((JSONObject) serviceIter.next());
            this.services.put(service.getID(), service);
        }
    }
    
    public JSONArray toJSONObject() {
        JSONArray servicesJSON = new JSONArray();
        for (String serviceID : services.keySet()) {
            servicesJSON.add(services.get(serviceID).toJSONObject());
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
            
            //updateExternPredBridgeFile();
            
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
        //updateExternPredBridgeFile();
    }

    public String getNames() {
        if (services.isEmpty()) {
            return null;
        }
        return String.join(" ", services.keySet());
    }
    
    public Map<String, serviceSpec> getServices() {
        return services;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==> total number of local services: " + services.size());
        services.keySet().stream().forEach(serviceID -> sb.append(services.get(serviceID)));
        return sb.toString();
    }
    
    public serviceSpec get(String serviceID) {
        return services.get(serviceID);
    }
    
    private void removeWrappers(String serviceID) {
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/wrappers/ && rm -f " + serviceID + "WRP.java " + serviceID + "ETBWRP.java");
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/etb/wrappers/ && rm -f " + serviceID + "WRP.class " + serviceID + "ETBWRP.class");
    }

    public List<String> getUpdated(Map<String, serviceSpec> inServices) {
        return Arrays.asList(inServices.keySet().stream().filter(inServiceID -> !(inServices.get(inServiceID).getVersion().equals(services.get(inServiceID).getVersion()))).toArray(String[]::new));
    }
    
    public void update(String serviceID, String version) {
        if (!services.keySet().contains(serviceID)) {
            System.out.println("=> a service with the name '" + serviceID + "' does not exist \u001B[31m(update not successful)\u001B[30m");
        }
        else {
            services.get(serviceID).setVersion(version);
        }
    }
}


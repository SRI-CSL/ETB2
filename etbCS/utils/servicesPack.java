package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbCS.etcServer;
import etb.etbDL.services.*;

import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class servicesPack {
    
    Map<String, serviceSpec> services = new HashMap();

    public servicesPack() {
        services = new HashMap();
    }
    
    public servicesPack(JSONArray servicesJSON) {
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
    
    private boolean validSignature(ArrayList<String> signature) {
        ArrayList<String> signSpecs = new ArrayList();
        signSpecs.add("string");
        signSpecs.add("file");
        signSpecs.add("string_list");
        signSpecs.add("file_list");
        
        for (int i=0; i < signature.size(); i++) {
            if (!signSpecs.contains(signature.get(i))) {
                System.out.println("--> not a valid signature entry '" + signature.get(i) + "' \u001B[31m(signature not accepted)\u001B[30m");
                return false;
            }
        }
        return true;
    }

    private String toSignatureCode(ArrayList<String> signature) {
        String encodSignStr = "";
        for (int i=0; i < signature.size(); i++) {
            
            switch(signature.get(i)){
                case "string":
                    encodSignStr += "1";
                    break;
                case "file":
                    encodSignStr += "2";
                    break;
                case "string_list":
                    encodSignStr += "3";
                    break;
                case "file_list":
                    encodSignStr += "4";
                    break;
                default:
                    System.out.println("unknown type '" + signature.get(i) + "'");
                    return null;
            }
        }
        return encodSignStr;
    }

    public void add() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> service name : ");
            String serviceName = in.nextLine();
            
            if (services.containsKey(serviceName)) {
                System.out.println("=> a tool with the name '" + serviceName + "' exists \u001B[31m(operation not successful)\u001B[30m");
                System.out.print("=> add more tools? [y] to add more : ");
                if (!in.nextLine().equals("y"))
                    break;
                continue;
            }
            
            String signatureStr;
            ArrayList<String> signature = new ArrayList();
            do {
                System.out.print("--> service signature : ");
                signatureStr = in.nextLine();
                signature = new ArrayList(Arrays.asList(signatureStr.split(" "))); //TODO: better signature specification
            } while (!validSignature(signature));
            
            System.out.print("--> number of invocation modes : ");
            String modesCount = in.nextLine();
            System.out.print("--> set of modes : ");
            String toolExecModes = in.nextLine();
            
            ArrayList<String> modeSet = glueCodeAutoGen.addToolWrapper(serviceName, signature, Integer.parseInt(modesCount), toolExecModes);
            services.put(serviceName, new serviceSpec(toSignatureCode(signature), modeSet));
            
            System.out.println("=> tool added successfully");
            System.out.print("=> add more tools? [y] to add more : ");
            if (!in.nextLine().equals("y"))
                break;
        }

        ArrayList<String> etcServiceNames = new ArrayList();
        etcServiceNames.addAll(services.keySet());
        glueCodeAutoGen.updateExternPredBridgeFile(etcServiceNames);
        //save();
    }

    public void remove() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> service name : ");
            String toolName = in.nextLine();
            
            //checking if workflow already exists with the same name
            if (!services.keySet().contains(toolName)) {
                System.out.println("=> a service with the name '" + toolName + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
                System.out.print("=> remove more tools? [y] to remove more : ");
                if (!in.nextLine().equals("y"))
                    break;
                continue;
            }
            services.remove(toolName);
            glueCodeAutoGen.removeToolWrapper(toolName);
            System.out.println("=> tool removed successfully");
            System.out.print("=> remove more tools? [y] to remove more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        ArrayList<String> etcServiceNames = new ArrayList();
        etcServiceNames.addAll(services.keySet());
        glueCodeAutoGen.updateExternPredBridgeFile(etcServiceNames);
        //save();
    }

    public String getNames() {
        if (services.isEmpty()) {
            return null;
        }
        Iterator<String> nameIter = services.keySet().iterator();
        String namesStr = nameIter.next();
        while (nameIter.hasNext()) {
            namesStr += " " + nameIter.next();
        }
        return namesStr;
    }
    
    public void print() {
        if (services.size() == 0) {
            System.out.println("==> no services found");
        }
        for (String serviceID : services.keySet()) {
            System.out.println("==> ID : " + serviceID);
            services.get(serviceID).print();
        }
    }
    
    
}


package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
//import etb.etbCS.etcServer;
import etb.etbCS.clientMode;

import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class serversPackage {
    
    Map<String, serverSpec> servers = new HashMap();
    
    public serversPackage() {
        servers = new HashMap();
    }
    
    public serversPackage(JSONArray serversJSON) {
        //this.repoDirPath = repoDirPath;
        Iterator<JSONObject> serverIter = serversJSON.iterator();
        while (serverIter.hasNext()) {
            //JSONObject serverSpecObj = (JSONObject) serverIter.next();
            serverSpec server = new serverSpec((JSONObject) serverIter.next());
            this.servers.put(server.getID(), server);
        }
    }
    
    public JSONArray toJSONObject() {
        JSONArray serversJSON = new JSONArray();
        for (String serverID : servers.keySet()) {
            serversJSON.add(servers.get(serverID).toJSONObject());
        }
        return serversJSON;
    }
    
    public void add() {
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> server address : ");
            String serverAddress = in.nextLine();
            System.out.print("--> server port : ");
            String serverPort = in.nextLine();
            //communicating with the server and registering its services *** act as a client
            clientMode cm = new clientMode(serverAddress, Integer.valueOf(serverPort));
            if (cm.isConnected()) {
                String remoteServicesStr = cm.newServicesRegistration();
                if (servers.containsKey(serverAddress+serverPort)) {
                    System.out.println("=> server already exists \u001B[31m(updating remote services)\u001B[30m");
                    this.servers.replace(serverAddress+serverPort, new serverSpec(serverAddress, Integer.valueOf(serverPort), new ArrayList(Arrays.asList(remoteServicesStr.split(" ")))));
                }
                else {
                    this.servers.put(serverAddress+serverPort, new serverSpec(serverAddress, Integer.valueOf(serverPort), new ArrayList(Arrays.asList(remoteServicesStr.split(" ")))));
                    System.out.println("=> server added successfully");
                }
            }
            else {
                System.out.println("=> server could not be added \u001B[31m(operation not successful)\u001B[30m");
            }
            
            System.out.print("=> add more servers? [y] to add more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
    }
    
    public void remove() {
        
        //print();
        System.out.print(this);
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> enter serverID : ");
            String serverID = in.nextLine();
            
            if (servers.containsKey(serverID)) {
                servers.remove(serverID);
                System.out.println("=> server removed successfully");
            }
            else {
                System.out.println("=> invalid serverID '" + serverID + "' \u001B[31m(removal not successful)\u001B[30m");
                System.out.print("=> remove more servers? [y] to remove more : ");
            }
            System.out.print("=> remove more servers? [y] to remove more : ");
            if (!in.nextLine().equals("y")) break;
        }
    }
    
    public void clean() {
        servers.clear();
    }

    public  Map<String, serverSpec> getServers() {
        return servers;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n==> number of servers: " + servers.size());
        servers.keySet().stream().forEach(serverID -> sb.append(servers.get(serverID)));
        return sb.toString();
    }

}


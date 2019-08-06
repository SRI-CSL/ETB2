package etb.etbCS.utils;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class serverSpec {

    String address;
    int port;
    String ID;
    ArrayList<String> services = new ArrayList();
    
    public serverSpec(String address, int port, ArrayList<String> services) {
        this.ID = address + port;
        this.address = address;
        this.port = port;
        this.services = services;
    }

    public serverSpec(JSONObject serverSpecJSON) {
        
        this.address = (String) serverSpecJSON.get("address");
        this.port = Integer.valueOf(String.valueOf(serverSpecJSON.get("port")));
        this.ID = address + port;
        
        JSONArray lowerServersJSON = (JSONArray) serverSpecJSON.get("services");
        Iterator<String> iterator = lowerServersJSON.iterator();
        while (iterator.hasNext()) {
            this.services.add(iterator.next());
        }
        
    }

    public String getID() {
        return ID;
    }

    public String getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public ArrayList getServices(){
        return services;
    }
    
    public JSONObject toJSONObject() {
        
        JSONObject NewObj = new JSONObject();
        NewObj.put("address", address);
        NewObj.put("port", port);
        
        JSONArray servicesJSON = new JSONArray();
        Iterator<String> it = services.iterator();
        while( it.hasNext()) {
            servicesJSON.add(it.next());
        }
        NewObj.put("services", servicesJSON);
        return NewObj;
        
    }
        
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==> [hostIP : " + address + " port : " + port + "]");
        sb.append("\n--> services: " + services.toString());
        if (isRunning()) {
            sb.append("\n--> status:\u001B[32m running\u001B[30m");
        }
        else {
            sb.append("\n--> status:\u001B[31m down\u001B[30m");
        }
        return sb.toString();
    }

    public boolean isRunning() {
        Socket serverSocket = new Socket();
        try {
            serverSocket.connect(new InetSocketAddress(address, port), 2000);
            serverSocket.setSoTimeout(2000);
            if (serverSocket.isConnected()) {
                OutputStream outStr = serverSocket.getOutputStream();
                DataOutputStream toServerData = new DataOutputStream(outStr);
                toServerData.writeUTF("statusCheck");
                return true;
            }
        } catch (IOException e) {
            System.err.println("-> no I/O for the connection to " + address + " at port " + port);
        }
        return false;
    }
    
}


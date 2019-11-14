package etb.etbCS;

import java.util.*;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.net.*;
import java.io.*;

import etb.etbCS.utils.*;
import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbDL.statements.etbDLParser;
import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class etbNode {
    String hostIP = "127.0.0.1";
    int port = 0;
    
    String repoDirPath = System.getProperty("user.dir"); //gitRepo
    
    serversPackage serversPack = new serversPackage();
    servicePackage servicePack = new servicePackage();
    workFlowsPackage workflowsPack = new workFlowsPackage();
    claimsPack claims = new claimsPack();
    
    String paramsFilePath = System.getProperty("user.dir") + "/params.json";
    
    //constructs an ETB object on the given IP address
    public etbNode() {
        try {
            this.hostIP = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public String getRepoDirPath() {
        return repoDirPath;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public serversPackage getServersPack() {
        return serversPack;
    }
    
    public servicePackage getServicePack() {
        return servicePack;
    }
    
    //sets up a node in a given location -- interactive
    private void initialise() {
        File paramsFile = new File(paramsFilePath);
        if (paramsFile.exists()){
            System.out.println("=> \u001B[31m[error]\u001B[30m an ETB node already initialised at this location (use -h to see more options)");
        }
        else {
            while (true) {
                System.out.print("-> provide port : ");
                Scanner in = new Scanner(System.in);
                try {
                    this.port = Integer.valueOf(in.nextLine());
                    break;
                }
                catch (NumberFormatException e) {
                    System.out.println("\u001B[31m[error]\u001B[30m non-numeric port value not allowed");
                }
            }
            while (true) {
                System.out.print("-> provide git repo : ");
                Scanner in = new Scanner(System.in);
                this.repoDirPath = in.nextLine();
                File repoDir = new File(repoDirPath);
                if (repoDir.exists() && repoDir.isDirectory()){
                    try {
                        File repoDirCan = new File(repoDir.getCanonicalPath());
                        this.repoDirPath = repoDirCan.getAbsolutePath();
                        break;
                    }
                    catch (IOException e) {
                        System.out.println("\u001B[31m[error]\u001B[30m canonical path for file not found");
                        System.out.println(e.getMessage());
                    }
                }
                else {
                    System.out.println("\u001B[31m[error]\u001B[30m please provide a valid path");
                }
            }
            save();
            System.out.println("=> ETB node initialised (use -h to see more options to update the node)");
        }
    }

    //sets up a node in a given location -- with a config file
    public void initialise(String initFilePath) {
        File paramsFile = new File(paramsFilePath);
        if (paramsFile.exists()){
            System.out.println("=> \u001B[31m[error]\u001B[30m an ETB node already initialised at this location (use -h to see more options)");
            return;
        }
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject initSpecJSON = (JSONObject) parser.parse(new FileReader(initFilePath));
            String port0 = (String) initSpecJSON.get("port");
            try {
                this.port = Integer.valueOf(port0.trim());
            }
            catch (NumberFormatException e) {
                System.out.println("=> no port or non-numeric port is given \u001B[31m(operation not successful)\u001B[30m");
            }
            
            String repoDirPath0 = (String) initSpecJSON.get("repoDirPath");
            if (repoDirPath0 == null) {
                System.out.println("=> no valid repository is given \u001B[31m(operation not successful)\u001B[30m");
                return;
            }
            this.repoDirPath = repoDirPath0.trim();
            File repoDir = new File(repoDirPath);
            if (repoDir.exists() && repoDir.isDirectory()){
                try {
                    File repoDirCan = new File(repoDir.getCanonicalPath());
                    this.repoDirPath = repoDirCan.getAbsolutePath();
                }
                catch (IOException e) {
                    System.out.println("\u001B[31m[error]\u001B[30m please provide a valid path");
                    System.out.println(e.getMessage());
                }
            }
            else {
                System.out.println("\u001B[31m[error]\u001B[30m please provide a valid path");
            }
            
            save();
            System.out.println("=> ETB node initialised (use -h to see more options to update the node)");

            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
    }
    
    //sets port number and repoDir path for the node
    private void instantiate() {
        try {
            JSONParser parser = new JSONParser();
            JSONObject nodeParamsJSONObj = (JSONObject) parser.parse(new FileReader(this.paramsFilePath));
            this.port = Integer.valueOf(nodeParamsJSONObj.get("port").toString());
            this.repoDirPath = (String) nodeParamsJSONObj.get("repoDirPath");
        } catch (FileNotFoundException e) {
        	System.out.println("\u001B[31m[error]\u001B[30m no ETB node at this location (use -init to initialise an ETB node)");
        	//System.out.println("\u001B[31m[paramsFilePath]\u001B[30m : " + paramsFilePath);
            System.exit(0);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.out.println("\u001B[31m[error]\u001B[30m init file can not be read (use -uninit to re-initialise an ETB node)");
            System.exit(0);
        }
    }
    
    //sets all components, including the basic 4 components, of an ETB node
    private void populate() {
        try {
            JSONParser parser = new JSONParser();
            JSONObject nodeParamsJSONObj = (JSONObject) parser.parse(new FileReader(this.paramsFilePath));
            
            this.port = Integer.valueOf(nodeParamsJSONObj.get("port").toString());
            this.repoDirPath = (String) nodeParamsJSONObj.get("repoDirPath");
            
            this.servicePack = new servicePackage((JSONArray) nodeParamsJSONObj.get("servicePack"));
            this.serversPack = new serversPackage((JSONArray) nodeParamsJSONObj.get("servers"));
            this.workflowsPack = new workFlowsPackage(repoDirPath, (JSONArray) nodeParamsJSONObj.get("workflows"));
            this. claims = new claimsPack((JSONArray) nodeParamsJSONObj.get("claims"));

        } catch (FileNotFoundException e) {
            System.out.println("\u001B[31m[error]\u001B[30mno ETB node at this location (use -init to initialise an ETB node)");
            System.out.println("\u001B[31m[paramsFilePath]\u001B[30m : " + paramsFilePath);
            System.exit(0);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.out.println("\u001B[31m[error]\u001B[30m init file can not be read (use -uninit to re-initialise an ETB node)");
            System.exit(0);
        }
    }

    //cleans wrapper files
    private void clean() {
        File wrappersSrc = new File("wrappers");
        File wrappersBin = new File("etb/wrappers");
        try {
            FileUtils.cleanDirectory(wrappersSrc);
            FileUtils.cleanDirectory(wrappersBin);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("\u001B[31m[warning]\u001B[30m problem while cleaning wrapper files");
        }
    }
    
    //cleans init file and wrapper files
    private void cleanAll() {
        clean();
        File initFile = new File("params.json");
        try {
            FileUtils.forceDelete(initFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("\u001B[31m[warning]\u001B[30m problem while deleting initialisation file");
        }
    }
    
    public void run(String args[]) {
        if (args.length == 0) {
            populate();
            serverMode SM = new serverMode();
            SM.run(this, repoDirPath, port);
        }
        else if (args.length == 1) {
            
            if (args[0].equals("-init")) {
                initialise();
            }
            else if (args[0].equals("-uninit")) {
                cleanAll();
                System.out.println("=> node uninitialised successfully");
                //utils.runCMD0("rm -f etb/wrappers/* wrappers/* " + paramsFilePath);
            }
            else if (args[0].equals("-clean")) {
                instantiate(); //leaving the 4 core ETB components empty
                clean();
                System.out.println("=> node cleaned successfully");
                //utils.runCMD0("rm -f etb/wrappers/* wrappers/*");
                save();
            }
            else if (args[0].equals("-help") || args[0].equals("-h")){
                help();
            }
            else {
                populate();
                if (args[0].equals("-node-info")){
                    System.out.println(this);
                }
                else if (args[0].equals("-claims-status")){
                    claims.checkStatus(servicePack, workflowsPack.getWorkflows(), repoDirPath);
                }
                else if (args[0].equals("-add-server")){
                    serversPack.add();
                    save();
                }
                else if (args[0].equals("-rm-server")){
                    serversPack.remove();
                    save();
                }
                else {
                    System.out.println("ERROR. Unknown option: " + args[0]);
                }
            }
        }
        
        else if (args.length == 2 && args[0].equals("-init")) {
                initialise(args[1]);
        }
        
        else if (args.length == 2) {
            populate();
            //TODO: giving query as an input (subsumed by claim addition?)
            if (args[0].equals("-query")){
                //directly processing a query
                List<String> serviceArgs = inQuery2Params(args[1]);
                String serviceName = serviceArgs.remove(0);
                serviceInvocation inv = new serviceInvocation(new Expr(serviceName, serviceArgs));
                inv.process(this);
            }
            //TODO: claims already from queries in the script?
            else if (args[0].equals("-script")){
                String scriptFile = args[1];
                File SourceFileObj = new File(args[1]);
                if (!SourceFileObj.exists()) {
                    System.out.println("error: problem with the script file!");
                }
                //etbDatalog dlPack = new etbDatalog();
                //dlPack.parseDatalogScript(scriptFile, repoDirPath);
                etbDatalog dlPack = etbDLParser.parseDatalogScript(scriptFile, repoDirPath);

                etbDatalogEngine dlEngine = new etbDatalogEngine();
                Collection<Map<String, String>> answers = dlEngine.run(this, dlPack);
                if (answers == null) {
                    System.out.println("=> no claim for the query (null binding found)");
                }
                else {
                    QueryOutput qo = new DefaultQueryOutput();
                    qo.writeResult2(answers);
                }
            }
            else if (args[0].equals("-set-port")){
                try {
                    this.port =  Integer.valueOf(args[1]);
                    save();
                }
                catch (NumberFormatException e) {
                    System.out.println("\u001B[31m[error]\u001B[30m non-numeric port value: " + args[1]);
                }
            }
            else if (args[0].equals("-set-repo")){
                setRepoDir(args[1]);
            }
            else if (args[0].equals("-add-claim")){
                claims.add(args[1], servicePack.getServices(), workflowsPack, repoDirPath, this);
                save();
            }
            else if (args[0].equals("-rm-claim")){
                claims.remove(Integer.parseInt(args[1]));
                save();
            }
            else if (args[0].equals("-add-service")){
                servicePack.add(args[1]);
                save();
            }
            else if (args[0].equals("-rm-service")){
                servicePack.remove(args[1]);
                save();
            }
            else if (args[0].equals("-add-workflow")){
                workflowsPack.add(args[1]);
                save();
            }
            else if (args[0].equals("-rm-workflow")){
                workflowsPack.remove(args[1]);
                save();
            }
            else if (args[0].equals("-update-claim")){
                claims.update(Integer.parseInt(args[1]), servicePack, this);
                save();
            }
            else {
                System.out.println("ERROR. Unknown option: " + args[0]);
            }
        }
        
        else if (args.length == 3) {
            if (args[0].equals("-update-service")){
                servicePack.update(args[1], args[2]);
                save();
            }
            else {
                System.out.println("ERROR. Unknown option: " + args[0]);
            }
        }

        else {
            System.out.println("ERROR. Incorrect Workflow argument(s) structure");
        }
    }
    
    private void setRepoDir(String inDirPath) {
        File repoDir = new File(inDirPath);
        if (repoDir.exists() && repoDir.isDirectory()){
            try {
                File repoDirCanonical = new File(repoDir.getCanonicalPath());
                this.repoDirPath = repoDirCanonical.getAbsolutePath();
                System.out.println("working directory successfully set");
                save();
            }
            catch (IOException e) {
                System.out.println("\u001B[31m[error]\u001B[30m canonical path for file not found");
                System.out.println(e.getMessage());
            }
        }
        else {
            System.out.println("\u001B[31m[error]\u001B[30m please provide a valid path");
        }
    }

    public void addClaim(int ID, claimSpec claim) {
        claims.add(ID, claim);
    }
    
    private void help() {
        System.out.println("\nOverview:  ETB 2.0 - Evidential Tool Bus (Linux 64-bit version)\n");
        System.out.println("Usage:     etb2 [options] <inputs>\n");
        System.out.println("Options: \n");
        System.out.println("-help/-h          shows this help menue");
        System.out.println("-init             initialises an etb node at a given location");
        System.out.println("-node-info        displays details of the node, like its port, claims, workflows, local services and available remote servers/services");
        System.out.println("-clean            removes available local services and remote servers from the server");
        System.out.println("-uninit           deletes initialisation componenets of the node");
        System.out.println("-set-port <int>   sets <int> as the port number of the server");
        System.out.println("-set-repo <dir>   sets <dir> as the git repo used as working directory");
        System.out.println("-query <term>     runs a query to get solutions for the given term");
        System.out.println("-script <file>    executes a file with datalog workflow to get solutions for its queries");
        System.out.println("-add-service      adds local service(s) to the server");
        System.out.println("-rm-service       removes local service(s) from the node");
        System.out.println("-add-server       adds remote server(s) whose services will avilable to the etb node");
        System.out.println("-rm-server        removes remote servers");
        System.out.println("-add-claim        adds claim(s) to the etb node");
        System.out.println("-rm-claim         removes claim(s) from the etb node");
        System.out.println("-update-claim     updates an outdated claim");
        System.out.println("-upgrade-claim    upgrades an outdated claim");
        System.out.println("-reconst-claim    reconstructs an outdated claim\n");
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[hostIP: " + hostIP + " -- port: " + port + "]");
        sb.append("\n==> git repo path : " + repoDirPath);
        sb.append(claims);
        sb.append(workflowsPack);
        sb.append(servicePack);
        sb.append(serversPack);
        return sb.toString();
    }
    
    private void save() {
        
        JSONObject NewObj = new JSONObject();
        NewObj.put("port", this.port);
        NewObj.put("repoDirPath", this.repoDirPath);
        NewObj.put("servicePack", servicePack.toJSONObject());
        NewObj.put("servers", serversPack.toJSONObject());
        NewObj.put("workflows", workflowsPack.toJSONObject());
        NewObj.put("claims", claims.toJSONObject());
        
        try {
            FileWriter fw = new FileWriter(paramsFilePath);
            fw.write(NewObj.toJSONString());
            fw.flush();
            fw.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> inQuery2Params(String inQuery) {
        List<String> serviceArgs = new ArrayList();
        int commasCount = inQuery.replaceAll("[^,]","").length();
        String paramsPatt = "(\\w+)";
        for (int i=0; i < commasCount; i++) {
            paramsPatt += "\\s*[,]\\s*(\\w+)";
        }
        String queryPattern = "(\\w+)\\s*[(]\\s*" + paramsPatt + "\\s*[)]\\s*";
        Pattern p1 = Pattern.compile(queryPattern);
        Matcher m1 = p1.matcher(inQuery);
        if (m1.find()) {
            for( int i=1; i <= m1.groupCount(); i++) {
                serviceArgs.add(m1.group(i));
            }
        } else {
            System.out.println("error: inQuery2Params -- invalid query string");
        }
        return serviceArgs;
    }
    
    public static void main(String args[]){
        etbNode FW = new etbNode();
        FW.run(args);
    }

}

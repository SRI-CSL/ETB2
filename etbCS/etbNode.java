package etb.etbCS;

import java.util.*;
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
import etb.etbDL.services.*;
import etb.etbDL.output.*;

public class etbNode {
    
    String hostIP = "127.0.0.1";
    int port = 0;
    String repoDirPath = System.getProperty("user.dir");

    servicesPack services;
    serversPackage serversPack;
    workflowsPackage workflowsPack;
    claimsPack claims;
    
    String paramsFilePath = System.getProperty("user.dir") + "/params.json";
    
    JSONObject nodeParamsJSONObj = new JSONObject();
    
    public etbNode() {
        try {
            this.hostIP = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        
        services = new servicesPack();
        serversPack = new serversPackage();
        workflowsPack = new workflowsPackage();
        claims = new claimsPack();
    }
    
    private void initialise() {
        File paramsFile = new File(paramsFilePath);
        if (paramsFile.exists()){
            System.out.println("--> \u001B[31m[warning]\u001B[30m initialised ETB node already exists at this location (use -h to see more options)");
        }
        else {
            while (true) {
                System.out.print("--> provide port : ");
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
                System.out.print("--> provide git repo : ");
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
            System.out.println("ETB node initialised (use -h to see more options to update the node)");
        }
    }

    private void instantiate() {
        try {
            JSONParser parser = new JSONParser();
            nodeParamsJSONObj = (JSONObject) parser.parse(new FileReader(this.paramsFilePath));
        } catch (FileNotFoundException e) {
            System.out.println("\u001B[31m[error]\u001B[30mno ETB node at this location (use -init to initialise an ETB node)");
            System.exit(0);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        
        port = Integer.valueOf(nodeParamsJSONObj.get("port").toString());
        repoDirPath = (String) nodeParamsJSONObj.get("repoDirPath");
        
    }

    private void populate() {
        
        services = new servicesPack((JSONArray) nodeParamsJSONObj.get("services"));
        serversPack = new serversPackage((JSONArray) nodeParamsJSONObj.get("servers"));
        workflowsPack = new workflowsPackage(repoDirPath, (JSONArray) nodeParamsJSONObj.get("workflows"));
        claims = new claimsPack((JSONArray) nodeParamsJSONObj.get("claims"));
        
    }

    public void run(String args[]) {
        if (args.length == 1) {
            if (args[0].equals("-init")) {
                initialise();
                System.exit(1);
            }
            else if (args[0].equals("-uninit")) {
                utils.runCMD0("rm -f etb/wrappers/* wrappers/* " + paramsFilePath);
                glueCodeAutoGen.updateExternPredBridgeFile();
                System.exit(1);
            }
            else if (args[0].equals("-clean")) {
                utils.runCMD0("rm -f etb/wrappers/* wrappers/*");
                glueCodeAutoGen.updateExternPredBridgeFile();
                instantiate();
                save();
                System.exit(1);
            }
            else if (args[0].equals("-help") || args[0].equals("-h")){
                help();
                System.exit(1);
            }
            else {
                instantiate();
                populate();
                if (args[0].equals("-node-info")){
                    print();
                }
                else if (args[0].equals("-claims-status")){
                    claims.checkStatus(workflowsPack.getWorkflows(), repoDirPath);
                }
                
                else if (args[0].equals("-add-workflow")){
                    workflowsPack.add();
                    save();
                }
                else if (args[0].equals("-rm-workflow")){
                    workflowsPack.remove();
                    save();
                }

                else if (args[0].equals("-add-claim")){
                    claims.add(workflowsPack.getWorkflows(), repoDirPath, this);
                    save();
                }
                else if (args[0].equals("-rm-claim")){
                    claims.remove();
                    save();
                }

                else if (args[0].equals("-add-service")){
                    services.add();
                    save();
                }
                else if (args[0].equals("-rm-service")){
                    services.remove();
                    save();
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
                System.exit(0);
            }
        }
        else if (args.length == 2) {
            instantiate();
            populate();
            if (args[0].equals("-query")){
                List<String> serviceArgs = inQuery2Params(args[1]);
                String serviceName = serviceArgs.remove(0);
                //System.out.println("serviceName : " + serviceName);
                //System.out.println("serviceArgs : " + serviceArgs.toString());
                processQuery(serviceName, serviceArgs);
            }
            else if (args[0].equals("-script")){
                String scriptFile = args[1];
                File SourceFileObj = new File(args[1]);
                if (!SourceFileObj.exists()) {
                    System.out.println("error: problem with the script file!");
                }
                try {
                    etbDatalog dlPack = new etbDatalog();
                    dlPack.parseToDatalog(scriptFile);
                    etbDatalogEngine dlEngine = new etbDatalogEngine();
                    Collection<Map<String, String>> answers = dlEngine.run(this, dlPack);
                    
                    System.out.println(dlPack.getGoal().toString());
                    if (answers == null) {
                        System.out.println("=> no claim for the query (null binding found)");
                    }
                    else {
                        //if (answers != null) {
                        //System.out.println(dlPack.getGoal().toString());
                        QueryOutput qo = new DefaultQueryOutput();
                        qo.writeResult2(answers);
                    }
                } catch (DatalogException e) {
                    e.printStackTrace();
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
                setWorkingDirectory(args[1]);
            }
            else if (args[0].equals("-update-claim")){
                claims.update(args[1], workflowsPack.getWorkflows(), repoDirPath, this);
                save();
            }
            else if (args[0].equals("-upgrade-claim")){
                claims.upgrade(args[1], workflowsPack.getWorkflows(), repoDirPath, this);
                save();
            }
            else if (args[0].equals("-reconst-claim")){
                claims.recreate(args[1], workflowsPack.getWorkflows(), repoDirPath, this);
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
    
    private void setWorkingDirectory(String inDirPath) {
        File repoDir = new File(inDirPath);
        if (repoDir.exists() && repoDir.isDirectory()){
            //this.repoDirPath = inDirPath;
            try {
                File repoDirCanonical = new File(repoDir.getCanonicalPath());
                this.repoDirPath = repoDirCanonical.getAbsolutePath();
                save();
                System.out.println("working directory successfully set");
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
    
    private void print() {
        System.out.println("hostIP : " + hostIP);
        System.out.println("port : " + port);
        System.out.println("git repo path : " + repoDirPath);
        claims.print();
        workflowsPack.print();
        services.print();
        serversPack.print();
    }
    
    private void save() {
        
        JSONObject NewObj = new JSONObject();
        NewObj.put("port", this.port);
        NewObj.put("repoDirPath", this.repoDirPath);
        NewObj.put("services", services.toJSONObject());
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
    
    public queryResult processQuery(String serviceName, List<String> serviceArgs) {
        Expr queryExpr = new Expr(serviceName, serviceArgs);
        if (services.containsService(serviceName)) {
            System.out.println("\t -> processing - " + serviceName + " - as a local service");
            System.out.println("\t -> service args " + serviceArgs.toString());
            String serviceInvMode = glueCodeAutoGen.getMode(serviceArgs);
            System.out.println("\t\t -> service invocation mode: " + serviceInvMode);
            String serviceSign = services.get(serviceName).getSignature();
            System.out.println("\t\t -> service signature: " + serviceSign);
            
            Iterator<String> argsIter = serviceArgs.iterator();
            ArrayList<String> serviceArgs2 = new ArrayList();
            while (argsIter.hasNext()) {
                serviceArgs2.add(argsIter.next());
            }
            
            externPred2Service extService = new externPred2ServiceInstance();
            Expr retQueryExpr = extService.invoke(serviceName, serviceArgs2, serviceInvMode, serviceSign);
            
            if (retQueryExpr == null) {
                System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m service could not be executed (please check previous error logs)");
                return new queryResult(queryExpr, null);
            }

            System.out.println("\t\t -> service invocation result: " + retQueryExpr.toString());
            Map<String, String> locBindings = new HashMap();
            retQueryExpr.unify(queryExpr, locBindings);
            System.out.println("\t\t -> bindings: " + OutputUtils.bindingsToString(locBindings));
            if (extService.getEvidence() == null) {
                System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m no evidence (please check the wrapper)");
            }
            else {
                System.out.println("\t\t -> evidence: " + extService.getEvidence());
            }
            return new queryResult(retQueryExpr, extService.getEvidence(), locBindings);
        }
        else {
            //getting all servers providing the service requested in the query
            Collection<serverSpec> usefulServerSpec = serversPack.getServers().values().stream().filter(eachServerSpec -> eachServerSpec.getServices().contains(serviceName)).collect(Collectors.toSet());
            if (usefulServerSpec.isEmpty()) {
                return new queryResult();
            }
            //TODO: trying to get the service from the first available server *** special tactic/heuristic needed?
            Iterator<serverSpec> eachServerIter = usefulServerSpec.iterator();
            while (eachServerIter.hasNext()) {
                serverSpec eachServer = eachServerIter.next();
                System.out.println("\t -> processing - " + serviceName + " - as a remote service");
                System.out.println("\t -> service args " + serviceArgs.toString());
                eachServer.print("\t\t -> ");
                
                clientMode cm = new clientMode(eachServer.getAddress(),eachServer.getPort());
                if (cm.isConnected()) {
                  return cm.remoteServiceExecution(serviceName, serviceArgs, repoDirPath);
                }
            }
            return new queryResult();
        }
    }
    
    //server mode
    public static String getFileOLD(String fileName, DataInputStream fromClientData) throws IOException {
        //needs to check first if claim already exists in the server
        //then the computation follows
        String tempDirPath = "TEMP";
        File tempDir = new File(tempDirPath);
        if (!tempDir.isDirectory()) {
            tempDir.mkdir();
            fileName = "TEMP/temp_1_" + fileName;
        }
        else {
            fileName = "TEMP/temp_" + (new File("TEMP").list().length + 1) + "_" + fileName;
        }
        File fout = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        
        String line;
        while(!(line = fromClientData.readUTF()).equals("EOF")) {
            bw.write(line);
            bw.newLine();
        }
        bw.close();
        fos.close();
        return fileName;
    }

    public static String getFile(String claimWorkingDir, String fileName, DataInputStream fromClientData) throws IOException {
        //needs to check first if claim already exists in the server
        //then the computation follows
        
        fileName = claimWorkingDir + "/temp_" + (new File(claimWorkingDir).list().length + 1) + "_" + fileName;

        File fout = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        
        String line;
        while(!(line = fromClientData.readUTF()).equals("EOF")) {
            bw.write(line);
            bw.newLine();
        }
        bw.close();
        fos.close();
        return fileName;
    }
    
    public void serverRun() {
        instantiate();
        populate();
        while (true) {
            print();
            System.out.println("waiting for a client ...");
            try (
                 ServerSocket serverSocket = new ServerSocket(this.port);
                 Socket clientSocket = serverSocket.accept();

                 InputStream inStr = clientSocket.getInputStream();
                 DataInputStream fromClientData = new DataInputStream(inStr);
                 OutputStream outStr = clientSocket.getOutputStream();
                 DataOutputStream toClientData = new DataOutputStream(outStr);

                 ) {
                System.out.println("connected to client : " + clientSocket.getInetAddress().getHostAddress());
                String request;
                if ((request = fromClientData.readUTF()) == null) {
                    System.out.println("null request found (liveness check?)");
                }
                
                else if (request.equals("regstReqst")){
                    System.out.println("this is a service registration request");
                    System.out.println("request being processed ... ");
                    //TODO: --> announce all the services I provide
                    System.out.println("request procssing done");
                    toClientData.writeUTF(services.getNames());
                }
                
                else if (request.equals("execReqst")){
                    System.out.println("request received for service execution");

                    //reading service details from client
                    String serviceName = fromClientData.readUTF();
                    System.out.println("serviceName : " + serviceName);
                    String serviceInvMode = fromClientData.readUTF();
                    System.out.println("serviceInvMode : " + serviceInvMode);

                    //reading service args from client
                    List<String> serviceArgs = getArgsFromClient(fromClientData, toClientData);
                    System.out.println("serviceArgs : " + serviceArgs.toString());
                    
                    queryResult qr = processQuery(serviceName, serviceArgs);
                    System.out.println("service execution done");

                    //sending back result
                    System.out.println("execution result: " + qr.getResultExpr().toString());
                    List<String> resultArgs = qr.getResultExpr().getTerms();
                    Iterator<String> resultIter = resultArgs.iterator();
                    while (resultIter.hasNext()) {
                        toClientData.writeUTF(resultIter.next());
                    }
                    toClientData.writeUTF("done");
                    toClientData.writeUTF(qr.getEvidence());
                    
                    //claimSpec newClaim = new claimSpec(new Expr(serviceName, serviceArgs));
                    claimSpec newClaim = new claimSpec(qr.getResultExpr());
                    newClaim.generateSHA1(repoDirPath);
                    newClaim.addAnswer(qr.getBindings());
                    claims.add(newClaim);
                    
                    save();
                }
                
                else if (request.equals("statusCheck")){
                    System.out.println("request received for availability check");
                }
                
                else {
                    System.out.println("unknown request type");
                }
                clientSocket.close();
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("error while trying to listen on port " + this.port + " or connection error");
                System.out.println(e.getMessage());
            }
        }
    }

    private List<String> getArgsFromClient(DataInputStream fromClientData, DataOutputStream toClientData) throws IOException {
        List<String> serviceArgs = new ArrayList();
        
        //setting up a directory to save temporary files
        String claimWorkingDirPath = repoDirPath + "/TEMP";
        File claimWorkingDir = new File(claimWorkingDirPath);
        if (!claimWorkingDir.isDirectory()) {
            claimWorkingDir.mkdir();
        }
        claimWorkingDirPath += "/claim" + (claimWorkingDir.list().length + 1);
        claimWorkingDir = new File(claimWorkingDirPath);
        claimWorkingDir.mkdir();
        
        String serviceArgType;
        while(!(serviceArgType = fromClientData.readUTF()).equals("done")) {
            System.out.println("serviceArgType : " + serviceArgType);
            if (serviceArgType.equals("file_list")) {
                //argument is list of files
                System.out.println("-> a list of files as arg");
                String eachFilePath, listRead = "listIdent";
                while(!(eachFilePath = fromClientData.readUTF()).equals("file_list_done")) {
                    //System.out.println("\t -> file : " + listElement);
                    //String eachFilePath = listElement;
                    System.out.println("\t\t -> file : " + eachFilePath);
                    String SHA1 = fromClientData.readUTF();
                    System.out.println("\t\t -> file SHA1 : " + SHA1);
                    if (utils.existsInRepo(eachFilePath, repoDirPath)) {
                        System.out.println("\t\t -> file in server repo");
                        if (utils.getSHA1(eachFilePath).equals(SHA1)) {
                            System.out.println("\t\t -> SHA1 matches");
                            toClientData.writeUTF("done");
                        }
                        else {
                            System.out.println("\t\t -> SHA1 does not match");
                            toClientData.writeUTF("sendMeCopy");
                            File eachFile = new File(eachFilePath);
                            //eachFilePath = getFile(eachFile.getName(), fromClientData);
                            eachFilePath = getFile(claimWorkingDirPath, eachFile.getName(), fromClientData);
                        }
                    }
                    else {
                        System.out.println("\t\t -> file NOT in server repo");
                        toClientData.writeUTF("sendMeCopy");
                        File eachFile = new File(eachFilePath);
                        //eachFilePath = getFile(eachFile.getName(), fromClientData);
                        eachFilePath = getFile(claimWorkingDirPath, eachFile.getName(), fromClientData);
                    }
                    listRead += " file(" + eachFilePath + ")";
                }
                System.out.println("new argument after reading files : " + listRead);
                serviceArgs.add(listRead);
            }
            else if (serviceArgType.equals("file")) {
                System.out.println("-> a file as arg");
                String fileElement = fromClientData.readUTF();
                System.out.println("\t -> file : " + fileElement);
                String SHA1 = fromClientData.readUTF();
                System.out.println("\t -> file SHA1 : " + SHA1);
                
                if (utils.existsInRepo(fileElement, repoDirPath)) {
                    System.out.println("\t\t -> file in server repo");
                    if (utils.getSHA1(fileElement).equals(SHA1)) {
                        System.out.println("\t\t -> SHA1 matches");
                        toClientData.writeUTF("done");
                    }
                    else {
                        System.out.println("\t\t -> SHA1 does not match");
                        toClientData.writeUTF("sendMeCopy");
                        File eachFile = new File(fileElement);
                        //fileElement = getFile(eachFile.getName(), fromClientData);
                        fileElement = getFile(claimWorkingDirPath, eachFile.getName(), fromClientData);
                    }
                }
                else {
                    System.out.println("\t\t -> file NOT in server repo");
                    toClientData.writeUTF("sendMeCopy");
                    File eachFile = new File(fileElement);
                    //fileElement = getFile(eachFile.getName(), fromClientData);
                    fileElement = getFile(claimWorkingDirPath, eachFile.getName(), fromClientData);
                }
                serviceArgs.add("file(" + fileElement + ")");
            }
            else {
                serviceArgs.add(fromClientData.readUTF());
            }
        }
        return serviceArgs;
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
        if (args.length == 0) {
            //running as a server
            FW.serverRun();
        }
        else {
         FW.run(args);
        }

    }

}

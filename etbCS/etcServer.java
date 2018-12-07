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

import etb.etbDL.utils.*;
import etb.etbDL.services.*;
import etb.etbDL.output.*;

public class etcServer {
    
    String hostIP = "127.0.0.1";
    int port = 0;
    Map<String, serviceSpec> etcServices = new HashMap();
    Map<String, serverSpec> servers = new HashMap();
    ArrayList<String> serverIDs = new ArrayList(); //TODO: all in one
    
    String paramsFilePath = System.getProperty("user.dir") + "/params.json";
    String repoDirPath = System.getProperty("user.dir");

    public etcServer() {
        try {
            this.hostIP = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
    
    private void initialise() {
        File paramsFile = new File(paramsFilePath);
        if (paramsFile.exists()){
            System.out.println("--> server already initialised (use -h to see more options for updating server)");
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
            System.out.println("server initialised (use -h to see more options for adding more server components)");
        }
    }

    public void populate() {

        JSONObject serverParamsJSONObj = new JSONObject();
        try {
            JSONParser parser = new JSONParser();
            serverParamsJSONObj = (JSONObject) parser.parse(new FileReader(this.paramsFilePath));
        } catch (FileNotFoundException e) {
            System.out.println("\u001B[31m[error]\u001B[30m server not yet initialised (use -init to initialise server)");
             System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        this.port = Integer.valueOf(serverParamsJSONObj.get("port").toString());
        this.repoDirPath = (String) serverParamsJSONObj.get("repoDirPath");
        
        JSONArray etcServicesJSON = (JSONArray) serverParamsJSONObj.get("services");
        Iterator<JSONObject> iterator = etcServicesJSON.iterator();
        while (iterator.hasNext()) {
            JSONObject serviceSpecObj = (JSONObject) iterator.next();
            String serviceName = (String) serviceSpecObj.get("name");
            this.etcServices.put(serviceName, new serviceSpec(serviceSpecObj));
        }
        
        JSONArray serversArrayJSON = (JSONArray) serverParamsJSONObj.get("servers");
        Iterator<JSONObject> iterator2 = serversArrayJSON.iterator();
        while (iterator2.hasNext()) {
            serverSpec serv = new serverSpec(iterator2.next());
            this.servers.put(serv.getID(), serv);
            this.serverIDs.add(serv.getID());
        }
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
                this.etcServices = new HashMap();
                this.servers = new HashMap();
                save();
                System.exit(1);
            }
            else if (args[0].equals("-help") || args[0].equals("-h")){
                help();
                System.exit(1);
            }
            else {
                populate();
                if (args[0].equals("-info")){
                    printServerDetails();
                }
                else if (args[0].equals("-add-service")){
                    addServices();
                }
                else if (args[0].equals("-rm-service")){
                    removeServices();
                }
                else if (args[0].equals("-add-server")){
                    addServers();
                }
                else if (args[0].equals("-rm-server")){
                    removeServers();
                }
                else {
                    System.out.println("ERROR. Unknown option: " + args[0]);
                }
                System.exit(0);
            }
        }
        else if (args.length == 2) {
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
                    
                    if (answers != null) {
                        System.out.println(dlPack.getGoal().toString());
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
                setRepo(args[1]);
            }
            else {
                System.out.println("ERROR. Unknown option: " + args[0]);
            }
        }
        else {
            System.out.println("ERROR. Incorrect Workflow argument(s) structure");
        }
    }
    
    private void setRepo() {
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
    }

    private boolean setRepo(String inDirPath) {
        
        File repoDir = new File(inDirPath);
        if (repoDir.exists() && repoDir.isDirectory()){
            this.repoDirPath = inDirPath;
            try {
                File repoDirCan = new File(repoDir.getCanonicalPath());
                this.repoDirPath = repoDirCan.getAbsolutePath();
                save();
                return true;
            }
            catch (IOException e) {
                System.out.println("\u001B[31m[error]\u001B[30m canonical path for file not found");
                System.out.println(e.getMessage());
            }
        }
        else {
            System.out.println("\u001B[31m[error]\u001B[30m please provide a valid path");
        }
        return false;
    }

    private void help() {
        System.out.println("");
        System.out.println("Overview:  ETB 2.0 - Evidential Tool Bus (Linux 64-bit version)\n");
        System.out.println("Usage:     etb2 [options] <inputs>\n");
        System.out.println("Options: \n");
        System.out.println("-help/-h          shows this help menue");
        System.out.println("-init             initialises the server at a given location");
        System.out.println("-info             displays details of the server, like port, local services and available remote servers/services");
        System.out.println("-clean            removes available local services and remote servers from the server");
        System.out.println("-set-port <int>   sets <int> as the port number of the server");
        System.out.println("-set-port <dir>   sets <dir> as the git repo used for storing files");
        System.out.println("-query <term>     runs a query to get solutions for the given term");
        System.out.println("-script <file>    executes a file with datalog workflow to get solutions for its queries");
        System.out.println("-add-service     adds local service(s) to the server");
        System.out.println("-rm-service      removes local service(s) from the server");
        System.out.println("-add-server      adds remote server(s) whose services are avilable to the server");
        System.out.println("-rm-server       removes remote servers\n");
    }
    
    private void printServerDetails() {
        System.out.println("==============================");
        System.out.println("hostIP : " + hostIP);
        System.out.println("port : " + port);
        System.out.println("git repo path : " + repoDirPath);
        
        System.out.println("------------------------------");
        if (etcServices.size() == 0) {
            System.out.println("local services : []");
        }
        
        Set<String> serviceNames = etcServices.keySet();
        Iterator<String> iterator = serviceNames.iterator();
        
        for (int i=1; iterator.hasNext(); i++) {
            System.out.println("local service " + i);
            String spekKey = iterator.next();
            etcServices.get(spekKey).print("-> ", spekKey);
        }
        
        System.out.println("------------------------------");
        if (serverIDs.size() == 0) {
            System.out.println("remote servers : []");
        }
        for (int i=0; i<serverIDs.size(); i++) {
            System.out.println("remote server " + (i+1));
            servers.get(serverIDs.get(i)).print("-> ");
        }
        System.out.println("==============================");
    }
    
    private void addServices() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> service name : ");
            String serviceName = in.nextLine();
            
            if (etcServices.containsKey(serviceName)) {
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
            
            //ArrayList<String> modeSet = glueCodeAutoGen.addToolWrapper(serviceName, signature.size(), Integer.parseInt(modesCount), toolExecModes);
            ArrayList<String> modeSet = glueCodeAutoGen.addToolWrapper(serviceName, signature, Integer.parseInt(modesCount), toolExecModes);
            
            etcServices.put(serviceName, new serviceSpec(signature, modeSet));

            System.out.println("=> tool added successfully");
            System.out.print("=> add more tools? [y] to add more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        //System.out.println("=> getServiceNames() : " + etcServices.keySet().toString());
        ArrayList<String> etcServiceNames = new ArrayList();
        etcServiceNames.addAll(etcServices.keySet());
        glueCodeAutoGen.updateExternPredBridgeFile(etcServiceNames);
        save();
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

    private void addServers() {
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
        save();
    }

    private void removeServers() {
        
        System.out.println("------------------------------");
        if (serverIDs.size() == 0) {
            System.out.println("remote servers : []");
        }
        for (int i=0; i<serverIDs.size(); i++) {
            System.out.println("remote server " + (i+1));
            System.out.println("-> serverID : " + (i+1));
            servers.get(serverIDs.get(i)).print("-> ");
        }
        System.out.println("==============================");
        
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> enter serverID : ");
            String serverID = in.nextLine();
            
            if (Integer.valueOf(serverID) < 1 || Integer.valueOf(serverID) > serverIDs.size()) {
                System.out.println("=> invalid serverID '" + serverID + "' \u001B[31m(removal not successful)\u001B[30m");
                System.out.print("=> remove more servers? [y] to remove more : ");
                if (!in.nextLine().equals("y"))
                    break;
                continue;
            }
            servers.remove(serverIDs.get(Integer.valueOf(serverID)-1));
            serverIDs.remove(Integer.valueOf(serverID)-1);
            
            System.out.println("=> server removed successfully");
            System.out.print("=> remove more servers? [y] to remove more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        save();
    }

    private void removeServices() {
        
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("--> service name : ");
            String toolName = in.nextLine();
            
            //checking if workflow already exists with the same name
            if (!etcServices.keySet().contains(toolName)) {
                System.out.println("=> a service with the name '" + toolName + "' does not exist \u001B[31m(removal not successful)\u001B[30m");
                System.out.print("=> remove more tools? [y] to remove more : ");
                if (!in.nextLine().equals("y"))
                    break;
                continue;
            }
            etcServices.remove(toolName);
            glueCodeAutoGen.removeToolWrapper(toolName);
            System.out.println("=> tool removed successfully");
            System.out.print("=> remove more tools? [y] to remove more : ");
            if (!in.nextLine().equals("y"))
                break;
        }
        ArrayList<String> etcServiceNames = new ArrayList();
        etcServiceNames.addAll(etcServices.keySet());
        glueCodeAutoGen.updateExternPredBridgeFile(etcServiceNames);
        save();
    }

    private void save() {
        
        JSONObject NewObj = new JSONObject();
        NewObj.put("port", this.port);
        NewObj.put("repoDirPath", this.repoDirPath);
        
        JSONArray etcServicesJSON = new JSONArray();
        Set<String> serviceNames = etcServices.keySet();
        Iterator<String> iterator = serviceNames.iterator();
        while (iterator.hasNext()) {
            String serviceSpecKey = iterator.next();
            etcServicesJSON.add(etcServices.get(serviceSpecKey).toJSONObj(serviceSpecKey));
        }
        NewObj.put("services", etcServicesJSON);
        
        JSONArray serversSpecsJSON = new JSONArray();
        Iterator<String> remoteServerIter = servers.keySet().iterator();
        while (remoteServerIter.hasNext()) {
            String serverID = remoteServerIter.next();
            serversSpecsJSON.add(servers.get(serverID).toJSONObj());
        }
        NewObj.put("servers", serversSpecsJSON);

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
        if (etcServices.keySet().contains(serviceName)) {
            System.out.println("\t -> processing - " + serviceName + " - as a local service");
            System.out.println("\t -> service args " + serviceArgs.toString());
            String serviceInvMode = glueCodeAutoGen.getMode(serviceArgs);
            System.out.println("\t\t -> service invocation mode: " + serviceInvMode);
            /*
             ArrayList<String> serviceSign = this.etcServices.get(serviceName).getSignature();
            System.out.println("\t\t -> service signature: " + serviceSign.toString());
            
            if (this.etcServices.get(serviceName).getModes().contains(serviceInvMode)) {
                System.out.println("\t\t\t -> known mode");
            }
            else {
                System.out.println("\t\t\t -> unknown mode");
            }
            */
            
            Iterator<String> argsIter = serviceArgs.iterator();
            ArrayList<String> serviceArgs2 = new ArrayList();
            while (argsIter.hasNext()) {
                serviceArgs2.add(argsIter.next());
            }
            
            externPred2Service extService = new externPred2ServiceInstance();
            Expr retQueryExpr = extService.invoke(serviceName, serviceArgs2, serviceInvMode);
            
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
            return new queryResult(retQueryExpr, extService.getEvidence());
        }
        else {
            //getting all servers providing the service requested in the query
            Collection<serverSpec> usefulServerSpec = servers.values().stream().filter(eachServerSpec -> eachServerSpec.getServices().contains(serviceName)).collect(Collectors.toSet());
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
                //return remoteServiceExecution(getServerConnection(eachServer.getAddress(),eachServer.getPort()), serviceName, serviceArgs);
                //return remoteServiceExecution(eachServer.getAddress(),eachServer.getPort(), serviceName, serviceArgs);
            }
            return new queryResult();
        }
    }
    
    //server mode
    public static String getFile(String fileName, DataInputStream fromClientData) throws IOException {
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
    
    public void serverRun() {
        populate();
        while (true) {
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
                    
                    String servicesStr = null;
                    Iterator<String> serviceNameIter = etcServices.keySet().iterator();
                    if (serviceNameIter.hasNext())
                        servicesStr = serviceNameIter.next();
                    //Set<String> serviceNames = etcServices.keySet();
                    while (serviceNameIter.hasNext()) {
                        servicesStr += " " + serviceNameIter.next();
                    }
                    toClientData.writeUTF(servicesStr);
                    //toClientData.writeUTF("[processed of (" + request + ")");
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
        String serviceArgType;
        while(!(serviceArgType = fromClientData.readUTF()).equals("done")) {
            System.out.println("serviceArgType : " + serviceArgType);
            if (serviceArgType.equals("file_list")) {
                System.out.println("-> a list of files as arg");
                String listElement, listRead = "listIdent";
                while(!(listElement = fromClientData.readUTF()).equals("file_list_done")) {
                    System.out.println("\t -> file : " + listElement);
                    String eachFilePath = listElement;
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
                            eachFilePath = getFile(eachFile.getName(), fromClientData);
                        }
                    }
                    else {
                        System.out.println("\t\t -> file NOT in server repo");
                        toClientData.writeUTF("sendMeCopy");
                        File eachFile = new File(eachFilePath);
                        eachFilePath = getFile(eachFile.getName(), fromClientData);
                    }
                    listRead += " file(" + eachFilePath + ")";
                }
                System.out.println("new argument after reading files : " + listRead);
                serviceArgs.add(listRead);
            }
            else if (serviceArgType.equals("file")) {
                System.out.println("-> a list of files as arg");
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
                        fileElement = getFile(eachFile.getName(), fromClientData);
                    }
                }
                else {
                    System.out.println("\t\t -> file NOT in server repo");
                    toClientData.writeUTF("sendMeCopy");
                    File eachFile = new File(fileElement);
                    fileElement = getFile(eachFile.getName(), fromClientData);
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
        etcServer FW = new etcServer();
        if (args.length == 0) {
            //running as a server
            FW.serverRun();
        }
        else {
         FW.run(args);
        }

    }
}

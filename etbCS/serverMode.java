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
import etb.etbDL.output.*;

public class serverMode {
        
    public void run(etbNode node, String repoDirPath, int port) {
        while (true) {
            System.out.println(node.toString());
            System.out.println("waiting for a client ...");
            try (
                 ServerSocket serverSocket = new ServerSocket(port);
                 Socket clientSocket = serverSocket.accept();
                 InputStream inStr = clientSocket.getInputStream();
                 DataInputStream fromClientData = new DataInputStream(inStr);
                 OutputStream outStr = clientSocket.getOutputStream();
                 DataOutputStream toClientData = new DataOutputStream(outStr);
                 ){
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
                    toClientData.writeUTF(node.getServicePack().getNames());
                }
                else if (request.equals("execReqst")){
                    System.out.println("=> request received for service execution");
                    //reading service details from client
                    String serviceName = fromClientData.readUTF();
                    System.out.println("-> serviceName : " + serviceName);
                    String serviceInvMode = fromClientData.readUTF();
                    System.out.println("-> serviceInvMode : " + serviceInvMode);
                    //reading service args from client
                    List<String> serviceArgs = getArgsFromClient(repoDirPath, fromClientData, toClientData);
                    System.out.println("-> serviceArgs : " + serviceArgs.toString());
                    
                    String serviceSign = fromClientData.readUTF();
                    System.out.println("-> serviceSign : " + serviceSign);
                    
                    Expr query = new Expr(serviceName, serviceArgs);
                    query.setMode(serviceInvMode);
                    query.setSignature(serviceSign);
                    
                    serviceInvocation inv = new serviceInvocation(query);
                    inv.process(node);
                    System.out.println("-> service execution done");
                    
                    //sending back result
                    System.out.println("-> execution result: " + inv.getResult());
                    List<String> resultArgs = inv.getResult().getTerms();

                    Iterator<String> resultIter = resultArgs.iterator();
                    while (resultIter.hasNext()) {
                        toClientData.writeUTF(resultIter.next());
                    }
                    toClientData.writeUTF("done");
                    toClientData.writeUTF(inv.getEvidence());
                    
                    Collection<Map<String, String>> answers = new ArrayList();
                    Map<String, String> answer = new HashMap();
                    inv.getResult().unify(query, answer);
                    answers.add(answer);
                    
                    if (answers == null) {
                        System.out.println("=> \u001B[31mclaim addition not successful\u001B[30m (service: " + serviceName + ")");
                    }
                    else {
                        claimSpec claim = new claimSpec(query, answers, repoDirPath);
                        node.addClaim(query.hashCode(), claim);
                        System.out.println("=> claim added successfully");
                    }
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
                System.out.println("error while trying to listen on port " + port + " or connection error");
                System.out.println(e.getMessage());
            }
        }
    }

    public static String getFile(String claimWorkingDir, String fileName, DataInputStream fromClientData) throws IOException {
        //needs to check first if claim already exists in the server
        //then the computation follows
        fileName = claimWorkingDir + "/temp_" + (new File(claimWorkingDir).list().length + 1) + "_" + fileName;
        System.out.println("fileName: " + fileName);
        //File fout = new File(fileName);
        //FileOutputStream fos = new FileOutputStream(fout);
        FileOutputStream fos = new FileOutputStream(new File(fileName));
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

    private List<String> getArgsFromClient(String repoDirPath, DataInputStream fromClientData, DataOutputStream toClientData) throws IOException {
        List<String> serviceArgs = new ArrayList();
        String serviceArgType;
        while(!(serviceArgType = fromClientData.readUTF()).equals("done")) {
            System.out.println("serviceArgType : " + serviceArgType);
            if (serviceArgType.equals("file_list")) {
                //argument is of type file_list
                System.out.println("-> a list of files as arg");
                String eachFilePath, listRead = "listIdent";
                while(!(eachFilePath = fromClientData.readUTF()).equals("file_list_done")) {
                    System.out.println("\t\t -> file : " + eachFilePath);
                    eachFilePath = getFileArgFromClient(eachFilePath, fromClientData, toClientData, repoDirPath);
                    listRead += " file(" + eachFilePath + ")";
                }
                System.out.println("new argument after reading files : " + listRead);
                serviceArgs.add(listRead);
            }
            else if (serviceArgType.equals("file")) {
                //argument is of type file
                System.out.println("-> a file as arg");
                String fileElement = fromClientData.readUTF();
                System.out.println("\t -> file : " + fileElement);
                fileElement = getFileArgFromClient(fileElement, fromClientData, toClientData, repoDirPath);
                serviceArgs.add("file(" + fileElement + ")");
            }
            else {
                //argument is of type string or string_list
                serviceArgs.add(fromClientData.readUTF());
            }
        }
        return serviceArgs;
    }
    
    private String getFileArgFromClient(String fileElement, DataInputStream fromClientData, DataOutputStream toClientData, String repoDirPath) throws IOException {
        String SHA1 = fromClientData.readUTF();
        System.out.println("\t -> file SHA1 : " + SHA1);
        if (utils.existsInRepo(fileElement, repoDirPath) && utils.getSHA1(fileElement).equals(SHA1)) {
            System.out.println("\t\t -> file in server repo and SHA1 matches");
            toClientData.writeUTF("done");
        }
        else {
            System.out.println("\t\t -> file NOT in server repo or SHA1 does not match");
            toClientData.writeUTF("sendMeCopy");
            File eachFile = new File(fileElement);
            fileElement = copyFileFromClient(repoDirPath, eachFile.getName(), fromClientData);
        }
        return fileElement;
    }
    
    public static String copyFileFromClient(String repoDirPath, String fileName, DataInputStream fromClientData) throws IOException {
        //setting up name for new files in server (e.g., repoDir/TEMP/temp_1_cbmc.json)
        String claimWorkingDirPath = repoDirPath + "/TEMP";
        File claimWorkingDir = new File(claimWorkingDirPath);
        if (!claimWorkingDir.isDirectory()) {
            claimWorkingDir.mkdir();
        }
        fileName = claimWorkingDirPath + "/temp_" + (claimWorkingDir.list().length + 1) + "_" + fileName;
        
        System.out.println("fileName: " + fileName);
        
        FileOutputStream fos = new FileOutputStream(new File(fileName));
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


}

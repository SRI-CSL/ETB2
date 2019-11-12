package etb.etbCS;

import java.util.*;
import java.net.*;
import java.io.*;
import etb.etbCS.utils.*;
import etb.etbDL.utils.*;
import etb.etbDL.utils.utils;
import etb.etbDL.output.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class clientMode {
    
    Socket serverSocket = new Socket();
    String repoDirPath;
    String evidence = null;
    
    public clientMode(String hostIP, int port) {
        try {
            serverSocket.connect(new InetSocketAddress(hostIP, port), 2000);
            serverSocket.setSoTimeout(2000);
        } catch(SocketTimeoutException e) {
            System.out.println("server not responding/reachable");
        } catch (UnknownHostException e) {
            System.err.println("unknown host " + hostIP);
        } catch (IOException e) {
            System.err.println("no I/O for the connection to " + hostIP + " at port " + port);
        }
    }
    
    public boolean isConnected() {
        return serverSocket.isConnected();
    }
    
    public Expr remoteServiceExecution(Expr query, String repoDirPath) {
        String serviceName = query.getPredicate();
        List<String> serviceArgs = query.getTerms();
        this.repoDirPath = repoDirPath;
        
        try {
            InputStream inStr = serverSocket.getInputStream();
            DataInputStream fromServerData = new DataInputStream(inStr);
            OutputStream outStr = serverSocket.getOutputStream();
            DataOutputStream toServerData = new DataOutputStream(outStr);
            
            //sending requestTag to server
            toServerData.writeUTF("execReqst");
            //sending service server for execution
            toServerData.writeUTF(serviceName); //service name
            //sending the mode -- to know if service is defined for the mode
            String serviceInvMode = utils.getMode(serviceArgs);
            toServerData.writeUTF(serviceInvMode);
            //sending service args to server
            String qSign = sendArgsToServer(serviceArgs, fromServerData, toServerData);
            toServerData.writeUTF(qSign);
            
            System.out.println("\t\t -> service invocation args sent successfully");

            //reading query processing result from server
            List<String> resultArgs = new ArrayList();
            String resultArg;
            while(!(resultArg = fromServerData.readUTF()).equals("done")) {
                resultArgs.add(resultArg);
            }
            this.evidence = fromServerData.readUTF();
            System.out.println("evidence: " + evidence);
            
            serverSocket.close();
            Expr result = new Expr(serviceName, resultArgs);
            System.out.println("\t\t -> service invocation result: " + result.toString());
            
            //importing remote results to local node
            List<String> finalArgs = getFinalArgs(serviceArgs, resultArgs, serviceInvMode);
            Expr finalResult = new Expr(serviceName, finalArgs);
            
            finalResult.setMode(serviceInvMode);
            finalResult.setSignature(qSign);

            System.out.println("\t\t -> service invocation final result: " + finalResult);
            
            Map<String, String> locBindings = new HashMap();
            finalResult.unify(query, locBindings);
            System.out.println("\t\t -> bindings: " + OutputUtils.bindingsToString(locBindings));
            if (evidence == null) {
                System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m no evidence (please check the wrapper)");
            }
            else {
                System.out.println("\t\t -> evidence: " + evidence);
            }
            return finalResult;
            
        } catch (IOException e) {
            System.err.println("error: problem while sending service to remote server");
        }
        return null;
    }
    
    public String newServicesRegistration() {
        try {
            InputStream inStr = serverSocket.getInputStream();
            DataInputStream fromServerData = new DataInputStream(inStr);
            OutputStream outStr = serverSocket.getOutputStream();
            DataOutputStream toServerData = new DataOutputStream(outStr);
            
            toServerData.writeUTF("regstReqst");
            String services = fromServerData.readUTF();
            //System.out.println("found services: " + services);
            return services;
        
        } catch (IOException e) {
            System.err.println("problem while regitering remote service");
            return null;
        }
    }

    /*
    //TODO: in utils
    private boolean existsInRepo(String maybeChildPath) {
        maybeChildPath = repoDirPath + "/" + maybeChildPath;
        //System.out.println("maybeChildPath path **** " + maybeChildPath);
        File maybeChild = new File(maybeChildPath);
        File possibleParent = new File(repoDirPath);
        try {
            if (!maybeChild.exists()) {
                return false;
            }

            final File parent = possibleParent.getCanonicalFile();
            File child = maybeChild.getCanonicalFile();
            while (child != null) {
                if (child.equals(parent)) {
                    return true;
                }
                child = child.getParentFile();
            }
        } catch (IOException e) {
            System.out.println("could not find file '" + maybeChildPath + "' in the git repo");
            System.out.println(e.getMessage());
        }
        return false;
    }
    */
    
    public static void sendFileCopy(String filePath, DataOutputStream toServer) throws IOException {
        //File file = new File(filePath);
        //FileReader fileReader = new FileReader(file);
        FileReader fileReader = new FileReader(new File(filePath));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line);
            stringBuffer.append("\n");
            toServer.writeUTF(line);
        }
        toServer.writeUTF("EOF");
        fileReader.close();
        //System.out.println("Contents of file : \n" + stringBuffer.toString());
    }
    
    private String sendArgsToServer(List<String> serviceArgs, DataInputStream fromServerData, DataOutputStream toServerData) throws IOException {
        String signToSend = "";
        Iterator<String> queryIter = serviceArgs.iterator();
        int argCount = 1;
        while (queryIter.hasNext()) {
            String eachServiceArg = queryIter.next();
            System.out.println("\t\t -> arg" + argCount++ + " : " + eachServiceArg);
            List<String> eachServiceArgLS = Arrays.asList(eachServiceArg.split(" "));
            if (eachServiceArgLS.size() > 0 && eachServiceArgLS.get(0).equals("listIdent")) {
                if (eachServiceArgLS.get(1).contains("file(")) {
                    System.out.println("\t\t\t -> arg is a list of files");
                    signToSend += 4;
                    toServerData.writeUTF("file_list");
                    for (int i=1; i < eachServiceArgLS.size(); i++) {
                        //TODO: a mixed list throws an exception
                        sendFileArgToServer(eachServiceArgLS.get(i), fromServerData, toServerData, repoDirPath);
                    }
                    toServerData.writeUTF("file_list_done");
                }
                else {
                    System.out.println("\t\t -> arg is a list of strings");
                    signToSend += 3;
                    toServerData.writeUTF("string_list");
                    toServerData.writeUTF(eachServiceArg);
                }
            }
            
            else if (eachServiceArg.contains("file(")) {
                signToSend += 2;
                System.out.println("\t\t -> arg is a file");
                sendFileArgToServer(eachServiceArg, fromServerData, toServerData, repoDirPath);
            }
            
            else if (utils.isVariable(eachServiceArg)) {
                signToSend += 1;
                System.out.println("\t\t -> THIS IS A VAR TERM ");
                toServerData.writeUTF("VAR");
                toServerData.writeUTF(eachServiceArg);
            }
            
            else {
                signToSend += 1;
                System.out.println("\t\t -> THIS IS A GROUND TERM ");
                toServerData.writeUTF("string");
                toServerData.writeUTF(eachServiceArg);
            }
        }
        toServerData.writeUTF("done");
        return signToSend;
    }
    
    private void sendFileArgToServer(String inputServiceArg, DataInputStream fromServerData, DataOutputStream toServerData, String repoDirPath) throws IOException {
        String filePath = inputServiceArg.substring(5, inputServiceArg.length()-1);
        System.out.println("\t\t\t -> file : " + filePath);
        //checking of client has the file locally
        if (utils.existsInRepo(filePath, repoDirPath)) {
            System.out.println("file in client repo");
            //informing server type of data it will send next
            toServerData.writeUTF("file");
            //sending file (with path RELATIVE to repo) and SHA1
            toServerData.writeUTF(filePath.replace(repoDirPath + "/",""));
            toServerData.writeUTF(utils.getSHA1(filePath));
            //waiting for server's info on file status
            String serverResp = fromServerData.readUTF();
            System.out.println("server response : " + serverResp);
            if (serverResp.equals("sendMeCopy")) {
                sendFileCopy(filePath, toServerData);
            }
        }
        else {
            System.err.println("file NOT in client repo \u001B[31m(service execution is aborted)\u001B[30m");
            //TODO: send up this info rather than killing execution
            System.exit(1);
        }
    }
    
    private List<String> getFinalArgs(List<String> serviceOrigArgs, List<String> serviceResultArgs, String invMode) {
        List<String> finalArgs = new ArrayList();
        for (int i=0; i<serviceOrigArgs.size(); i++) {
            if (invMode.charAt(i) == '+') {
                finalArgs.add(serviceOrigArgs.get(i));
            }
            else {
                finalArgs.add(serviceResultArgs.get(i));
            }
        }
        return finalArgs;
    }
    
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
        //clientData.close();
        //in.close();
        
        return fileName;
    }
    
    public String getEvidence() {
        return this.evidence;
    }

}

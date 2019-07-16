package etb.etbCS;

import java.util.*;
import java.net.*;
import java.io.*;
import etb.etbCS.utils.*;
import etb.etbDL.utils.*;
import etb.etbDL.services.*;
import etb.etbDL.output.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class clientMode {
    
    Socket serverSocket = new Socket();
    String repoDirPath;
    
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
    
    public queryResult remoteServiceExecution(String serviceName, List<String> serviceArgs, String repoDirPath) {
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
            //String serviceInvMode = glueCodeAutoGen.getMode(serviceArgs); //TODO: move to utils **** from the glueCodeAutoGen
            String serviceInvMode = utils.getMode(serviceArgs); //TODO: move to utils **** from the glueCodeAutoGen
            toServerData.writeUTF(serviceInvMode);
            
            //sending service args to server
            sendArgsToServer(serviceArgs, fromServerData, toServerData);
            
            //reading query processing result from server
            List<String> resultArgs = new ArrayList();
            String resultArg;
            
            //while(!(resultArg = in.readLine()).equals("done")) {
            while(!(resultArg = fromServerData.readUTF()).equals("done")) {
                resultArgs.add(resultArg);
            }
            
            //String evidence = in.readLine();
            String evidence = fromServerData.readUTF();
            
            serverSocket.close();
            Expr queryExpr = new Expr(serviceName, serviceArgs);
            Expr resultQueryExpr = new Expr(serviceName, resultArgs);
            System.out.println("\t\t -> service invocation result: " + resultQueryExpr.toString());
            
            List<String> finalArgs = getFinalArgs(serviceArgs, resultArgs, serviceInvMode);
            Expr finalQueryExpr = new Expr(serviceName, finalArgs);
            System.out.println("\t\t -> service invocation final result: " + finalQueryExpr.toString());
            
            Map<String, String> locBindings = new HashMap();
            //resultQueryExpr.unify(queryExpr, locBindings);
            finalQueryExpr.unify(queryExpr, locBindings);
            System.out.println("\t\t -> bindings: " + OutputUtils.bindingsToString(locBindings));
            if (evidence == null) {
                System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m no evidence (please check the wrapper)");
            }
            else {
                System.out.println("\t\t -> evidence: " + evidence);
            }
            //return new queryResult(resultQueryExpr, evidence);
            return new queryResult(finalQueryExpr, evidence);
            
        } catch (IOException e) {
            System.err.println("error: problem while sending service to remote server");
        }
        return new queryResult();
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

    //TODO: in utils
    public static String getSHA1(String file) {
        StringBuffer sb = new StringBuffer();
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[1024];
            int read = 0;
            while ((read = fis.read(data)) != -1) {
                sha1.update(data, 0, read);
            };
            byte[] hashBytes = sha1.digest();
            
            for (int i = 0; i < hashBytes.length; i++) {
                sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            }

        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sb.toString();
    }
    
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
    
    public static void sendFile(String filePath, DataOutputStream toServer) throws IOException {
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
    
    private void sendArgsToServer(List<String> serviceArgs, DataInputStream fromServerData, DataOutputStream toServerData) throws IOException {
        Iterator<String> queryIter = serviceArgs.iterator();
        int argCount = 1;
        while (queryIter.hasNext()) {
            String eachServiceArg = queryIter.next();
            System.out.println("\t\t -> arg" + argCount++ + " : " + eachServiceArg);
            List<String> eachServiceArgLS = Arrays.asList(eachServiceArg.split(" "));
            if (eachServiceArgLS.size() > 0 && eachServiceArgLS.get(0).equals("listIdent")) {
                if (eachServiceArgLS.get(1).contains("file(")) {
                    System.out.println("\t\t\t -> arg is a list of files");
                    toServerData.writeUTF("file_list");
                    for (int i=1; i < eachServiceArgLS.size(); i++) {
                        //TODO: a mixed list throws an exception
                        String eachFilePath = eachServiceArgLS.get(i).substring(5, eachServiceArgLS.get(i).length()-1);
                        System.out.println("\t\t\t -> file" + i + " : " + eachFilePath);
                        System.out.println("\t\t\t -> repoDirPath : " + repoDirPath);
                        
                        //if (utils.existsInRepo(eachFilePath, repoDirPath)) {
                        if (existsInRepo(eachFilePath)) {
                            System.out.println("\t\t\t\t -> file in client repo");
                            //sending file path and sha1
                            toServerData.writeUTF(eachFilePath);
                            //toServerData.writeUTF(getSHA1(eachFilePath));
                            String eachFilePath2 = repoDirPath + "/" + eachFilePath;
                            toServerData.writeUTF(getSHA1(eachFilePath2));
                            
                            //waiting for server's info on file status
                            String nextStep = fromServerData.readUTF();
                            System.out.println("\t\t\t\t -> next step : " + nextStep);
                            if (nextStep.equals("sendMeCopy")) {
                                //sendFile(eachFilePath, toServerData);
                                sendFile(eachFilePath2, toServerData);
                            }
                        }
                        else {
                            System.err.println("file NOT in client repo \u001B[31m(service execution is aborted)\u001B[30m");
                            System.exit(1);
                        }
                    }
                    toServerData.writeUTF("file_list_done");
                }
                else {
                    System.out.println("\t\t -> arg is a list of strings");
                    toServerData.writeUTF("string_list");
                    toServerData.writeUTF(eachServiceArg);
                }
            }
            
            else if (eachServiceArg.contains("file(")) {
                System.out.println("\t\t -> arg is a file");
                String filePath = eachServiceArg.substring(5, eachServiceArg.length()-1);
                System.out.println("\t\t\t -> file : " + filePath);
                toServerData.writeUTF("file");
                
                //if (utils.existsInRepo(filePath, repoDirPath)) {
                if (existsInRepo(filePath)) {
                    System.out.println("file in client repo");
                    //sending file path and sha1
                    toServerData.writeUTF(filePath);
                    
                    //toServerData.writeUTF(getSHA1(filePath));
                    String filePath2 = repoDirPath + "/" + filePath;
                    toServerData.writeUTF(getSHA1(filePath2));

                    //waiting for server's info on file status
                    String nextStep = fromServerData.readUTF();
                    System.out.println("next step : " + nextStep);
                    if (nextStep.equals("sendMeCopy")) {
                        //sendFile(filePath, toServerData);
                        sendFile(filePath2, toServerData);
                    }
                }
                else {
                    System.err.println("file NOT in client repo");
                    System.exit(1);
                }
            }
            else if (utils.isVariable(eachServiceArg)) {
                System.out.println("\t\t -> THIS IS A VAR TERM ");
                toServerData.writeUTF("VAR");
                toServerData.writeUTF(eachServiceArg);
            }
            else {
                System.out.println("\t\t -> THIS IS A GROUND TERM ");
                toServerData.writeUTF("string");
                toServerData.writeUTF(eachServiceArg);
            }
        }
        toServerData.writeUTF("done");
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
    
}

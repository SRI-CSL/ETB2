package etb.etbCS.utils;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import etb.etbDL.etbDatalog;
import etb.etbDL.etbDatalogEngine;
import etb.etbCS.etbNode;

import etb.etbDL.utils.*;
import etb.etbDL.output.*;

public class claimSpec {
    
    String repoDirPath;
    
    String ID;
    Expr query;
    Collection<Map<String, String>> answers = new ArrayList<>();
    ArrayList<String> SHA1List = new ArrayList();
    String derivationPath;
    
    String wfName;
    String wfSHA1;
    
    int status = 0;
    
    
    public claimSpec(Expr query, Collection<Map<String, String>> answers, String wfName, String wfSHA1) {
        this.query = query;
        this.ID = query.getPredicate() + query.getSignature() + query.getMode();
        this.answers = answers;
        this.wfName = wfName;
        this.wfSHA1 = wfSHA1;
    }
    
    public claimSpec(String repoDirPath, Expr query, Collection<Map<String, String>> answers, String wfName, String wfSHA1, String derivation) {
        this.repoDirPath = repoDirPath;
        this.query = query;
        this.ID = query.getPredicate() + query.getSignature() + query.getMode();
        this.answers = answers;
        generateSHA1(repoDirPath);
        setClaimDerivation(repoDirPath, derivation);
        this.wfName = wfName;
        this.wfSHA1 = wfSHA1;
    }
    
    public Collection<Map<String, String>> getQueryAnswers() {
        return answers;
    }
    
    public claimSpec(Expr query) {
        this.query = query;
        this.ID = query.getPredicate() + query.getSignature() + query.getMode();
    }

    public void addAnswer(Map<String, String> answer) {
        this.answers.add(answer);
    }
    
    public claimSpec(JSONObject claimSpecJSON) {
        
        this.ID = (String) claimSpecJSON.get("ID");
        
        String predicate = (String) claimSpecJSON.get("predicate");
        List<String> terms = new ArrayList();
        JSONArray argsJSON = (JSONArray) claimSpecJSON.get("args");
        Iterator<String> argsIter = argsJSON.iterator();
        while (argsIter.hasNext()) {
            terms.add((String) argsIter.next());
        }
        String signature = (String) claimSpecJSON.get("signature");
        String mode = (String) claimSpecJSON.get("mode");
        
        this.query = new Expr(predicate, terms, signature, mode);
        
        JSONArray answersJSON = (JSONArray) claimSpecJSON.get("answers");
        Iterator<JSONObject> answersIter = answersJSON.iterator();
        while (answersIter.hasNext()) {
            JSONObject eachAnswerJSON = (JSONObject) answersIter.next();
            Map<String, String> answerMap = new HashMap();
            for (Object key : eachAnswerJSON.keySet()) {
                String keyStr = (String) key;
                String keyvalue = (String) eachAnswerJSON.get(keyStr);
                answerMap.put(keyStr, keyvalue);
            }
            this.answers.add(answerMap);
        }
        
        JSONArray SHA1ListJSON = (JSONArray) claimSpecJSON.get("SHA1List");
        Iterator<String> SHA1ListIter = SHA1ListJSON.iterator();
        while (SHA1ListIter.hasNext()) {
            this.SHA1List.add((String) SHA1ListIter.next());
        }
        
        this.derivationPath = (String) claimSpecJSON.get("derivationPath");
        this.wfName = (String) claimSpecJSON.get("wfName");
        this.wfSHA1 = (String) claimSpecJSON.get("wfSHA1");
        
    }
    
    public JSONObject toJSONObject() {
        JSONObject NewObj = new JSONObject();

        NewObj.put("ID", this.ID);
        NewObj.put("predicate", query.getPredicate());
 
        List<String> terms = query.getTerms();
        JSONArray argsJSON = new JSONArray();
        for (int i=0; i<terms.size(); i++) {
            argsJSON.add(terms.get(i));
        }
        NewObj.put("args", argsJSON);
        NewObj.put("signature", query.getSignature());
        NewObj.put("mode", query.getMode());
        
        JSONArray answersJSON = new JSONArray();
        for (Map<String, String> answer : answers) {
            JSONObject eachAnswerJSON = new JSONObject();
            for (String key : answer.keySet()) {
                eachAnswerJSON.put(key, answer.get(key));
            }
            answersJSON.add(eachAnswerJSON);
        }
        
        NewObj.put("answers", answersJSON);
        
        JSONArray SHA1ListJSON = new JSONArray();
        for (int j=0; j<SHA1List.size(); j++) {
            SHA1ListJSON.add(SHA1List.get(j));
        }
        NewObj.put("SHA1List", SHA1ListJSON);
        NewObj.put("derivationPath", derivationPath);
        NewObj.put("wfName", wfName);
        NewObj.put("wfSHA1", wfSHA1);
        
        return NewObj;
        
    }
    
    public void generateSHA1(String repoDirPath) {
        SHA1List = new ArrayList();
        
        //System.out.println("query.getMode() : " + query.getMode());
        
        for (int i=0; i<query.getMode().length(); i++) {
            //System.out.println("current i : " + i);
            if (query.getMode().charAt(i) == '+') {
                //System.out.println("current interesting i (input i) : " + i);
                if (query.getSignature().charAt(i) == '2') {
                    //System.out.println("FILE");
                    String eachFilePath = query.getTerms().get(i);
                    eachFilePath = eachFilePath.substring(5, eachFilePath.length()-1);
                    //System.out.println("eachFilePath :" + eachFilePath);
                    SHA1List.add(utils.getSHA1(utils.getFilePathInDirectory(eachFilePath, repoDirPath)));
                }
                else if (query.getSignature().charAt(i) == '4') {
                    System.out.println("LIST OF FILES");
                    List<String> eachFileLS = Arrays.asList(query.getTerms().get(i).split(" "));
                    //eachFileLS.remove(0);
                    eachFileLS = eachFileLS.subList(1, eachFileLS.size());
                    String SHA1Str = "";
                    for (String eachFilePath : eachFileLS) {
                        eachFilePath = eachFilePath.substring(5, eachFilePath.length()-1);
                        //System.out.println("eachFilePath :" + eachFilePath);
                        SHA1Str += " " + utils.getSHA1(utils.getFilePathInDirectory(eachFilePath, repoDirPath));
                    }
                    SHA1List.add(SHA1Str);
                }
            }
        }
    }
    
    public void setClaimDerivation(String repoDirPath, String derivation) {
        File tempDir = new File(repoDirPath + "/claimDerivations");
        if (!tempDir.isDirectory()) {
            tempDir.mkdir();
        }
        this.derivationPath = repoDirPath + "/claimDerivations/deriv_" + ID + ".pl";
        try (PrintWriter out = new PrintWriter(this.derivationPath)) {
            out.println(derivation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getID() {
        return this.ID;
    }
    
    public void print() {
        query.print();
        QueryOutput qo = new DefaultQueryOutput();
        qo.writeResult2(answers);
        
        System.out.println("--> wfName : " + wfName);
        System.out.println("--> wfSHA1 : " + wfSHA1);
        
        System.out.println("--> SHA1List : " + SHA1List.toString());
        System.out.println("--> derivationPath : " + this.derivationPath);
        if (this.derivationPath != null) {
            System.out.println("--> derivation : ");
            try {
                BufferedReader br = new BufferedReader(new FileReader(derivationPath));
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println("\t" + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public int checkStatus(String repoDirPath, Map<String, workFlowSpec> workflows) {
        System.out.println("=> checking claim status (ID: " + ID + ")");
        for (int i=0; i<query.getMode().length(); i++) {
            if (query.getMode().charAt(i) == '+') {
                if (query.getSignature().charAt(i) == '2') {
                    String eachFilePath = query.getTerms().get(i).substring(5, query.getTerms().get(i).length()-1);
                    
                    if (utils.getSHA1(utils.getFilePathInDirectory(eachFilePath, repoDirPath)).equals(this.SHA1List.get(i))) {
                        
                    } else {
                        System.out.println("-> [\u001B[31minput change\u001B[30m] file@pos " + i + ": " + query.getTerms().get(i));
                        status = 1;
                    }
                }
                else if (query.getSignature().charAt(i) == '4') {
                    //TODO: for list of files
                    System.out.println("TODO:InfileList@pos " + i + " : " + query.getTerms().get(i));
                }
            }
        }
        
        if(workflows.containsKey(wfName)) {
            if(!workflows.get(wfName).getSHA1(repoDirPath).equals(wfSHA1)) {
                System.out.println("-> [\u001B[31mworkflow change\u001B[30m] a new version of workflow is found");
                status += 2;
            }
        }
        else {
            System.out.println("-> [\u001B[31mworkflow change\u001B[30m] workflow '" + wfName + "' does not exist anymore");
            status += 4;
        }
        
        return status;
    }
    
    //change of input (SUV) -- use existing derivation
    public void update(Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        try {
            etbDatalog dlPack = new etbDatalog();
            String wfScriptPath = workflows.get(wfName).getScriptPath();
            dlPack.parseToDatalog(wfScriptPath);
            dlPack.setGoal(query);
            etbDatalogEngine dlEngine = new etbDatalogEngine();
            Collection<Map<String, String>> updatedAnswers = dlEngine.run(etcSS, dlPack);//TODO: single claim derivation
            
            if (updatedAnswers == null) {
                System.out.println("-> \u001B[31mclaim update failed\u001B[30m");
            }
            else {
                System.out.println("-> claim updated successfully");
                this.answers = updatedAnswers;
                generateSHA1(repoDirPath);
            }
        }
        catch (DatalogException e) {
            e.printStackTrace();
        }
    }

    //change to the workflow -- new derivation for existing
    public void upgrade(Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        try {
            etbDatalog dlPack = new etbDatalog();
            String wfScriptPath = workflows.get(wfName).getScriptPath();
            dlPack.parseToDatalog(wfScriptPath);
            dlPack.setGoal(query);
            etbDatalogEngine dlEngine = new etbDatalogEngine();
            Collection<Map<String, String>> updatedAnswers = dlEngine.run(etcSS, dlPack);//TODO: single claim derivation
            
            if (updatedAnswers == null) {
                System.out.println("-> \u001B[31mclaim upgrade failed\u001B[30m");
            }
            else {
                System.out.println("-> claim upgrade successfully");
                this.answers = updatedAnswers;
                generateSHA1(repoDirPath);
                this.wfSHA1 = utils.getSHA1(utils.getFilePathInDirectory(wfScriptPath, repoDirPath));
            }
        }
        catch (DatalogException e) {
            e.printStackTrace();
        }
    }

    //TODO: workflow is removed -- new workflow
    public void recreate(Map<String, workFlowSpec> workflows, String repoDirPath, etbNode etcSS) {
        //grabbing a matching workflow(s)
        Set<String> wfNames = workflows.keySet();
        Iterator<String> wfNamesIter = wfNames.iterator();
        while (wfNamesIter.hasNext()) {
            this.wfName = wfNamesIter.next();
            if (workflows.get(wfName).containsQuery(query)) {
                try {
                    etbDatalog dlPack = new etbDatalog();
                    String wfScriptPath = workflows.get(wfName).getScriptPath();
                    dlPack.parseToDatalog(workflows.get(wfName).getScriptPath());
                    dlPack.setGoal(query);
                    etbDatalogEngine dlEngine = new etbDatalogEngine();
                    Collection<Map<String, String>> updatedAnswers = dlEngine.run(etcSS, dlPack);//TODO: single claim derivation
                    if (updatedAnswers == null) {
                        System.out.println("-> \u001B[31mclaim update failed\u001B[30m");
                    }
                    else {
                        System.out.println("-> claim updated successfully");
                        this.answers = updatedAnswers;
                        this.wfSHA1 = utils.getSHA1(utils.getFilePathInDirectory(wfScriptPath, repoDirPath));
                        generateSHA1(repoDirPath);
                        setClaimDerivation(repoDirPath, dlEngine.getDerivation());
                        break;
                    }
                }
                catch (DatalogException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }

}


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
    
    Expr query;
    Collection<Map<String, String>> answers = new ArrayList<>();
    int status = 0;
    
    Expr qClaim;
    String workFlowID;
    String workFlowSHA1;
    ArrayList<String> SHA1List = new ArrayList();
    List<Rule> derivRules = new ArrayList();
    List<Expr> derivFacts = new ArrayList();
    Map<String, serviceSpec> derivServices = new HashMap();
    
    String repoDirPath;
    
    public claimSpec(Expr query) {
        this.query = query;
    }
    
    public claimSpec(Expr query, Collection<Map<String, String>> answers, String workFlowID, String workFlowSHA1, String repoDirPath) {
        this.query = query;
        this.answers = answers;
        this.qClaim = query.substitute(answers.iterator().next());
        generateSHA1(repoDirPath);
        this.workFlowID = workFlowID;
        this.workFlowSHA1 = workFlowSHA1;
        this.repoDirPath = repoDirPath;
    }
    
    public claimSpec(JSONObject claimSpecJSON) {
        this.query = new Expr((JSONObject) claimSpecJSON.get("query"));
        this.qClaim = new Expr((JSONObject) claimSpecJSON.get("qClaim"));
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
        JSONArray derivRulesJSON = (JSONArray) claimSpecJSON.get("derivRules");
        Iterator<JSONObject> derivRulesIter = derivRulesJSON.iterator();
        while (derivRulesIter.hasNext()) {
            this.derivRules.add(new Rule((JSONObject) derivRulesIter.next()));
        }
        
        JSONArray derivFactsJSON = (JSONArray) claimSpecJSON.get("derivFacts");
        Iterator<JSONObject> derivFactsIter = derivFactsJSON.iterator();
        while (derivFactsIter.hasNext()) {
            this.derivFacts.add(new Expr((JSONObject) derivFactsIter.next()));
        }
        
        JSONArray derivServicesJSON = (JSONArray) claimSpecJSON.get("derivServices");
        Iterator<JSONObject> derivServicesIter = derivServicesJSON.iterator();
        while (derivServicesIter.hasNext()) {
            serviceSpec service = new serviceSpec((JSONObject) derivServicesIter.next());
            this.derivServices.put(service.getID(), service);
        }
        
        JSONArray SHA1ListJSON = (JSONArray) claimSpecJSON.get("SHA1List");
        Iterator<String> SHA1ListIter = SHA1ListJSON.iterator();
        while (SHA1ListIter.hasNext()) {
            this.SHA1List.add((String) SHA1ListIter.next());
        }
        this.workFlowID = (String) claimSpecJSON.get("workFlowID");
        this.workFlowSHA1 = (String) claimSpecJSON.get("workFlowSHA1");
        
    }
    
    public Collection<Map<String, String>> getQueryAnswers() {
        return answers;
    }
    
    public void setDerivationRules(List<Rule> derivRules) {
        this.derivRules = derivRules;
    }
    
    public void setDerivationFacts(List<Expr> derivFacts) {
        this.derivFacts = derivFacts;
    }
    
    public void setDerivationServices(Map<String, serviceSpec> derivServices) {
        this.derivServices = derivServices;
    }
    
    public void addAnswer(Map<String, String> answer) {
        this.answers.add(answer);
    }
    
    public JSONObject toJSONObject() {
        JSONObject NewObj = new JSONObject();
        NewObj.put("query", query.toJSONObject());
        NewObj.put("qClaim", qClaim.toJSONObject());
        
        JSONArray answersJSON = new JSONArray();
        for (Map<String, String> answer : answers) {
            JSONObject eachAnswerJSON = new JSONObject();
            for (String key : answer.keySet()) {
                eachAnswerJSON.put(key, answer.get(key));
            }
            answersJSON.add(eachAnswerJSON);
        }
        NewObj.put("answers", answersJSON);
        
        JSONArray derivRulesJSON = new JSONArray();
        for (Rule derivRule : derivRules) {
            derivRulesJSON.add(derivRule.toJSONObject());
        }
        NewObj.put("derivRules", derivRulesJSON);

        JSONArray derivFactsJSON = new JSONArray();
        for (Expr derivFact : derivFacts) {
            derivFactsJSON.add(derivFact.toJSONObject());
        }
        NewObj.put("derivFacts", derivFactsJSON);
        
        JSONArray derivServicesJSON = new JSONArray();
        for (String derivServiceKey : derivServices.keySet()) {
            derivServicesJSON.add(derivServices.get(derivServiceKey).toJSONObject());
        }
        NewObj.put("derivServices", derivServicesJSON);
        
        
        JSONArray SHA1ListJSON = new JSONArray();
        for (String SHA1str : SHA1List) {
            SHA1ListJSON.add(SHA1str);
        }
        NewObj.put("SHA1List", SHA1ListJSON);
        NewObj.put("workFlowID", workFlowID);
        NewObj.put("workFlowSHA1", workFlowSHA1);
        
        return NewObj;
        
    }
    
    public void generateSHA1(String repoDirPath) {
        SHA1List = new ArrayList();
        for (int i=0; i<query.getMode().length(); i++) {
            if (query.getMode().charAt(i) == '+') {
                if (query.getSignature().charAt(i) == '2') {
                    SHA1List.add(utils.getSHA1(utils.getFilePathInDirectory(
                                 utils.fromETBfile(query.getTerms().get(i)), repoDirPath)));
                }
                else if (query.getSignature().charAt(i) == '4') {
                    List<String> eachFileLS = Arrays.asList(query.getTerms().get(i).split(" "));
                    eachFileLS = eachFileLS.subList(1, eachFileLS.size());
                    
                    List<String> subSHA1List = new ArrayList();
                    subSHA1List = Arrays.asList(eachFileLS.stream().map(inFile -> utils.getSHA1(utils.getFilePathInDirectory(utils.fromETBfile(inFile), repoDirPath))).toArray(String[]::new));
                    SHA1List.add(String.join(" ", subSHA1List));
                }
            }
        }
    }
    
    public void writeDerivation(String FileName) {
        File tempDir = new File(repoDirPath + "/claimDerivations");
        if (!tempDir.isDirectory()) {
            tempDir.mkdir();
        }
        try (PrintWriter out = new PrintWriter(tempDir.getAbsolutePath() + "/" + FileName)) {
            derivRules.stream().forEach(rule -> out.println(rule.toString()));
            derivFacts.stream().forEach(fact -> out.println(fact.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //TODO: query or claim?
    public Integer getID() {
        return query.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n--> hashCode : " + query.hashCode());
        sb.append("\n--> qClaim : " + qClaim);
        sb.append("\n--> query : " + query.toString());
        sb.append("\n--> workFlowID : " + workFlowID);
        sb.append("\n==> derivRules : ");
        derivRules.stream().forEach(rule -> sb.append("\n---> " + rule));
        sb.append("\n==> derivFacts : ");
        derivFacts.stream().forEach(fact -> sb.append("\n---> " + fact));
        return sb.toString();
    }
    
    public void print() {
        System.out.println("--> hashCode : " + query.hashCode());
        System.out.println("--> query : " + query.toString());
        QueryOutput qo = new DefaultQueryOutput();
        qo.writeResult2(answers);
        System.out.println("--> workFlowID : " + workFlowID);
        System.out.println("=> derivRules : ");
        derivRules.stream().forEach(rule -> System.out.println("---> " + rule.toString()));
        System.out.println("=> derivFacts : ");
        derivFacts.stream().forEach(fact -> System.out.println("---> " + fact.toString()));
    }
    
    public List<String> getUpdatedServices(Map<String, serviceSpec> nodeServices) {
        List<String> updatedServiceIDs = new ArrayList();
        for (String derivServiceID : derivServices.keySet()) {
            String claimVersion = derivServices.get(derivServiceID).getVersion();
            String nodeVersion = nodeServices.get(derivServiceID).getVersion();
            if (!claimVersion.equals(nodeVersion)) {
                updatedServiceIDs.add(derivServiceID);
            }
        }
        return updatedServiceIDs;
    }
    
    public boolean suvUpdated(String repoDirPath) {
        for (int i=0; i<query.getMode().length(); i++) {
            if (query.getMode().charAt(i) == '+') {
                if (query.getSignature().charAt(i) == '2') {
                    if (!utils.getSHA1(utils.getFilePathInDirectory(utils.fromETBfile(query.getTerms().get(i)), repoDirPath)).equals(this.SHA1List.get(i))) {
                        
                        System.out.println("-> [\u001B[31minput change\u001B[30m] file@pos " + i + ": " + query.getTerms().get(i));
                        return true;
                    }
                }
                else if (query.getSignature().charAt(i) == '4') {
                    //TODO: for list of files
                    System.out.println("-> [\u001B[31mTODO:InfileList@pos " + i + " : \u001B[30m]" + query.getTerms().get(i));
                }
            }
        }
        return false;
    }
    
    public boolean workFlowUpdated(Map<String, workFlowSpec> workflows) {
        if (workflows.containsKey(workFlowID) && !(workflows.get(workFlowID).getScriptID(repoDirPath).equals(workFlowSHA1))) {
            System.out.println("-> [\u001B[31mworkflow change\u001B[30m] a new version of workflow is found");
            return true;
        }
        return false;
    }
    
    public boolean workFlowMissing(Map<String, workFlowSpec> workflows) {
        if(workflows.containsKey(workFlowID)) {
            return false;
        }
        else {
            System.out.println("-> [\u001B[31mworkflow missing\u001B[30m]: workflow '" + workFlowID + "' does not exist anymore");
            return true;
        }
    }
    
    public int checkStatus(servicePackage servicePack, String repoDirPath, Map<String, workFlowSpec> workflows) {
        System.out.println("=> checking claim status (query: " + query.toString() + ")");
        List<String> updatedClaimServiceIDs = getUpdatedServices(servicePack.getServices());        
        if (updatedClaimServiceIDs.size() > 0) {
            status++;
            System.out.println("-> [\u001B[31mservice change\u001B[30m] updated services : " + updatedClaimServiceIDs.toString());
        }
        
        boolean suvChange = false;
        for (int i=0; i<query.getMode().length(); i++) {
            if (query.getMode().charAt(i) == '+') {
                if (query.getSignature().charAt(i) == '2') {
                    if (utils.getSHA1(utils.getFilePathInDirectory(utils.fromETBfile(query.getTerms().get(i)), repoDirPath)).equals(this.SHA1List.get(i))) {
                    } else {
                        System.out.println("-> [\u001B[31minput change\u001B[30m] file@pos " + i + ": " + query.getTerms().get(i));
                        suvChange = true;
                    }
                }
                else if (query.getSignature().charAt(i) == '4') {
                    //TODO: for list of files
                    System.out.println("-> [\u001B[31mTODO:InfileList@pos " + i + " : \u001B[30m]" + query.getTerms().get(i));
                }
            }
        }
        
        if (suvChange)
            status+=2;
        
        if(workflows.containsKey(workFlowID)) {
            if(!workflows.get(workFlowID).getScriptID(repoDirPath).equals(workFlowSHA1)) {
                System.out.println("-> [\u001B[31mworkflow change\u001B[30m] a new version of workflow is found");
                status += 4;
            }
        }
        else {
            System.out.println("-> [\u001B[31mworkflow change\u001B[30m] workflow '" + workFlowID + "' does not exist anymore");
            status += 8;
        }
        return status;
    }

    //change of analysis service (facts) -- use existing derivation
    public void update(servicePackage servicePack, etbNode etcSS) {
        System.out.println("=> maintaining claim for (query: " + query.toString() + ")");
        List<String> recUpdatedServiceIDs = getUpdatedServices(servicePack.getServices());
        System.out.println("-> updated services : " + recUpdatedServiceIDs.toString());
        recUpdatedServiceIDs.stream().forEach(serviceID -> this.derivServices.remove(serviceID));
        
        List<Expr> reUsedFacts = new ArrayList();
        List<Expr> unAffectedFacts = derivFacts;
        
        while (!recUpdatedServiceIDs.isEmpty()) {
            String recUpdatedService = recUpdatedServiceIDs.remove(0);
            for (Rule derivRule : derivRules) {
                if (derivRule.inBody(recUpdatedService)) {
                    int index = derivRule.indexOf(recUpdatedService);
                    //dependency map for the rule
                    List<String> ruleDepends = derivRule.getDependents(derivFacts);
                    //dependency for the updated predicate/service
                    String predDepends0 = ruleDepends.get(index);
                    ArrayList<Integer> predDepends = new ArrayList(Arrays.asList(Arrays.asList(predDepends0.split(" ")).stream().map(dependStr -> Integer.parseInt(dependStr)).toArray(Integer[]::new)));
                    predDepends.add(index);
                    for (int i=0; i<derivRule.getBody().size(); i++) {
                        Expr targetBodyExpr = derivRule.getBody().get(i);
                        List<Expr> factsBodyExpr = Arrays.asList(derivFacts.stream().filter(derivFact ->
                                derivFact.unify(targetBodyExpr, new HashMap())).toArray(Expr[]::new));
                        if (predDepends.contains(i)) {
                            unAffectedFacts.removeAll(factsBodyExpr);
                        }
                        else {
                            reUsedFacts.addAll(factsBodyExpr);
                        }
                    }
                    //adding head of the rule to the impacted service
                    recUpdatedServiceIDs.add(derivRule.getHead().getPredicate());
                }
            }
        }
        unAffectedFacts.remove(qClaim);
        maintain2(reUsedFacts, etcSS, unAffectedFacts);
    }

    public void maintain2(List<Expr> reUsedFacts, etbNode etcSS, List<Expr> unAffectedFacts) {
        //creating a datalog instance with just required rules and facts
        etbDatalog dlPack = new etbDatalog(derivRules, reUsedFacts);
        etbDatalogEngine dlEngine = new etbDatalogEngine(query);
        Collection<Map<String, String>> refAnswers;
        if ((refAnswers = dlEngine.run(etcSS, dlPack))!= null) {
            this.qClaim = query.substitute(refAnswers.iterator().next());//TODO: just one derivation?
            List<Expr> refinedFacts = dlEngine.getDerivationFacts();
            refinedFacts.removeAll(reUsedFacts);
            unAffectedFacts.addAll(refinedFacts);
            this.derivFacts = unAffectedFacts;
            //adding updated service to the set of claim derivation services
            
            //dlEngine.getDerivationServices().keySet().stream().forEach(serviceID -> );
            for(String ss: dlEngine.getDerivationServices().keySet())
                this.derivServices.put(ss, dlEngine.getDerivationServices().get(ss));
            System.out.println("=> claim maintained successfully");
        }
        else {
            System.out.println("=> \u001B[31mclaim mantainance not successful\u001B[30m ()");
        }
    }

}


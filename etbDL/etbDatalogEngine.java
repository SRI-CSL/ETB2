package etb.etbDL;

import java.util.*;

import etb.etbDL.engine.IndexedSet;
import etb.etbDL.utils.*;
import etb.etbDL.output.OutputUtils;
import etb.etbCS.etbNode;
import etb.etbCS.utils.queryResult;

import java.util.stream.Collectors;

public class etbDatalogEngine {
    Set<goalNode> goals = new HashSet();
    Set<clauseNode> clauses = new HashSet();
    int index;
    
    boolean foundNewClaim = false, foundNewClause = false, foundNewGoal = false, goalUpdated = false;
    String derivation = "";
    
    private void backChain() {
        Iterator<clauseNode> clauseIter = clauses.iterator();
        while (clauseIter.hasNext()) {
            clauseNode clNode = clauseIter.next();
            if (clNode.getClause().getBody().size() > 0 && clNode.getSubGoal() == null) {
                boolean goalNodeExists = false;
                Iterator<goalNode> goalIter = goals.iterator();
                while (goalIter.hasNext()) {
                    goalNode gNode = goalIter.next();
                    if (gNode.getLiteral().litEquals(clNode.getClause().getBody().get(0))) {// goal with specific literal exists
                        gNode.addNodeToParents(clNode);
                        foundNewClause = true;
                        goalUpdated = true;
                        clNode.setSubGoal(gNode);
                        clNode.setSubGoalIndex(0);
                        goalNodeExists = true;
                        break;
                    }
                }
                if (!goalNodeExists) {//such goal doesn't exist, create a new one
                    goals.add(new goalNode(index+1, clNode.getClause().getBody().get(0), "open", clNode));
                    foundNewGoal = true;
                    this.index++;
                }
            }
        }
    }
    
    private void resolve(etbNode etcSS, etbDatalog dlPack) {
        Iterator<goalNode> goalIter = goals.iterator();
        while (goalIter.hasNext()) {
            goalNode gNode = goalIter.next();
            if (gNode.getStatus().equals("open")) {
                //facts are rules with empty body
                System.out.println("---------------------------");
                System.out.println("=> goal being resolved: " + gNode.getLiteral().toString());
                IndexedSet<Expr, String> goalFacts = new IndexedSet<>();
                goalFacts.addAll(dlPack.getExtDB().getFacts(gNode.getLiteral().getPredicate()));
                int i=1;
                for (Expr fact : goalFacts) {
                    System.out.println("\t -> fact " + i + " : " + fact.toString());
                    Map<String, String> locBindings = new HashMap();
                    if(fact.unify(gNode.getLiteral(), locBindings)) {
                        derivation += fact.toString() + ".\n";
                        System.out.println("\t    bindings: " + OutputUtils.bindingsToString(locBindings));
                        clauses.add(new clauseNode(new Rule(fact, new ArrayList()), gNode, getPredEvidence(fact)));
                        foundNewClause = true;
                    }
                    i++;
                }
                if (!foundNewClause) {
                    System.out.println("\t -> no matching facts");
                }
                //adding useful rules
                Collection<String> goalAsSet = Collections.singletonList(gNode.getLiteral().getPredicate());
                Collection<Rule> goalRules = dlPack.getIntDB().stream().filter(rule -> goalAsSet.contains(rule.getHead().getPredicate())).collect(Collectors.toSet());
                i=1;
                boolean rulesFound = false;
                for (Rule rule : goalRules) {
                    System.out.println("\t -> rule " + i + " : " + rule.toString());
                    Map<String, String> locBindings = new HashMap();
                    rule.getHead().unify(gNode.getLiteral(), locBindings);
                    System.out.println("\t    bindings: " + OutputUtils.bindingsToString(locBindings));
                    if (locBindings.size() > 0) {
                        derivation += rule.toString() + ".\n";
                        System.out.println("\t    rule after substitution : " + rule.substitute(locBindings).toString());
                        clauses.add(new clauseNode(rule.substitute(locBindings), gNode));
                        foundNewClause = true;
                        rulesFound = true;
                    }
                    i++;
                }
                if (!rulesFound) {
                    System.out.println("\t -> no matching rules");
                }
                
                //external tools
                queryResult qr = etcSS.processQuery(gNode.getLiteral().getPredicate(), gNode.getLiteral().getTerms());
                if (qr.getResultExpr() != null) {
                    Expr toolInvResult = qr.getResultExpr();
                    derivation += toolInvResult.toString() + ".\n";
                    clauses.add(new clauseNode(new Rule(toolInvResult, new ArrayList()), gNode, qr.getEvidence()));
                    foundNewClause = true;
                }
                else {
                    System.out.println("\t -> no matching external tools");
                }
                //checking if resolved
                if (foundNewClause) {
                    System.out.println("=> goal successfully resolved");
                }
                else {
                    System.out.println("** no facts, no rules, and no external services supporting goal predicate");
                    System.out.println("\u001B[31m\t[datalog engine backtracks]\u001B[30m");
                }
                gNode.setStatus("resolved");
            }
        }
    }

    public String getDerivation() {
        return derivation;
    }
    
    private String getPredEvidence(Expr fact) {
        return "{serviceName: " + fact.getPredicate() + ", serviceParams: " + fact.getTerms().toString() + ", serviceType : pred}";
    }
    
    private void propagate() {
        Iterator<goalNode> goalIter = goals.iterator();
        while (goalIter.hasNext()) {
            goalNode gNode = goalIter.next();
            Set<clauseNode> parents = gNode.getParents();
            Iterator<clauseNode> parentIter = parents.iterator();
            while (parentIter.hasNext()) {
                clauseNode parentNode = parentIter.next();
                if (parentNode.getSubGoalIndex() < gNode.getClaims().size()) {
                    Expr claimClause = gNode.getClaims().get(parentNode.getSubGoalIndex());
                    String newEvidence = gNode.getEvidence(claimClause); //copy claim evidence into the new clause evidence
                    Rule parentNodeClause = parentNode.getClause();
                    if (!(parentNode.getEvidence() == null)) {
                        newEvidence = "{" + parentNode.getEvidence() + ", " + newEvidence + "}"; //TODO: advnaced evidence composition
                    }
                    Map<String, String> locBindings = new HashMap();
                    claimClause.unify(parentNodeClause.getBody().get(0), locBindings);
                    List<Expr> parentClauseBody = parentNodeClause.getBody();
                    Rule redClause = new Rule(parentNodeClause.getHead(), parentClauseBody.subList(1, parentClauseBody.size()));
                    clauseNode redParentNode = new clauseNode(redClause.substitute(locBindings), parentNode.getGoal(), newEvidence);
                    clauses.add(redParentNode);
                    foundNewClause = true;
                    parentNode.addToSubClauses(redParentNode);
                    parentNode.incrementSubGoalIndex();
                }
            }
        }
    }
    
    private void claim() {
        Iterator<clauseNode> clauseIter = clauses.iterator();
        while (clauseIter.hasNext()) {
            clauseNode clNode = clauseIter.next();
            if (clNode.getClause().getBody().size() == 0){
                Expr clHead = clNode.getClause().getHead();
                if (!clNode.getGoal().getClaims().contains(clHead)) {
                    //clNode.getGoal().addClaims(clHead);
                    clNode.getGoal().addClaims(clHead, clNode.getEvidence());
                    goalUpdated = true;
                    //clNode.getGoal().printClaims();
                }
            }
        }
    }
    
    //runs the DL engine over input rules and facts in the DL suit
    public Collection<Map<String, String>> run(etbNode etcSS, etbDatalog dlPack) {
        
        this.goals.add(new goalNode(0, dlPack.getGoal(), "open")); //initial goal instantiation
        this.foundNewGoal = true;
        this.index = 1;
        
        do {
            foundNewGoal = false;
            resolve(etcSS, dlPack);
            
            if (foundNewClause) {
                do {
                    foundNewClause = false;
                    goalUpdated = false;
                    backChain();
                    claim();
                    
                    if (goalUpdated) {
                        propagate();
                    }
                    else {
                        break;
                    }
                    
                } while (foundNewClause);
            }
        } while (foundNewGoal);

        Expr mainGoal = dlPack.getGoal();
        //grabs goals and their corresponding claims
        Collection<Map<String, String>> answers = new ArrayList<>();
        Iterator<goalNode> goalIter = goals.iterator();
        while (goalIter.hasNext()) {
            goalNode gNode = goalIter.next();
            if (gNode.getLiteral().litEquals(mainGoal)) {// grabbing the main goal
                ArrayList<Expr> gClaims = gNode.getClaims();
                gNode.printClaims();
                for (int i=0; i < gClaims.size(); i++) {
                    Map<String, String> newBindings = new HashMap<String, String>();
                    if(mainGoal.unify(gClaims.get(i), newBindings)) {
                        answers.add(newBindings);
                    }
                }
                
                if (gClaims.size() == 0) {
                    return null; 
                }
                break;
            }
        }
        
        //refining the main goal
        Collection<Map<String, String>> refAnswers = new ArrayList<>();
        List<String> goalTerms = mainGoal.getTerms();
        IndexedSet<Expr,String> newFacts = new IndexedSet<>();
        
        for (Map<String, String> answer : answers) {
            if (!newFacts.contains(mainGoal.substitute(answer))) {
                Map<String, String> projAnswer = new HashMap();
                Set<String> bindKeySet = answer.keySet();
                Iterator<String> it = bindKeySet.iterator();
                while( it.hasNext()) {
                    String bindKey = it.next();
                    if (goalTerms.contains(bindKey))
                        projAnswer.put(bindKey, answer.get(bindKey));
                }
                newFacts.add(mainGoal.substitute(answer)); // the local delta
                refAnswers.add(projAnswer);
            }
        }
        return refAnswers;
        
    }
    
}

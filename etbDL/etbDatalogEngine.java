package etb.etbDL;

import java.util.*;

import etb.etbDL.engine.IndexedSet;
import etb.etbDL.utils.*;
import etb.etbDL.output.OutputUtils;
import etb.etbCS.etbNode;
import etb.etbCS.utils.*;

import java.util.stream.Collectors;

public class etbDatalogEngine {
    Expr mainGoal;
    Set<goalNode> goals = new HashSet();
    Set<clauseNode> clauses = new HashSet();
    int index;
    boolean foundNewClaim = false;
    boolean foundNewClause = false;
    boolean foundNewGoal = false;
    boolean goalUpdated = false;
    List<Rule> derivRules = new ArrayList();
    List<Expr> derivFacts = new ArrayList();
    Map<String, serviceSpec> derivServices = new HashMap();
    
    public etbDatalogEngine(Expr mainGoal) {
        this.mainGoal = mainGoal;
        //initial goal instantiation
        this.goals.add(new goalNode(0, mainGoal, "open"));
        this.foundNewGoal = true;
        //termination for the abstract machine
        this.index = 1;
    }

    public etbDatalogEngine() {}

    private void backChain() {
        Iterator<clauseNode> clauseIter = clauses.iterator();
        while (clauseIter.hasNext()) {
            clauseNode clNode = clauseIter.next();
            //if the clause's subgoal is not yet set of processing
            if (clNode.getClause().getBody().size() > 0 && clNode.getSubGoal() == null) {
                boolean goalNodeExists = false;
                Iterator<goalNode> goalIter = goals.iterator();
                //check if a goal node exists for the subgoal
                while (goalIter.hasNext()) {
                    goalNode gNode = goalIter.next();
                    // goal with specific literal exists
                    if (gNode.getLiteral().litEquals(clNode.getClause().getBody().get(0))) {
                        gNode.addNodeToParents(clNode);
                        foundNewClause = true;
                        goalUpdated = true;
                        clNode.setSubGoal(gNode);
                        //clNode.setSubGoalIndex(0);
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
    
    private void claim() {
        Iterator<clauseNode> clauseIter = clauses.iterator();
        while (clauseIter.hasNext()) {
            clauseNode clNode = clauseIter.next();
            if (clNode.getClause().getBody().size() == 0){
                Expr clHead = clNode.getClause().getHead();
                if (!clNode.getGoal().getClaims().contains(clHead)) {
                    clNode.getGoal().addClaims(clHead, clNode.getEvidence());
                    goalUpdated = true;
                }
            }
        }
    }

    private boolean resolve(etbNode node, etbDatalog dlPack) {
        Iterator<goalNode> goalIter = goals.iterator();
        while (goalIter.hasNext()) {
            goalNode gNode = goalIter.next();
            if (gNode.getStatus().equals("open")) {
                System.out.println("---------------------------");
                System.out.println("=> goal being resolved: " + gNode.getLiteral());
                IndexedSet<Expr, String> goalFacts = new IndexedSet<>();
                goalFacts.addAll(dlPack.getExtDB().getFacts(gNode.getLiteral().getPredicate()));
                int i=1;
                for (Expr fact : goalFacts) {
                    System.out.println("\t -> fact " + i + " : " + fact.toString());
                    Map<String, String> locBindings = new HashMap();
                    if(fact.unify(gNode.getLiteral(), locBindings)) {
                        System.out.println("\t    bindings: " + OutputUtils.bindingsToString(locBindings));
                        clauses.add(new clauseNode(new Rule(fact, new ArrayList()), gNode, getPredEvidence(fact)));
                        foundNewClause = true;
                    }
                    i++;
                }
                //TODO: multiple derivation turned off -- do we need them?
                if (foundNewClause) {
                    System.out.println("=> goal successfully resolved");
                    gNode.setStatus("resolved");
                    break; //TODO: added for claim update
                }
                 else {
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
                        derivRules.add(rule);
                        Rule subtRule = rule.substitute(locBindings);
                        System.out.println("\t    rule after substitution : " + subtRule.toString());
                        clauses.add(new clauseNode(subtRule, gNode));
                        foundNewClause = true;
                        rulesFound = true;
                    }
                    i++;
                }
                if (!rulesFound) {
                    System.out.println("\t -> no matching rules");
                }
                
                serviceInvocation inv = new serviceInvocation(gNode.getLiteral());
                inv.process(node);
                if (inv.getResult() != null) {
                    String serviceName = gNode.getLiteral().getPredicate();
                    if (inv.isLocal()) {
                        derivServices.put(serviceName, node.getServicePack().get(serviceName));
                    }
                    clauses.add(new clauseNode(new Rule(inv.getResult(), new ArrayList()), gNode, inv.getEvidence()));
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
                    return false;
                }
                gNode.setStatus("resolved");
            }
        }
        return true;
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
                    //copy claim evidence into the new clause evidence
                    String newEvidence = gNode.getEvidence(claimClause);
                    Rule parentNodeClause = parentNode.getClause();
                    if (!(parentNode.getEvidence() == null)) {
                        //TODO: advnaced evidence composition
                        newEvidence = "{" + parentNode.getEvidence() + ", " + newEvidence + "}";
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
    
    //runs the DL engine over input rules and facts in the DL suit
    public Collection<Map<String, String>> run(etbNode etcSS, etbDatalog dlPack) {
        do {
            foundNewGoal = false;
            //resolve(etcSS, dlPack);
            if (!resolve(etcSS, dlPack)) {
                return null;
            }
            
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

        Iterator<goalNode> goalIter00 = goals.iterator();
        while (goalIter00.hasNext()) {
            goalNode gNode = goalIter00.next();
            Expr derivFact = gNode.getClaim();
            derivFact.setMode(gNode.getLiteral().getMode());
            derivFacts.add(derivFact);
        }
        //grabs goals and their corresponding claims
        Collection<Map<String, String>> answers = new ArrayList<>();
        Iterator<goalNode> goalIter = goals.iterator();
        while (goalIter.hasNext()) {
            goalNode gNode = goalIter.next();
            if (gNode.getLiteral().litEquals(mainGoal)) {// grabbing the main goal
                ArrayList<Expr> gClaims = gNode.getClaims();
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
    
    public List<Rule> getDerivationRules() {
        return derivRules;
    }
    
    public List<Expr> getDerivationFacts() {
        return derivFacts;
    }
    
    public Map<String, serviceSpec> getDerivationServices() {
        return derivServices;
    }
    
    private String getPredEvidence(Expr fact) {
        return "{serviceName: " + fact.getPredicate() + ", serviceParams: " + fact.getTerms().toString() + ", serviceType : pred}";
    }
    
    
}

package etb.etbDL.utils;

import java.util.HashSet;
import java.util.Set;

public class clauseNode {
    
    Rule clause;
    goalNode goal;
    goalNode subGoal = null;
    int subGoalIndex = 0;
    Set<clauseNode> subClauses = new HashSet();
    String evidence = null;
    
    public clauseNode() {}
    
    public clauseNode(Rule clause, String evidence) {//to be used in the glue code
        this.clause = clause;
        this.evidence = evidence;
    }
    
    public clauseNode(Rule clause, goalNode goal) {
        this.clause = clause;
        this.goal = goal;
    }
    
    public clauseNode(Rule clause, goalNode goal, String evidence) {
        this.clause = clause;
        this.goal = goal;
        this.evidence = evidence;
    }
    
    public void setSubGoal(goalNode gNode){
        this.subGoal = gNode;
        this.subGoalIndex = 0;
    }
    
    public goalNode getSubGoal(){
        return this.subGoal; 
    }
    /*
    public void setSubGoalIndex(int subGoalIndex) {
        this.subGoalIndex = subGoalIndex;
    }
    */
    public int getSubGoalIndex() {
        return this.subGoalIndex;
    }
    
    public void incrementSubGoalIndex() {
        this.subGoalIndex++;
    }
    
    public Rule getClause() {
        return this.clause;
    }
    
    public goalNode getGoal() {
        return this.goal;
    }
    
    public void addToSubClauses(clauseNode newNode) {
        this.subClauses.add(newNode);
    }
    
    public void composeEvidence(String moreEvidence) {
        this.evidence += " : " + moreEvidence;
    }
    
    public String getEvidence() {
        return this.evidence; 
    }
    
    public void print() {
        System.out.println("clause : " + clause.toString());
    }

}

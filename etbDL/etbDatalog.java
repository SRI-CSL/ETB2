package etb.etbDL;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import etb.etbDL.statements.*;
import etb.etbDL.utils.*;

public class etbDatalog {

    Collection<Rule> intDB = new ArrayList<>();
    extDataBaseSuit extDB = new extDataBaseSuit();
    
    public etbDatalog() {}

    public etbDatalog(List<Rule> rules, List<Expr> facts) {
        for(Rule rule : rules)
            add(rule); //addRule(rule);
        for(Expr fact : facts)
            add(fact); //addFact(fact);
    }

    public void validate() throws DatalogException {
        for(Rule rule : intDB) {
            rule.validate();
        }
        for (Expr fact : extDB.allFacts()) {
			fact.validFact();
		}
    }
    
    public void add(Rule newRule) {
        try {
            newRule.validate();
            intDB.add(newRule);
        } catch (DatalogException e) {
            e.printStackTrace();
        }
    }

    public void add(Expr newFact) {
        if(!newFact.isGround()) {
            //throw new DatalogException("Facts must be ground: " + newFact);
            DatalogException e = new DatalogException("Facts must be ground: " + newFact);
            e.printStackTrace();
        }
        if(newFact.isNegated()) {
            //throw new DatalogException("Facts cannot be negated: " + newFact);
            DatalogException e = new DatalogException("Facts cannot be negated: " + newFact);
            e.printStackTrace();
        }
        //TODO: matching arity against existing facts
        extDB.add(newFact);
    }
    
    @Override
    public String toString() {
        // The output of this method should be parseable again and produce an exact replica of the database
        StringBuilder sb = new StringBuilder("% Facts:\n");
        for(Expr fact : extDB.allFacts()) {
            sb.append(fact).append(".\n");
        }
        sb.append("\n% Rules:\n");
        for(Rule rule : intDB) {
            sb.append(rule).append(".\n");
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof etbDatalog)) {
            return false;
        }
        etbDatalog that = ((etbDatalog) obj);
        if(this.intDB.size() != that.intDB.size()) {
            return false;
        }
        for(Rule rule : intDB) {
            if(!that.intDB.contains(rule))
                return false;
        }
        
        Collection<Expr> theseFacts = this.extDB.allFacts();
        Collection<Expr> thoseFacts = that.extDB.allFacts();
        
        if(theseFacts.size() != thoseFacts.size()) {
            return false;
        }
        for(Expr fact : theseFacts) {
            if(!thoseFacts.contains(fact))
                return false;
        }
        return true;
    }
    
    public extDataBaseSuit getExtDB() {
            return extDB;
    }
    
    public void setExtDB(extDataBaseSuit extDB) {
        this.extDB = extDB;
    }
    
    public Collection<Rule> getIntDB() {
        return intDB;
    }
    
}

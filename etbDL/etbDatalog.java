package etb.etbDL;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import etb.etbDL.statements.*;
import etb.etbDL.utils.*;

public class etbDatalog {

    Expr goal;
    Collection<Rule> intDB = new ArrayList<>();
    extDataBaseSuit extDB = new extDataBaseSuit();
    
    private static StreamTokenizer getTokenizer(Reader reader) throws IOException {
		StreamTokenizer scan = new StreamTokenizer(reader);
		scan.ordinaryChar('.'); // assumed number by default
		scan.commentChar('%'); // % comments will be ignored
		scan.quoteChar('"');
		scan.quoteChar('\'');
		return scan;
	}
    
    /*
    public void parseToDatalog(String scriptFile) throws DatalogException {
        try {
            Reader reader = new BufferedReader(new FileReader(scriptFile));
            StreamTokenizer scan = getTokenizer(reader);
            scan.nextToken();
            while(scan.ttype != StreamTokenizer.TT_EOF) {
                scan.pushBack();
                //each line being parsed and a corresponding statement constructed
                etbDLStatement statement = etbDLParser.parseStmt(scan);
                try {
                    statement.addTo(this);
                } catch (DatalogException e) {
                    throw new DatalogException("[line " + scan.lineno() + "] Error executing statement", e);
                }
                scan.nextToken();
            }
        } catch (IOException e) {
            throw new DatalogException(e);
        }
        
    }
    */
    public void parseToDatalog(String scriptFile, String repoDirPath) throws DatalogException {
        try {
            Reader reader = new BufferedReader(new FileReader(scriptFile));
            StreamTokenizer scan = getTokenizer(reader);
            scan.nextToken();
            while(scan.ttype != StreamTokenizer.TT_EOF) {
                scan.pushBack();
                //each line being parsed and a corresponding statement constructed
                etbDLStatement statement = etbDLParser.parseStmt(scan, repoDirPath);
                try {
                    statement.addTo(this);
                } catch (DatalogException e) {
                    throw new DatalogException("[line " + scan.lineno() + "] Error executing statement", e);
                }
                scan.nextToken();
            }
        } catch (IOException e) {
            throw new DatalogException(e);
        }
        
    }
    
    
    public void validate() throws DatalogException {
        for(Rule rule : intDB) {
            rule.validate();
        }
        for (Expr fact : extDB.allFacts()) {
			fact.validFact();
		}
    }
    
    public void setGoal(Expr goal) {
        this.goal = goal;
    }
    
    public void addRule(Rule newRule) throws DatalogException {
        newRule.validate();
        intDB.add(newRule);
    }

    public void addFact(Expr newFact) throws DatalogException {
        if(!newFact.isGround()) {
            throw new DatalogException("Facts must be ground: " + newFact);
        }
        if(newFact.isNegated()) {
            throw new DatalogException("Facts cannot be negated: " + newFact);
        }
        //TODO: matching arity against existing facts
        extDB.add(newFact);
        //return this;
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
    
    public Expr getGoal() {
        return goal;
    }
    
}

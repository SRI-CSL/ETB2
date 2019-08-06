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
    
    public etbDatalog() {
        
    }

    public etbDatalog(List<Rule> rules, List<Expr> facts) {
        for(Rule rule : rules)
            addRule(rule);
        for(Expr fact : facts)
            addFact(fact);
    }

    private static StreamTokenizer getTokenizer(Reader reader) throws IOException {
		StreamTokenizer scan = new StreamTokenizer(reader);
		scan.ordinaryChar('.'); // assumed number by default
		scan.commentChar('%'); // % comments will be ignored
		scan.quoteChar('"');
		scan.quoteChar('\'');
		return scan;
	}
    
    public void parseDatalogScript(String scriptFile, String repoDirPath) {
        try {
            Reader reader = new BufferedReader(new FileReader(scriptFile));
            StreamTokenizer scan = getTokenizer(reader);
            scan.nextToken();
            while(scan.ttype != StreamTokenizer.TT_EOF) {
                scan.pushBack();
                //each line being parsed and a corresponding statement constructed
                try {
                    etbDLStatement statement = etbDLParser.parseStmt(scan, repoDirPath);
                    statement.addTo(this);
                } catch (DatalogException e) {
                    System.out.println("[line " + scan.lineno() + "] Error executing statement");
                    e.printStackTrace();
                }
                scan.nextToken();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    
    public void addRule(Rule newRule) {
        try {
            newRule.validate();
            intDB.add(newRule);
        } catch (DatalogException e) {
            e.printStackTrace();
        }
    }

    public void addFact(Expr newFact) {
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
    
}

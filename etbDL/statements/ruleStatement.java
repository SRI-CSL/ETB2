package etb.etbDL.statements;

import java.util.Collection;
import java.util.Map;

import etb.etbDL.etbDatalog;
import etb.etbDL.utils.DatalogException;
import etb.etbDL.utils.Rule;

public class ruleStatement implements etbDLStatement {
	
	private final Rule rule;
	
	public ruleStatement(Rule rule) {
		this.rule = rule;
	}
    
    @Override
    public void addTo(etbDatalog datalog) throws DatalogException {
        //datalog.addRule(rule);
        datalog.add(rule);
    }    
}

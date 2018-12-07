package etb.etbDL.statements;

import java.util.List;
import etb.etbDL.etbDatalog;
import etb.etbDL.utils.Expr;
import etb.etbDL.utils.Rule;

/**
 * Provides factory methods for building Statement instances for
 * use with the fluent API.
 * <p>
 * {@link etbDatalog#prepareStatement(String)} can be used to parse
 * Strings to statement object.
 * </p>
 * @see Statement
 * @see Statement#execute(etbDatalog, java.util.Map)
 * @see etbDatalog#prepareStatement(String)
 */
public class etbDLStatementFactory {
	
	/**
	 * Creates a statement to query the database.
	 * @param goals The goals of the query
	 * @return A statement that will query the database for the given goals.
	 */
    public static etbDLStatement getQueryStatement(Expr goal) {
        return new queryStatement(goal);
    }
    
	/**
	 * Creates a statement that will insert a fact into the EDB.
	 * @param fact The fact to insert
	 * @return A statement that will insert the given fact into the database.
	 */
	public static etbDLStatement getFactStatement(Expr fact) {
		return new factStatement(fact);
	}
	
	/**
	 * Creates a statement that will insert a rule into the IDB.
	 * @param rule The rule to insert
	 * @return A statement that will insert the given rule into the database.
	 */
	public static etbDLStatement getRuleStatement(Rule rule) {
		return new ruleStatement(rule);
	}
}

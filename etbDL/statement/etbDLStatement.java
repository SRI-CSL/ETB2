package etb.etbDL.statements;

import java.util.Collection;
import java.util.Map;

import etb.etbDL.etbDatalog;
import etb.etbDL.utils.DatalogException;
import etb.etbDL.output.OutputUtils;

/**
 * constructs a statement that can be run against a etbDatalog database.
 * <p>
 * There are 3 types of statements: to insert facts, to insert rules, and to query the database.
 * </p><p>
 * Instances of Statement are created by {@link StatementFactory}.
 * </p><p>
 * Strings can be parsed to Statements through {@link etbDatalog#prepareStatement(String)}
 * </p>
 * @see StatementFactory
 * @see etbDatalog#prepareStatement(String)
 */
public interface etbDLStatement {
	
	/**
	 * Executes a statement against a etbDatalog database.
	 * @param datalog The database against which to execute the statement.
	 * @param bindings an optional (nullable) mapping of variables to values.
	 * <p>
	 * A statement like "a(B,C)?" with bindings {@code <B = "foo", C = "bar">}
	 * is equivalent to the statement "a(foo,bar)?"
	 * </p> 
	 * @return The result of the statement.
     * <ul>
	 * <li> If null, the statement was an insert or delete that didn't produce query results.
	 * <li> If empty the query's answer is "No."
	 * <li> If a list of empty maps, then answer is "Yes."
	 * <li> Otherwise it is a list of all bindings that satisfy the query.
	 * </ul>
	 * etbDatalog provides a {@link OutputUtils#answersToString(Collection)} method that can convert answers to 
	 * Strings
	 * @throws DatalogException if an error occurs in processing the statement
	 * @see OutputUtils#answersToString(Collection)
	 */
    public void addTo(etbDatalog datalog) throws DatalogException;
    
	/**
	 * Shorthand for {@code statement.execute(etbDatalog, null)}.
	 * @param datalog The database against which to execute the statement.
	 * @return The result of the statement
	 * @throws DatalogException if an error occurs in processing the statement
	 * @see #execute(etbDatalog, Map)
	 */

}

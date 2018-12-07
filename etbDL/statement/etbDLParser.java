//package etb.etbDL;
package etb.etbDL.statements;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.io.File;

//import etb.etbDL.statements.*;
//import etb.etbDL.statement.StatementFactory;
import etb.etbDL.utils.*;

/**
 * Internal class that encapsulates the parser for the Datalog language.
 */
public class etbDLParser {
    
    /* Parses a Datalog statement.
     * A statement can be:
     * - a fact, like parent(alice, bob).
     * - a rule, like ancestor(A, B) :- ancestor(A, C), parent(C, B).
     * - a query, like ancestor(X, bob)?
     */
    public static etbDLStatement parseStmt(StreamTokenizer scan) throws DatalogException {
        try {
            Expr head = parseExpr(scan);
    
            if(scan.nextToken() == ':') {// dealing with a rule
                if(scan.nextToken() != '-') {
                    throw new DatalogException("[line " + scan.lineno() + "] expected ':-'");
                }
                List<Expr> body = new ArrayList<>();
                do { //parses each expression (pred or builtin) of the body of the rule
                    Expr arg = parseExpr(scan);
                    body.add(arg);
                } while(scan.nextToken() == ','); // body preds must be separated by comma
                
                if(scan.ttype != '.') { //rule must end with a dot
                    throw new DatalogException("[line " + scan.lineno() + "] expected '.' after rule");
                }
                Rule newRule = new Rule(head, body);
                return etbDLStatementFactory.getRuleStatement(newRule); //inserting to the DB
            }
            else {
                if(scan.ttype == '.') {//fact
                    return etbDLStatementFactory.getFactStatement(head); //inserting to the DB
                }
                else if (scan.ttype == '?') {//query
                    return etbDLStatementFactory.getQueryStatement(head);//inserting to the DB
                }
                else {
                    throw new DatalogException("[line " + scan.lineno() + "] unexpected symbol of type '" + scan.ttype + "' found");
                }
            }
        } catch (IOException e) {
            throw new DatalogException(e);
        }
    }
    
    // parses an expression
    private static Expr parseExpr(StreamTokenizer scan) throws DatalogException, IOException {
        boolean negated = false, builtInExpected = false;
        String lhs = null;
        
        scan.nextToken();
        if(scan.ttype == StreamTokenizer.TT_WORD && scan.sval.equalsIgnoreCase("not")) { //a negated expression
            negated = true;
            scan.nextToken();
        }
        
        if(scan.ttype == StreamTokenizer.TT_WORD) {
            lhs = readComplexTerm(scan.sval, scan);
        }
        else if(scan.ttype == '"' || scan.ttype == '\'') {
            lhs = scan.sval;
            builtInExpected = true;
        }
        else if(scan.ttype == StreamTokenizer.TT_NUMBER) {
            lhs = numberToString(scan.nval);
            builtInExpected = true;
        }
        else {
            throw new DatalogException("[line " + scan.lineno() + "] predicate or start of expression expected");
        }
        
        scan.nextToken(); //TB: moving forward to get the operator and rhs of the expression)
        if(scan.ttype == StreamTokenizer.TT_WORD || scan.ttype == '=' || scan.ttype == '!' || scan.ttype == '<' || scan.ttype == '>') {//to take care of built-ins??
            scan.pushBack();
            Expr e = parseBuiltInPredicate(lhs, scan);
            e.negated = negated;
            return e;
        }
        
        if(builtInExpected) {// LHS was a number or a quoted string but we didn't get an operator
            throw new DatalogException("[line " + scan.lineno() + "] built-in predicate expected");
        } else if(scan.ttype != '(') {// LHS was a predicate/operator but no operand is found
            throw new DatalogException("[line " + scan.lineno() + "] expected '(' after predicate or an operator");
        }
        
        //non-builtin operator (i.e., predicate) and next scan is '('... diving into args of predicate)
        List<String> terms = new ArrayList<>();
        if(scan.nextToken() != ')') {
            scan.pushBack();
            terms = getPredicateTerms(scan);
            if(scan.ttype != ')') {
                throw new DatalogException("[line " + scan.lineno() + "] expected ')'");
            }
        }
        
        Expr e = new Expr(lhs, terms);
        e.negated = negated;
        return e;
    }
    
    private static List<String> getPredicateTerms(StreamTokenizer scan) throws IOException, DatalogException {
        //not builtin operator (i.e., predicate) and next scan is '('... diving into args of predicate)
        List<String> terms = new ArrayList<>();
        do {
            if(scan.nextToken() == StreamTokenizer.TT_WORD) {// a word
                terms.add(readComplexTerm(scan.sval, scan));
            }
            else if(scan.ttype == '"' || scan.ttype == '\'') {//TODO: separate handling of single and double quotes
                String xxx = scan.sval;
                if (xxx.contains("/") || xxx.contains(".")) {
                    terms.add("file(" + xxx + ")");
                }
                else {
                    File file = new File(xxx);
                    //String filePath = file.getCanonicalPath();
                    //System.out.println("filePath: " + filePath);
                    if (file.exists()) {//a file variable with no / and .
                        terms.add("file(" + xxx + ")");
                    }
                    else {//normal variable
                        System.out.println("a valid non-file string");
                    }
                }
            }
            else if(scan.ttype == StreamTokenizer.TT_NUMBER) {// a number
                terms.add(numberToString(scan.nval));
            }
            else if(scan.ttype == '[') {// a list
                
                List<String> listTerms = getPredicateTerms(scan);
                if(scan.ttype != ']') {
                    throw new DatalogException("[line " + scan.lineno() + "] list is expected to end with ']'");
                }
                
                Iterator<String> iterator = listTerms.iterator();
                String listStr = "listIdent";
                while (iterator.hasNext()) {
                    listStr += " " + iterator.next();
                }
                terms.add(listStr);
            }
            else {
                throw new DatalogException("[line " + scan.lineno() + "] '" + scan.sval + "' is not a valid ETB data type");
            }
        } while(scan.nextToken() == ',');
        return terms;
    }
    
    //makes sure complex terms with underscore, e.g., 'inv_1_main', are parsed
    private static String readComplexTerm(String initTerm, StreamTokenizer scan) throws DatalogException, IOException {
        String compTerm = initTerm;
        if(scan.nextToken() == '_') {
            do {
                if(scan.nextToken() == StreamTokenizer.TT_WORD) {
                    compTerm += "_" + scan.sval;
                } else if(scan.ttype == StreamTokenizer.TT_NUMBER) {
                    compTerm += "_" + numberToString(scan.nval);
                } else {
                    throw new DatalogException("[line " + scan.lineno() + "] expected a string/number type but found: " + scan.ttype);
                }
            } while(scan.nextToken() == '_');
        }
        scan.pushBack();
        return compTerm;
    }
    
    private static final List<String> validOperators = Arrays.asList(new String[] {"=", "!=", "<>", "<", "<=", ">", ">="});
    
    /* parses builtin arithmetic expressions, like X >= Y, which is internally represented as ETBDL expression
     where the operator is considered as a predicate and the operands are considered as its terms,
     e.g., >=(X, Y) */
    private static Expr parseBuiltInPredicate(String lhs, StreamTokenizer scan) throws DatalogException, IOException {
        String operator;
        scan.nextToken();
        if(scan.ttype == StreamTokenizer.TT_WORD) {
            operator = scan.sval;
        } else {// <, >, =, !
            operator = Character.toString((char)scan.ttype);
            scan.nextToken();
            if(scan.ttype == '=' || scan.ttype == '>') {//TB: != or <>
                operator = operator + Character.toString((char)scan.ttype);
            } else { // TB: the rest of single char operators
                scan.pushBack();
            }
        }
        
        if(!validOperators.contains(operator)) {
            throw new DatalogException("invalid operator '" + operator + "'");
        }
        
        scan.nextToken(); //move on to the rhs of the expression
        String rhs = null;
        if(scan.ttype == StreamTokenizer.TT_WORD) {
            rhs = scan.sval;
        } else if(scan.ttype == '"' || scan.ttype == '\'') {
            rhs = scan.sval;
        } else if(scan.ttype == StreamTokenizer.TT_NUMBER) {
            rhs = numberToString(scan.nval);
        } else {
            throw new DatalogException("[line " + scan.lineno() + "] right hand side of an expression expected");
        }
        return new Expr(operator, lhs, rhs); //parsed expression with builtin operator
    }
    
    /* Converts a number to a string - The StreamTokenizer returns numbers as doubles by default
     * so we need to convert them back to strings to store them in the expressions */
    private static String numberToString(double nval) {
        // Remove trailing zeros; http://stackoverflow.com/a/14126736/115589
        if(nval == (long) nval)
            return String.format("%d",(long)nval);
        else
            return String.format("%s",nval);
    }
    
    private static final Pattern numberPattern = Pattern.compile("[+-]?\\d+(\\.\\d*)?([Ee][+-]?\\d+)?");
    
    /* Checks, via regex, if a String can be parsed as a Double */
    public static boolean tryParseDouble(String str) {
        return numberPattern.matcher(str).matches();
    }
}

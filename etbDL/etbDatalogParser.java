package etb.etbDL.statements;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.*;

import etb.etbDL.utils.*;
import etb.etbDL.etbDatalog;

/**
 * Internal class that encapsulates the parser for the Datalog language.
 */
public class etbDLParser {
    
    public static etbDatalog parseDatalogScript(String scriptFile, String repoDirPath) {
        etbDatalog etbDL = new etbDatalog();
        try {
            Reader reader = new BufferedReader(new FileReader(scriptFile));
            StreamTokenizer scan = getTokenizer(reader);
            scan.nextToken();
            while(scan.ttype != StreamTokenizer.TT_EOF) {
                scan.pushBack();
                try {
                    //parses each line to a corresponding statement
                    etbDLStatement statement = etbDLParser.parseStmt(scan, repoDirPath);
                    statement.addTo(etbDL);
                } catch (DatalogException e) {
                    System.out.println("[line " + scan.lineno() + "] error parsing statement");
                    e.printStackTrace();
                    return null;
                }
                scan.nextToken();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return etbDL;
    }
    
    private static StreamTokenizer getTokenizer(Reader reader) throws IOException {
        StreamTokenizer scan = new StreamTokenizer(reader);
        scan.ordinaryChar('.'); // assumed number by default
        scan.commentChar('%'); // % comments will be ignored
        scan.quoteChar('"');
        scan.quoteChar('\'');
        return scan;
    }

    /* Parses a Datalog statement.
     * A statement can be:
     * - a fact, e.g., parent(alice, bob).
     * - a rule, e.g., ancestor(A, B) :- ancestor(A, C), parent(C, B).
     * - a query, e.g., ancestor(X, bob)?
     */
    public static etbDLStatement parseStmt(StreamTokenizer scan, String repoDirPath) throws DatalogException {
        try {
            Expr head = parseExpr(scan, repoDirPath);
            if(scan.nextToken() == ':') {
                //parsing a RULE (whose HEAD already parsed)
                if(scan.nextToken() != '-') {
                    throw new DatalogException("[line " + scan.lineno() + "] expected ':-'");
                }
                List<Expr> body = new ArrayList<>();
                do {
                    //parsing each PREDICATE (uninterpreted or interpreted) in the BODY
                    Expr arg = parseExpr(scan, repoDirPath);
                    body.add(arg);
                } while(scan.nextToken() == ',');
                // BODY preds must be separated by comma
                
                if(scan.ttype != '.') { //RULE must end with a dot
                    throw new DatalogException("[line " + scan.lineno() + "] expected '.' after rule");
                }
                Rule newRule = new Rule(head, body);
                //constructing a DL statement of type RULE (using statementFactory utility)
                return etbDLStatementFactory.getRuleStatement(newRule);
            }
            else {
                if(scan.ttype == '.') {//parsing FACT
                    //constructing a DL statement of type FACT
                    return etbDLStatementFactory.getFactStatement(head);
                }
                else if (scan.ttype == '?') {//parsing QUERY
                    //constructing a DL statement of type QUERY
                    return etbDLStatementFactory.getQueryStatement(head);
                }
                else {
                    throw new DatalogException("[line " + scan.lineno() + "] unexpected symbol of type '" + scan.ttype + "' found");
                }
            }
        } catch (IOException e) {
            throw new DatalogException(e);
        }
    }
    //parses a PREDICATE (uninterpreted or interpreted)
    public static Expr parseExpr(StreamTokenizer scan, String repoDirPath) throws DatalogException, IOException {
        boolean negated = false, builtInExpected = false;
        String lhs = null;
        
        scan.nextToken();
        if(scan.ttype == StreamTokenizer.TT_WORD && scan.sval.equalsIgnoreCase("not")) {
            //a negated PREDICATE
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
        if(scan.ttype == StreamTokenizer.TT_WORD || scan.ttype == '=' || scan.ttype == '!' || scan.ttype == '<' || scan.ttype == '>') {
            //to take care of built-ins
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
            terms = getPredicateTerms(scan, repoDirPath);
            if(scan.ttype != ')') {
                throw new DatalogException("[line " + scan.lineno() + "] expected ')'");
            }
        }
        
        Expr e = new Expr(lhs, terms, terms.remove(terms.size()-2), terms.remove(terms.size()-1));
        e.negated = negated;
        return e;
    }
    
    private static List<String> getPredicateTerms(StreamTokenizer scan, String repoDirPath) throws IOException, DatalogException {
        //not builtin operator (i.e., predicate) and next scan is '('... diving into args of predicate)
        List<String> terms = new ArrayList<>();
        String signature = "", mode = "";
        do {
            if(scan.nextToken() == StreamTokenizer.TT_WORD) {
                //checking string type
                String wordTerm = readComplexTerm(scan.sval, scan);
                terms.add(wordTerm);
                signature += "1";
                if (Character.isUpperCase(wordTerm.charAt(0)))
                    mode += "-";
                else
                    mode += "+";
            }
            else if(scan.ttype == '"' || scan.ttype == '\'') {
                //TODO: separate handling of single and double quotes
                String restScan = scan.sval;
                if (restScan.contains("/") || restScan.contains(".")) {
                    //checking file type
                    String filePath;
                    if ((filePath = utils.getFilePathInDirectory(restScan, repoDirPath)).equals(null)) {
                        throw new DatalogException("[line " + scan.lineno() + "] a non-valid file path " + restScan + "']'");
                    }
                    else {
                        terms.add("file(" + filePath + ")");
                        signature += "2";
                        mode += "+";
                    }
                }
                else {
                    File file = new File(restScan);
                    if (file.exists()) {
                        //TODO: a file variable with no / and .??
                        terms.add("file(" + restScan + ")"); //TODO: get full path
                        signature += "2";
                        mode += "+";
                    }
                    else {//normal variable
                        //TODO: does this really happen?
                        signature += "1";
                        if (Character.isUpperCase(restScan.charAt(0)))
                            mode += "-";
                        else
                            mode += "+";
                        terms.add(restScan);
                    }
                }
            }
            else if(scan.ttype == StreamTokenizer.TT_NUMBER) {// a number
                terms.add(numberToString(scan.nval));
                signature += "1";
                mode += "+";
            }
            else if(scan.ttype == '[') {// a list
                List<String> listTerms = getPredicateTerms(scan, repoDirPath);
                if(scan.ttype != ']') {
                    throw new DatalogException("[line " + scan.lineno() + "] list is expected to end with ']'");
                }
                String signature2 = listTerms.remove(listTerms.size()-2);
                String mode2 = listTerms.remove(listTerms.size()-1);
                //TODO: efficient/better logic
                mode += "+";
                if (listTerms.get(0).contains("file(")) {
                    signature += "4";
                }
                else {
                    signature += "3";
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
        terms.add(signature);
        terms.add(mode);
        return terms;
    }
    
    //parses complex terms with underscore, e.g., 'inv_1_main'
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
            if(scan.ttype == '=' || scan.ttype == '>') {
                // operator is != or <>
                operator += Character.toString((char)scan.ttype);
            } else {
                // operator is is of single char, e.g., < or >
                scan.pushBack();
            }
        }
        
        if(!validOperators.contains(operator)) {
            throw new DatalogException("invalid operator '" + operator + "'");
        }
        
        //move on to the rhs of the expression
        scan.nextToken();
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
    
}

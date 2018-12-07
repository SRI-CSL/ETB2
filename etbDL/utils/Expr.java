package etb.etbDL.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import etb.etbDL.engine.Indexable;
import etb.etbDL.statements.etbDLParser;

//represents a Datalog literal, which is a predicate followed by zero or more terms, e.g., pred(term1, term2, term3...).
//number of terms is the expression's <i>arity</i>.

public class Expr implements Indexable<String> {

    private String predicate;
    private List<String> terms;
    
    //public ArrayList<Integer> listPositions = new ArrayList();
    public boolean negated = false;

    public Expr(String predicate, List<String> terms) {
        this.predicate = predicate;
        if(this.predicate.equals("!=")) { //convert to "<>" internally for simplicity
            this.predicate = "<>";
        }
        this.terms = terms;
    }

    public Expr(String predicate, String... terms) {
        this(predicate, Arrays.asList(terms));
    }

    public int arity() {
        return terms.size();
    }

    public boolean isGround() {
        for(String term : terms) {
            if(utils.isVariable(term))
                return false;
        }
        return true;
    }

    public boolean isNegated() {
        return negated;
    }

    //checks if the expression represents a supported built-in predicates (=, <>, <, <=, >, >=)
    //engine calls evalBuiltIn(Map) for such predicates, rather than unifying it against the goals.
    public boolean isBuiltIn() {
        char op = predicate.charAt(0);
        return !Character.isLetterOrDigit(op) && op != '\"';
    }

    /**
     * Unifies the expression with another expression.
     * bindings represents the bindings of variables to values after unification
     * returns true if the expressions unify.
     */
    
    public boolean unifyOLD(Expr that, Map<String, String> bindings) {
        if(!this.predicate.equals(that.predicate) || this.arity() != that.arity()) {
            return false;
        }
        for(int i = 0; i < this.arity(); i++) {
            String term1 = this.terms.get(i);
            String term2 = that.terms.get(i);
            if(utils.isVariable(term1)) {
                if(!term1.equals(term2)) {
                    if(!bindings.containsKey(term1)) {
                        bindings.put(term1, term2);
                    } else if (!bindings.get(term1).equals(term2)) {
                        return false;
                    }
                }
            } else if(utils.isVariable(term2)) {
                if(!bindings.containsKey(term2)) {
                    bindings.put(term2, term1);
                } else if (!bindings.get(term2).equals(term1)) {
                    return false;
                }
            } else if (!term1.equals(term2)) {
                return false;
            }
        }
        return true;
    }

    public boolean unify(Expr that, Map<String, String> bindings) {
        if(!this.predicate.equals(that.predicate) || this.arity() != that.arity()) {
            return false;
        }
        
        for (int i = 0; i < this.arity(); i++) {
            
            String term1 = this.terms.get(i), term2 = that.terms.get(i);
            ArrayList<String> listTerms1 = new ArrayList(Arrays.asList(term1.split(" ")));
            
            if (listTerms1.size() > 1 && listTerms1.get(0).equals("listIdent")) { //term1 is a list
                //System.out.println("list term1 : " + term1.toString());
                ArrayList<String> listTerms2 = new ArrayList(Arrays.asList(term2.split(" ")));
                
                if(listTerms2.size() > 1 && listTerms2.get(0).equals("listIdent")) { //term2 is a list
                    //System.out.println("list term2 : " + term2.toString());
                    if (listTerms1.size() != listTerms2.size()) {
                        return false;
                    }
                    for (int j = 1; j < listTerms1.size(); j++) {
                        if (!unifyTerms(listTerms1.get(j), listTerms2.get(j), bindings)) {
                            return false;
                        }
                    }
                }
                
                else if(utils.isVariable(term2)) {
                    
                    if(bindings.containsKey(term2)) {
                        return false; //TODO: further refinements if current binding unifies with the list term1
                    } else {
                        bindings.put(term2, term1);
                    }
                }
                else {//term2 is neither a list or a var, i.e., a ground variable
                    return false;
                }
            }
            
            else if(utils.isVariable(term1)) {//term1 is a variable
            
                ArrayList<String> listTerms2 = new ArrayList(Arrays.asList(term2.split(" ")));
                if(listTerms2.size() > 1 && listTerms2.get(0).equals("listIdent")) { // term2 is a list
                    if(bindings.containsKey(term1)) {
                        return false; //TODO: further refinements if current binding unifies with the list term2
                    } else {
                        bindings.put(term1, term2);
                    }
                }
                else if(!term1.equals(term2)) {
                    if(!bindings.containsKey(term1)) {
                        bindings.put(term1, term2);
                    } else if (!bindings.get(term1).equals(term2)) {
                        return false;
                    }
                }
            }
            
            else { //term1 is neither a list nor a variable, i.e., a ground variable
                ArrayList<String> listTerms2 = new ArrayList(Arrays.asList(term2.split(" ")));
                if (listTerms2.size() > 1 && listTerms2.get(0).equals("listIdent")) { // term2 is a list
                    return false;
                }
                else if(utils.isVariable(term2)) {
                    if(!bindings.containsKey(term2)) {
                        bindings.put(term2, term1);
                    }
                    else if (!bindings.get(term2).equals(term1)) {
                        return false;
                    }
                }
                else if (!term1.equals(term2)) {//term2 is neither a list nor a variable, i.e., a ground variable
                    return false;
                }
            }
        }
        return true;
    }

    private boolean unifyTerms(String term1, String term2, Map<String, String> bindings) {
        if(utils.isVariable(term1)) {
            if(!term1.equals(term2)) {
                if(!bindings.containsKey(term1)) {
                    bindings.put(term1, term2);
                } else if (!bindings.get(term1).equals(term2)) {
                    return false;
                }
            }
        }
        else if(utils.isVariable(term2)) {
            if(!bindings.containsKey(term2)) {
                bindings.put(term2, term1);
            } else if (!bindings.get(term2).equals(term1)) {
                return false;
            }
        }
        else if (!term1.equals(term2)) {
            return false;
        }
        return true;
    }
    
    /**
     * Substitutes the variables in this expression with bindings from a unification.
     * @param bindings The bindings to substitute.
     * @return A new expression with the variables replaced with the values in bindings.
     */
    public Expr substitute(Map<String, String> bindings) {
        
        Expr that = new Expr(this.predicate, new ArrayList<>());
        that.negated = negated;
        
        int i=0;
        for(String term : this.terms) {
        
            String value = "";
            if(utils.isVariable(term)) {//TB: try to sub unbound vars
                value = bindings.get(term); //TB: get value from corresponding variable term in the binding
                if(value == null) {
                    value = term; //TB: leave it as it is if value is NULL
                }
            }
            
            else {
                ArrayList<String> listTerms = new ArrayList(Arrays.asList(term.split(" ")));
                if (listTerms.size() > 1) {// a list variable to be substituted
                    value = "listIdent";
                    String listValue;
                    for(int j = 1; j < listTerms.size(); j++) {
                        if(utils.isVariable(listTerms.get(j))) {
                            listValue = bindings.get(listTerms.get(j));
                            if(listValue == null) {
                                listValue = listTerms.get(j);
                            }
                            value += " " + listValue;
                        }
                        
                        else {
                            value += " " + listTerms.get(j);
                        }
                    }
                    //value = "listIdent " + value;
                }
                else { //constant
                    value = term;
                }
            }
            
            that.terms.add(value);
            i++;
        }
        return that;
    }

    //evaluates a built-in predicate by taking a map of variable bindings
    //returns true if the operator matched
    public boolean evalBuiltIn(Map<String, String> bindings) {
    	// This method may throw a RuntimeException for a variety of possible reasons, but 
    	// these conditions are supposed to have been caught earlier in the chain by 
    	// methods such as Rule#validate().
    	// The RuntimeException is a requirement of using the Streams API.
    	String term1 = terms.get(0);
        if(utils.isVariable(term1) && bindings.containsKey(term1))
            term1 = bindings.get(term1);
        String term2 = terms.get(1);
        if(utils.isVariable(term2) && bindings.containsKey(term2))
            term2 = bindings.get(term2);
        
        if(predicate.equals("=")) {// '=' is special
            if(utils.isVariable(term1)) {
                if(utils.isVariable(term2)) {
                	// Rule#validate() was supposed to catch this condition
                    throw new RuntimeException("Both operands of '=' are unbound (" + term1 + ", " + term2 + ") in evaluation of " + this);
                }
                bindings.put(term1, term2);
                return true;
            } else if(utils.isVariable(term2)) {
                bindings.put(term2, term1);
                return true;
            } else {
				if (etbDLParser.tryParseDouble(term1) && etbDLParser.tryParseDouble(term2)) {
					double d1 = Double.parseDouble(term1);
					double d2 = Double.parseDouble(term2);
					return d1 == d2; //both numbers
				} else {
					return term1.equals(term2); //at least one is not a number
				}
            }
        } else {
            try {
            	// These errors can be detected in the validate method:
                if(utils.isVariable(term1) || utils.isVariable(term2)) {
                	// Rule#validate() was supposed to catch this condition
                	throw new RuntimeException("Unbound variable in evaluation of " + this);
                }
                
                if(predicate.equals("<>")) {
                    // '<>' is also a bit special
                    if(etbDLParser.tryParseDouble(term1) && etbDLParser.tryParseDouble(term2)) {
                            double d1 = Double.parseDouble(term1);
                            double d2 = Double.parseDouble(term2);
                            return d1 != d2;
                    } else {
                        return !term1.equals(term2);
                    }
                } else {
                    // Ordinary comparison operator
                	// If the term doesn't parse to a double it gets treated as 0.0.
                	double d1 = 0.0, d2 = 0.0;
                    if(etbDLParser.tryParseDouble(term1)) {
                    	d1 = Double.parseDouble(term1);
                    }
                    if(etbDLParser.tryParseDouble(term2)) {
                    	d2 = Double.parseDouble(term2);
                    }
                    switch(predicate) {
                        case "<": return d1 < d2;
                        case "<=": return d1 <= d2;
                        case ">": return d1 > d2;
                        case ">=": return d1 >= d2;
                    }
                }
            } catch (NumberFormatException e) {
                // You found a way to write a double in a way that the regex in tryParseDouble() doesn't understand.
                throw new RuntimeException("tryParseDouble() experienced a false positive!?", e);
            }
        }
        throw new RuntimeException("Unimplemented built-in predicate " + predicate);
    }
    
    public String getPredicate() {
		return predicate;
	}
    
    public List<String> getTerms() {
		return terms;
	}

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof Expr)) {
            return false;
        }
        Expr that = ((Expr) other);
        if(!this.predicate.equals(that.predicate)) {
            return false;
        }
        if(arity() != that.arity() || negated != that.negated) {
            return false;
        }
        for(int i = 0; i < terms.size(); i++) {
            if(!terms.get(i).equals(that.terms.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = predicate.hashCode();
        for(String term : terms) {
            hash += term.hashCode();
        }
        return hash;
    }

    //converts Expr to string
    //@Override
    public String toStringOLD() {
        StringBuilder sb = new StringBuilder();
        if(isNegated()) {
            sb.append("not ");
        }
        if(isBuiltIn()) {
            termToString(sb, terms.get(0));
            sb.append(" ").append(predicate).append(" ");
            termToString(sb, terms.get(1));
        } else {
            sb.append(predicate).append('(');
            for(int i = 0; i < terms.size(); i++) {
                String term = terms.get(i);
                termToString(sb, term);
                if(i < terms.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(isNegated()) {
            sb.append("not ");
        }
        if(isBuiltIn()) {
            termToString(sb, terms.get(0));
            sb.append(" ").append(predicate).append(" ");
            termToString(sb, terms.get(1));
        } else {
            sb.append(predicate).append('(');
            for(int i = 0; i < terms.size(); i++) {
                
                ArrayList<String> listTerms = new ArrayList(Arrays.asList(terms.get(i).split(" ")));
                if (listTerms.size() > 1) {
                    sb.append('[');
                    for(int j = 1; j < listTerms.size(); j++) {
                        String term = listTerms.get(j);
                        termToString(sb, term);
                        if(j < listTerms.size() - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append(']');

                }
                else {
                    String term = terms.get(i);
                    termToString(sb, term);
                }
                
                if(i < terms.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

    
    /* Converts a term to a string. If it started as a quoted string it is now enclosed in quotes,
     * and other quotes escaped.
     * caveat: You're going to have trouble if you have other special characters in your strings */
    private static StringBuilder termToString(StringBuilder sb, String term) {
        if(term.startsWith("\""))
            sb.append('"').append(term.substring(1).replaceAll("\"", "\\\\\"")).append('"');
        else
            sb.append(term);
        return sb; //is it needed at all??
    }

	//helper method for creating a new expression (part of the fluent API)
	public static Expr expr(String predicate, String... terms) {
		return new Expr(predicate, terms);
	}

    //helper method for constructing negated expressions of the form 'not predicate(term1, term2,...).
    public static Expr not(String predicate, String... terms) {
        Expr e = new Expr(predicate, terms);
        e.negated = true;
        return e;
    }
    
    public static Expr eq(String a, String b) {
        return new Expr("=", a, b);
    }
    
    public static Expr ne(String a, String b) {
        return new Expr("<>", a, b);
    }
    
    public static Expr lt(String a, String b) {
        return new Expr("<", a, b);
    }
    
    public static Expr le(String a, String b) {
        return new Expr("<=", a, b);
    }
    
    public static Expr gt(String a, String b) {
        return new Expr(">", a, b);
    }
    
    public static Expr ge(String a, String b) {
        return new Expr(">=", a, b);
    }

	@Override
	public String index() {		
		return predicate;
	}

	//checks if this is a valid fact in the IDB to be ground and non-negative
	public void validFact() throws DatalogException {
		if(!isGround()) {
            throw new DatalogException("Fact " + this + " is not ground");
        } else if(isNegated()) {
            throw new DatalogException("Fact " + this + " is negated");
        }
	}
    
    //TB: for handling query equality
    public boolean litEquals(Expr that) {
        if(!this.predicate.equals(that.predicate)) {
            return false;
        }
        if(arity() != that.arity() || negated != that.negated) {
            return false;
        }
        for(int i = 0; i < terms.size(); i++) {
            if(!utils.isVariable(terms.get(i)) && !utils.isVariable(that.getTerms().get(i)) && !terms.get(i).equals(that.terms.get(i))) {
                return false;
            }
        }
        return true;
    }

    
}

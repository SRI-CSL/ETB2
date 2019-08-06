package etb.etbDL.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

//represents an ETB datalog rule
public class Rule {
	private Expr head;
	private List<Expr> body = new ArrayList();
    //to handle in-body dependency
    
	//constructor may reorder expressions in the body to evaluate rules correctly
	public Rule(Expr head, List<Expr> body) {
		this.setHead(head);
		//this.setBody(Engine.reorderQuery(body)); //ETB-DL strict left to right -- avoiding re-ordering
        this.setBody(body);
	}

	//constructor that allows a variable number of body expressions given directly
	public Rule(Expr head, Expr... body) {
		this(head, Arrays.asList(body));
	}
    
    public Rule(JSONObject ruleJSON) {
        this.head = new Expr((JSONObject) ruleJSON.get("head"));
        
        JSONArray bodyJSON = (JSONArray) ruleJSON.get("body");
        Iterator<JSONObject> bodyIter = bodyJSON.iterator();
        while (bodyIter.hasNext()) {
            this.body.add(new Expr((JSONObject) bodyIter.next()));
        }
        /*
        JSONArray dependentsJSON = (JSONArray) ruleJSON.get("dependents");
        Iterator<String> dependentsIter = dependentsJSON.iterator();
        while (dependentsIter.hasNext()) {
            this.dependents.add((String) dependentsIter.next());
        }
        */
    }


	/**
	 * Checks whether a rule is valid.
	 * There are a variety of reasons why a rule may not be valid:
	 * <ul>
	 * <li> Each variable in the head of the rule <i>must</i> appear in the body.
	 * <li> Each variable in the body of a rule should appear at least once in a positive (that is non-negated) expression.
	 * <li> Variables that are used in built-in predicates must appear at least once in a positive expression.
	 * </ul>
	 * @throws DatalogException if the rule is not valid, with the reason in the message.
	 */
	public void validate() throws DatalogException {
        /*
		// Check for /safety/: each variable in the body of a rule should appear at least once in a positive expression,
		// to prevent infinite results. E.g. p(X) :- not q(X, Y) is unsafe because there are an infinite number of values
		// for Y that satisfies `not q`. This is a requirement for negation - [gree] contains a nice description.
		// We also leave out variables from the built-in predicates because variables must be bound to be able to compare
		// them, i.e. a rule like `s(A, B) :- r(A,B), A > X` is invalid ('=' is an exception because it can bind variables)
		// You won't be able to tell if the variables have been bound to _numeric_ values until you actually evaluate the
		// expression, though.
         */
		Set<String> bodyVariables = new HashSet<String>();
		for(Expr clause : getBody()) {
			if (clause.isBuiltIn()) { //built in
				if (clause.getTerms().size() != 2) //must have two operands
					throw new DatalogException("Operator " + clause.getPredicate() + " must have only two operands");
				String a = clause.getTerms().get(0);
				String b = clause.getTerms().get(1);
				if (clause.getPredicate().equals("=")) {
					if (utils.isVariable(a) && utils.isVariable(b) && !bodyVariables.contains(a) && !bodyVariables.contains(b)) {
						throw new DatalogException("Both variables of '=' are unbound in clause " + a + " = " + b);
					}
				} else {
					if (utils.isVariable(a) && !bodyVariables.contains(a)) {
						throw new DatalogException("Unbound variable " + a + " in " + clause);
					}
					if (utils.isVariable(b) && !bodyVariables.contains(b)) {
						throw new DatalogException("Unbound variable " + b + " in " + clause);
					}
				}
			} 
			if(clause.isNegated()) {
				for (String term : clause.getTerms()) {
					if (utils.isVariable(term) && !bodyVariables.contains(term)) {
						throw new DatalogException("Variable " + term + " of rule " + toString() + " must appear in at least one positive expression");
					}
				}
			} else {
                
                for (int i=0; i< clause.getTerms().size(); i++) {
                    String term = clause.getTerms().get(i);
                    ArrayList<String> listTerms = new ArrayList(Arrays.asList(term.split(" ")));
                    if (listTerms.size() > 1) {
                        //bodyVariables.addAll(Arrays.asList(term.split(" ")));
                        bodyVariables.addAll(listTerms.subList(1, listTerms.size()));
                    }
                    else if (utils.isVariable(term)) {
                        bodyVariables.add(term);
                    }
                }
                
                /*
				for (String term : clause.getTerms()) {
					if (utils.isVariable(term)) {
						bodyVariables.add(term);
					}
				}
                 */
			}
		}
		
		// Enforce the rule that variables in the head must appear in the body
		for (String term : getHead().getTerms()) {
			if (!utils.isVariable(term)) {
				//throw new DatalogException("Constant " + term + " in head of rule " + toString());
                // TODO: non-variable in the head of a rule allowed for now
			}
			if (!bodyVariables.contains(term)) {
				//throw new DatalogException("Variables " + term + " from the head of rule " + toString() + " must appear in the body");
                // TODO: non-variable in the head (which may not exist in the body) of a rule allowed for now
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getHead());
		sb.append(" :- ");
		for(int i = 0; i < getBody().size(); i++) {
			sb.append(getBody().get(i));
			if(i < getBody().size() - 1)
				sb.append(", ");
		}
        return sb.toString();
	}

	/**
	 * Creates a new Rule with all variables from bindings substituted.
	 * eg. a Rule {@code p(X,Y) :- q(X),q(Y),r(X,Y)} with bindings {X:aa}
	 * will result in a new Rule {@code p(aa,Y) :- q(aa),q(Y),r(aa,Y)}
	 * @param bindings The bindings to substitute.
	 * @return the Rule with the substituted bindings. 
	 */
	public Rule substitute(Map<String, String> bindings) {
		List<Expr> subsBody = new ArrayList<>();
		for(Expr e : getBody()) {
			subsBody.add(e.substitute(bindings));
		}
		return new Rule(this.getHead().substitute(bindings), subsBody);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Rule)) {
			return false;
		}
		Rule that = ((Rule) other);
		if (!this.getHead().equals(that.getHead())) {
			return false;
		}
		if (this.getBody().size() != that.getBody().size()) {
			return false;
		}
		for (Expr e : this.getBody()) {
			if (!that.getBody().contains(e)) {
				return false;
			}
		}
		return true;
	}

	public Expr getHead() {
		return head;
	}

	public void setHead(Expr head) {
		this.head = head;
	}

	public List<Expr> getBody() {
		return body;
	}

	public void setBody(List<Expr> body) {
		this.body = body;
	}
    
    public boolean recursive() {//TB's original
        for (Expr e : this.getBody()) {
            if (e.getPredicate().equals(this.getHead().getPredicate())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean inBody(String pred) {
        return (Arrays.asList(getBody().stream().map(bodyExpr -> bodyExpr.getPredicate())
                      .filter(bodyPred -> bodyPred.equals(pred))
                      .toArray(String[]::new)).size() != 0);
    }

    public int indexOf(String pred) {
        Expr bodyExpr = Arrays.asList(getBody().stream()
                              .filter(bodyExpr0 -> bodyExpr0.getPredicate().equals(pred))
                              .toArray(Expr[]::new)).get(0);
        return body.indexOf(bodyExpr);
    }

    public ArrayList<String> filterArgs(String mode, List<String> args) {
        ArrayList<String> pair = new ArrayList();
        List<String> ins = new ArrayList();
        List<String> outs = new ArrayList();
        for (int i=0; i < mode.length(); i++) {
            if (mode.charAt(i) == '+')
                ins.add(args.get(i));
            else
                outs.add(args.get(i));
        }
        pair.add(String.join(" ", ins));
        pair.add(String.join(" ", outs));
        return pair;
    }
    
    public List<String> getDependents(List<Expr> facts) {
        ArrayList<String> allPairs = new ArrayList();
        for(Expr e : getBody()) {
            //grabbing corresponding facts for a body pred
            List<Expr> filtFacts = Arrays.asList(facts.stream()
                        .filter(factExpr -> factExpr.getPredicate().equals(e.getPredicate()))
                        .toArray(Expr[]::new));
            //if at least one fact
            if (filtFacts.size() > 0) {
                e.setMode(filtFacts.get(0).getMode());
                ArrayList<String> pair = filterArgs(e.getMode(), e.getTerms());
                allPairs.addAll(pair);
            }
            else {
                System.out.println("==>\u001B[31m[warning]\u001B[30m could not find fact: " + e.toString());
            }
        }

        List<String> dependents = new ArrayList();
        int bodySize = getBody().size();
        for(int i = 0; i < bodySize; i++) {
            List<String> subDependents  = new ArrayList();
            for(int j = i+1; j < bodySize; j++) {
                List<String> subIns = Arrays.asList(allPairs.get(j*2).split(" "));
                if (Arrays.asList(((Arrays.asList(allPairs.get(i*2+1).split(" "))).stream()
                               .filter(outVar -> subIns.contains(outVar))
                               .toArray(String[]::new))).size() != 0)
                    subDependents.add(j+"");
            }
            if (subDependents.size() == 0) {
                subDependents.add("-1");
            }
            dependents.add(String.join(" ", subDependents));
        }
        //System.out.println("=> dependents: " + dependents.toString());
        return dependents;
    }
    
    public void printModes() {
        System.out.println("=> head.getMode() : " + head.getMode());
        for(int i = 0; i < getBody().size(); i++) {
            System.out.println("=> getMode() for body expr " + i + " : " + head.getMode());
        }
    }
    
    public JSONObject toJSONObject() {
        JSONObject NewObj = new JSONObject();
        NewObj.put("head", head.toJSONObject());
        
        JSONArray bodyJSON = new JSONArray();
        for(Expr bodyPred : body) {
            bodyJSON.add(bodyPred.toJSONObject());
        }
        NewObj.put("body", bodyJSON);
        /*
        JSONArray dependentsJSON = new JSONArray();
        for(String dependent : dependents) {
            dependentsJSON.add(dependent);
        }
        NewObj.put("dependents", dependentsJSON);
        */
        
        return NewObj;
        
    }

}

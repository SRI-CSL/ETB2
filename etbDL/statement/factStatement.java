package etb.etbDL.statements;

import java.util.Collection;
import java.util.Map;

import etb.etbDL.etbDatalog;
import etb.etbDL.utils.DatalogException;
import etb.etbDL.utils.Expr;

class factStatement implements etbDLStatement {

	private final Expr fact;
	
	public factStatement(Expr fact) {
		this.fact = fact;
	}

    @Override
    public void addTo(etbDatalog datalog) throws DatalogException {
        datalog.addFact(fact);
    }
}

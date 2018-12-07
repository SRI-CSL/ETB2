package etb.etbDL.statements;

import java.util.ArrayList;

import etb.etbDL.etbDatalog;
import etb.etbDL.utils.DatalogException;
import etb.etbDL.utils.Expr;

public class queryStatement implements etbDLStatement {
    
    private final Expr goal;
    
    public queryStatement(Expr goal) {
        this.goal = goal;
    }
    
    @Override
    public void addTo(etbDatalog datalog) throws DatalogException {
        datalog.setGoal(goal);
    }
}

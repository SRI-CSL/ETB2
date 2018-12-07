/*
 an auto-generated wrapper template for external tool invocations (in this case for the tool gccCompileLinux).
 variables to use - inParams (of length 3) and mode (the invocation mode)
 variables to edit - outParams (of length 3) and evidence
*/

package etb.wrappers;

import java.util.ArrayList;
import etb.etbDL.services.genericWRP;

public abstract class gccCompileLinuxETBWRP extends genericWRP {
	
    protected String in1;
	protected ArrayList<String> in2;
	protected String in3;
    
    protected String out1;
	protected ArrayList<String> out2;
	protected String out3;
    
    @Override
    public void initialise(){
        serviceName = "gccCompileLinux";
        signatureStr = "file file_list file";
        modesStr = "++- +++";
        
        in1 = datalog2JavaStrConst(mode, argList, 1);
		in2 = datalog2JavaListConst(mode, argList, 2);
		in3 = datalog2JavaStrConst(mode, argList, 3);
        
        out1 = in1;
        out2 = in2;
        out3 = in3;
	}

	@Override
	public ArrayList<String> getListOutput(int pos) {
		if (pos == 2) {
			return this.out2;
		}
		return null;
	}

	@Override
	public String getStrOutput(int pos) {
		if (pos == 1) {
			return this.out1;
		}
		if (pos == 3) {
			return this.out3;
		}
		return null;
	}
}

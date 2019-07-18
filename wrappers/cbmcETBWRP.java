/*
 an auto-generated ETB wrapper template for the service 'cbmc'
*/

package etb.wrappers;

import java.util.ArrayList;
import etb.etbDL.services.genericWRP;

public abstract class cbmcETBWRP extends genericWRP {
	//input variables declaration
	protected String in1;
	protected String in2;
	protected String in3;
	//output variables declaration
	protected String out1;
	protected String out2;
	protected String out3;

	@Override
	public void initialise(){
		serviceName = "cbmc";
		signatureStr = "file file file";
		modesStr = "++-";
		//input variables instantiation
		in1 = datalog2JavaStrConst(mode, argList, 1);
		in2 = datalog2JavaStrConst(mode, argList, 2);
		in3 = datalog2JavaStrConst(mode, argList, 3);
		//output variables default instantiation
		out1 = in1;
		out2 = in2;
		out3 = in3;
	}

	@Override
	public ArrayList<String> getListOutput(int pos) {
		return null;
	}

	@Override
	public String getStrOutput(int pos) {
		if (pos == 1) {
			return this.out1;
		}
		if (pos == 2) {
			return this.out2;
		}
		if (pos == 3) {
			return this.out3;
		}
		return null;
	}
}
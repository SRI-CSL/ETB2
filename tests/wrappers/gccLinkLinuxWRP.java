/*
 an auto-generated wrapper template for external tool invocations (in this case for the tool gccLinkLinux).
 variables to use - inParams (of length 3) and mode (the invocation mode)
 variables to edit - outParams (of length 3) and evidence
*/

package etb.wrappers;

import java.util.ArrayList;
import etb.etbDL.services.genericWRP;
import etb.etbDL.services.glueCodeAutoGen;

public class gccLinkLinuxWRP extends genericWRP { 
	ArrayList<String> in1;
	String in2;
	String in3;
	ArrayList<String> out1 = null;
	String out2 = null;
	String out3 = null;

	public gccLinkLinuxWRP(String mode, ArrayList<String> argList){
		in1 = glueCodeAutoGen.datalogList2JavaConst(mode, argList, 1);
		in2 = glueCodeAutoGen.datalogString2JavaConst(mode, argList, 2);
		in3 = glueCodeAutoGen.datalogString2JavaConst(mode, argList, 3);
		this.mode = mode;
		this.argList = argList;
		this.serviceName = "gccLinkLinux";
		this.signatureStr = "file_list string file";
        this.modeStr = "++- +++";
	}

	@Override
	public ArrayList<String> getListOutput(int pos) {
		if (pos == 1) {
			return this.out1;
		}
		return null;
	}

	@Override
	public String getStrOutput(int pos) {
		if (pos == 2) {
			return this.out2;
		}
		if (pos == 3) {
			return this.out3;
		}
		return null;
	}

	@Override
	public void run(){
		if (mode.equals("++-")) {
            //System.out.println("mode: ++-");
            out3 = "repo/src/" + in2 + ".txt";
            evidence = "{file: " + in1 + ", headers: " + in2 + ", os: linux}";
		}
		else {
			System.out.println("unrecognized mode for gccLinkLinux");
		}
	}
}

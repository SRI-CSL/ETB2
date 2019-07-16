/*
 implements a mechanism for translating external predicates to corresponding tool invocation (auto-generated code)
*/

package etb.etbDL.services;
import java.util.ArrayList;
import etb.wrappers.*;

public class externPred2ServiceInstance extends externPred2Service {
	@Override
	public genericWRP getGroundParams(String toolName, ArrayList<String> args, String mode) {
		genericWRP genWRP = null;
		if(toolName.equals("infer")){
			genWRP = new inferWRP();
		}
		else if(toolName.equals("merge")){
			genWRP = new mergeWRP();
		}
		else if(toolName.equals("genPDF")){
			genWRP = new genPDFWRP();
		}
		else if(toolName.equals("cppCheck")){
			genWRP = new cppCheckWRP();
		}
		else if(toolName.equals("cbmc")){
			genWRP = new cbmcWRP();
		}
		else{
			System.out.println("no external service found with name: " + toolName);
		}
		return genWRP;
	}
}
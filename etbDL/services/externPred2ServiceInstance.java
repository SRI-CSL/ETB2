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
		if(toolName.equals("test2")){
			genWRP = new test2WRP();
		}
		else if(toolName.equals("gccCompileLinux")){
			genWRP = new gccCompileLinuxWRP();
		}
		else if(toolName.equals("testCC")){
			genWRP = new testCCWRP();
		}
		else{
			System.out.println("no external service found with name: " + toolName);
		}
		return genWRP;
	}
}
/*
 an auto-generated wrapper template for external tool invocations (in this case for the tool gccCompileLinux).
 variables to use - inParams (of length 3) and mode (the invocation mode)
 variables to edit - outParams (of length 3) and evidence
*/

package etb.wrappers;

public class gccCompileLinuxWRP extends gccCompileLinuxETBWRP {
	
	@Override
	public void run(){
		if (mode.equals("++-")) {
            if (in1.equals("repo/src/s1.txt"))
                out3 = "repo/src/compS1X.txt";
            else if (in1.equals("repo/src/s2.txt"))
                out3 = "repo/src/compS2X.txt";
            else
                out3 = "repo/src/compX.txt";
            //evidence = "GEN_EVID";
		}
		else if (mode.equals("+++")) {
			//do something
		}
		else {
			System.out.println("unrecognized mode for gccCompileLinux");
		}
	}
}

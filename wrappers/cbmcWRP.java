/*
 an auto-generated user wrapper template for the service 'cbmc'
*/

package etb.wrappers;

import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import org.apache.commons.io.FileUtils;
import java.util.Scanner;


public class cbmcWRP extends cbmcETBWRP {

	@Override
	public void run(){
		if (mode.equals("++-")) {
            File SourceFile = new File(in1);
            String FILEDIR = SourceFile.getParent();
            
            String TEMP = FILEDIR + "/CBMC-TEMP";
            out3 = FILEDIR + "/cbmcRes.json";
            try {
                
                JSONParser parser = new JSONParser();
                Object JsonObj = parser.parse(new FileReader(in2));
                JSONArray Errors = (JSONArray) JsonObj;
                
                JSONArray RefErrors = apply(Errors, in2, TEMP);
                
                
                FileWriter fw = new FileWriter(out3);
                fw.write(RefErrors.toJSONString());
                fw.flush();
                fw.close();
                
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            

		}
		else {
			System.out.println("unrecognized mode for cbmc");
		}
	}
    
    
    public static String[] getProcedures(String File, String TEMPDIR){
        
        File SourceFileObj = new File(File);
        String SourceFileBase = SourceFileObj.getName();
        String SourceDirPath = SourceFileObj.getParent();
        
        String ProcList0 = runCMD1("echo $(cd " + SourceDirPath + " && gcc -c " + SourceFileBase + " -o temp.o && nm temp.o | awk '$2 == \"T\" {print $3}')");
        
        String[] ProcList = (ProcList0.replace("\n","")).split(" ");
        for (int i=0; i<ProcList.length; i++){
            ProcList[i] = ProcList[i].substring(1);
        }
        return ProcList;
    }
    
    public static boolean cbmcCheck(String SourceFile, String TEMPDIR, String assertion, Long line, String annotID, String procedure){
        File SourceFileObj = new File(SourceFile);
        String SourceFileBase = SourceFileObj.getName();
        String SourceDirPath = SourceFileObj.getParent();
        File SourceDir = new File(SourceDirPath);
        
        String AnnotDirPath = TEMPDIR + "/AnnotDir" + annotID;
        File AnnotDir = new File(AnnotDirPath);
        
        try{
            FileUtils.copyDirectory(SourceDir, AnnotDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        runCMD0("cd " + AnnotDirPath + " && make clean && rm Makefile "+ SourceFileBase);
        
        String annotProgPath = AnnotDirPath + "/" + SourceFileBase;
        annotateFile(SourceFile, assertion, line, annotProgPath);
        String cbmcOUT = runCMD1("echo $(timeout 1 cbmc --function " + procedure + " " + annotProgPath + ") | grep -q 'VERIFICATION FAILED' && echo $?");
        //System.out.println("cbmcOUT 0: " + cbmcOUT);
        
        cbmcOUT = (cbmcOUT.replace("\n","")).replace(" ","");
        
        //System.out.println("cbmcOUT: " + cbmcOUT);
        //System.out.println("echo $(timeout 1 cbmc --function " + procedure + " " + annotProgPath + ") | grep -q 'VERIFICATION FAILED' && echo $?");
        /*if (cbmcOUT.equals("0")) {
         System.out.println("\u001B[31m ***** ZERO [false alarm]\u001B[0m");
         }
         else {
         System.out.println("\u001B[31m ***** NON ZERO [false alarm]\u001B[0m");
         }
         */
        return cbmcOUT.equals("0");
    }
    
    public static boolean cbmcRefineBB(String SourceFile, String TEMPDIR, String assertion, Long line, int i, JSONObject Error){
        String[] ProcList = getProcedures(SourceFile, TEMPDIR);
        for (int j = 0; j < ProcList.length; j++){
            String annotID = "" + (i+1) + (j+1);
            if (cbmcCheck(SourceFile, TEMPDIR, assertion, line, annotID, ProcList[j])){
                //System.out.println("\u001B[31m ***** multi-proc [real error]\u001B[0m");
                Error.put("severity", "Verified as real error [multi-proc]");
                Error.put("status", "0");
                return true; //TODO: More precise analysis
            }
        }
        Error.put("severity", "Verified as false alarm [multi-proc]");
        Error.put("status", "1");
        return false;
    }
    
    public static boolean cbmcRefineBB(String SourceFile, String TEMPDIR, String assertion, Long line, int i, String procedure, JSONObject Error){
        String annotID = "" + (i+1);
        if (cbmcCheck(SourceFile, TEMPDIR, assertion, line, annotID, procedure)){
            //System.out.println("\u001B[31m ***** def-proc [real error]\u001B[0m");
            Error.put("severity", "Verified as real error [def-proc]");
            Error.put("status", "0");
            return true;
        }
        else {
            Error.put("severity", "Verified as false alarm");
            Error.put("status", "1");
            return false;
        }
    }

    public static JSONArray apply(JSONArray Errors, String SourceFile, String TEMPDIR){
        JSONArray RefErrors = new JSONArray();
        System.out.println("");
        Iterator<JSONObject> iterator = Errors.iterator();
        int refinedCount = 0, falsePositiveCount = 0;
        int errorsSize = Errors.size();
        for (int i = 0 ; iterator.hasNext() ; ++i ){
            
            System.out.print("\tRefining error " + (i+1) + " of " + errorsSize);
            
            JSONObject Error = iterator.next();
            JSONObject RefError = new JSONObject();
            
            String errorFile = (String) Error.get("file");
            Long errorLine = (Long) Error.get("line");
            String errorQual = (String) Error.get("qualifier");
            String errorVar = (String) Error.get("variable");
            String errorType = (String) Error.get("type");
            String errorProc = (String) Error.get("procedure");
            String errorTools = (String) Error.get("tools");
            
            RefError.put("file", errorFile);
            RefError.put("line", errorLine);
            RefError.put("qualifier", errorQual);
            RefError.put("variable", errorVar);
            RefError.put("type", errorType);
            RefError.put("procedure", errorProc);
            RefError.put("tools", errorTools);
            
            if (errorType.equals("NULL DEREFERENCE")){
                refinedCount++;
                String assertion = "assert(" + errorVar + "!= NULL);";
                if (errorProc.equals(" ")) {
                    //cbmcRefine(SourceFile, TEMPDIR, assertion, errorLine, i, RefError);
                    if (cbmcRefineBB(SourceFile, TEMPDIR, assertion, errorLine, i, RefError) == true){
                        System.out.println("\u001B[32m [real error]\u001B[0m");
                    }
                    else {
                        System.out.println("\u001B[31m [false alarm]\u001B[0m");
                        falsePositiveCount++;
                    }
                }
                else {
                    //cbmcRefine(SourceFile, TEMPDIR, assertion, errorLine, i, errorProc, RefError);
                    if (cbmcRefineBB(SourceFile, TEMPDIR, assertion, errorLine, i, errorProc, RefError) == true){
                        System.out.println("\u001B[32m [real error]\u001B[0m");
                    }
                    else {
                        System.out.println("\u001B[31m [false alarm]\u001B[0m");
                        falsePositiveCount++;
                    }
                    
                }
            }
            else {
                RefError.put("severity", "[TODO] for " + errorType);
                RefError.put("status", "2");
                System.out.println("\u001B[32m [real error (not refined)]\u001B[0m");
            }
            RefErrors.add(RefError);
        }
        
        //System.out.println("\tNumber of refined errors : " + refinedCount);
        System.out.println("\tNumber of false positives : " + falsePositiveCount + " out of " + refinedCount + " refined errors.");
        if (refinedCount != 0) {
            System.out.println("\tPercentage of false positives: " + falsePositiveCount*100/refinedCount + "% of refined errors, " + falsePositiveCount*100/errorsSize + "% of total errors");
        }
        
        return RefErrors;
    }
    
    public static void runCMD0(String cmd0){
        Runtime run = Runtime.getRuntime();
        try {
            String[] cmd = { "/bin/sh", "-c", cmd0 };
            Process pr = run.exec(cmd);
            pr.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String runCMD1(String cmd0){
        Runtime run = Runtime.getRuntime();
        String Out = "";
        try {
            String[] cmd = { "/bin/sh", "-c", cmd0 };
            Process pr = run.exec(cmd);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while ((line=buf.readLine())!=null) {
                Out = Out + "\n" + line;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Out;
    }
    
    public static void annotateFile(String InFile, String assertion, Long line, String OutFile) {
        try {
            Scanner scan = new Scanner(new File(InFile));
            FileWriter fw = new FileWriter(OutFile);
            fw.write("#include <assert.h>\n");
            int lcount = 1;
            while(scan.hasNextLine()){
                String lineStr = scan.nextLine();
                if (lcount == line)
                    fw.write(assertion + "\n" + lineStr + "\n");
                else
                    fw.write(lineStr + "\n");
                lcount++;
            }
            fw.flush();
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

/*
 an auto-generated user wrapper template for the service 'infer'
*/

package etb.wrappers;

import java.io.IOException;
import java.lang.InterruptedException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class inferWRP extends inferETBWRP {
    
    @Override
	public void run(){
        if (mode.equals("+-")) {
            out2 = workSpaceDirPath + "/inferRes.json";
            try {
                FileUtils.copyDirectory(new File(in1).getAbsoluteFile().getParentFile(), new File(workSpaceDirPath));
                FileWriter makeFileWriter = new FileWriter(workSpaceDirPath + "/Makefile");
                makeFileWriter.write("\n\nSOURCES = $(shell ls *.c)\nOBJECTS = $(SOURCES:.c=.o)\n\nall: $(OBJECTS)\n\n.c.o:\n\t${CC} -c $<\n\nclean:\n\trm -rf $(OBJECTS) *.xml *.o infer-out \n");
                makeFileWriter.flush();
                makeFileWriter.close();

                FileWriter fw = new FileWriter(out2);
                fw.write(getErrors().toJSONString());
                fw.flush();
                fw.close();

                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
		}
		else {
			System.out.println("unrecognized mode for infer");
		}
	}
    
    private String filterErrorType(String Type) {
        switch (Type) {
            case "NULL_DEREFERENCE":
                return "NULL DEREFERENCE";
            case "RESOURCE_LEAK":
                return "RESOURCE LEAK";
            case "MEMORY_LEAK":
                return "MEMORY LEAK";
            default:
                System.out.println("ERROR: Unknown case for Infer wrapper!");
                System.exit(0);
        }
        return "";
    }
    
    private JSONArray getErrors(){
        
        String quickFixOSX = "ls /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk && export SDKROOT=/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk";
        runCMD0("cd " + workSpaceDirPath + " && make clean && " + quickFixOSX + " && infer run -- gcc -c " + new File(in1).getName());
        JSONParser parser = new JSONParser();
        JSONArray PartErrors = new JSONArray();
        try {
            Object JsonObj = parser.parse(new FileReader(workSpaceDirPath + "/infer-out/report.json"));
            JSONArray Errors = (JSONArray) JsonObj;
            Iterator<JSONObject> iterator = Errors.iterator();
            for (int i = 0 ; iterator.hasNext() ; ++i ){
                JSONObject Error = iterator.next();
                String type0 = (String) Error.get("bug_type");
                String type = filterErrorType(type0);
                String file = (String) Error.get("file");
                Long line = (Long) Error.get("line");
                String procedure = (String) Error.get("procedure");
                String qualifier = (String) Error.get("qualifier");
                JSONObject PartError = new JSONObject();
                
                PartError.put("type", type);
                PartError.put("file", file);
                PartError.put("line", line);
                PartError.put("procedure", procedure);
                PartError.put("qualifier", qualifier);
                PartError.put("class", 1);
                
                if (type.equals("NULL DEREFERENCE")){
                    
                    String qualifierPat = "pointer `(\\w+)` last assigned.*";
                    Pattern p1 = Pattern.compile(qualifierPat);
                    Matcher m1 = p1.matcher(qualifier);
                    String nullVar = "1";
                    if (m1.find()) {
                        for( int j=1; j <= m1.groupCount(); j++) {
                            nullVar = m1.group(j);
                        }
                    }
                    else {
                        System.out.println("Error1: NOT a valid syntax.");
                    }
                    
                    PartError.put("variable", nullVar);
                }
                else{
                    PartError.put("variable", "*none");
                }
                PartErrors.add(PartError);
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return PartErrors;
        
    }
    
    private void clean(){
        runCMD0("cd " + workSpaceDirPath + " && make clean && rm Makefile");
    }
    
    private static void runCMD0(String cmd0){
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

}

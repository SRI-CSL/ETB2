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

public class inferWRP extends inferETBWRP {
    String FILEBASE;
    String FILEDIR;
    JSONArray PartErrors;
    
	@Override
	public void run(){
        File SourceFile = new File(in1);
        SourceFile = new File(SourceFile.getAbsolutePath());
        FILEBASE = SourceFile.getName();
        FILEDIR = SourceFile.getParent();
        PartErrors = new JSONArray();
        
		if (mode.equals("+-")) {

            write_Makefile();
            getErrors();
            clean();
            
            out2 = FILEDIR + "/inferRes.json";
            
            try {
                FileWriter fw = new FileWriter(out2);
                fw.write(PartErrors.toJSONString());
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
    
    private void write_Makefile(){
        try {
            String MAKEFILE_Path = FILEDIR + "/Makefile";
            FileWriter MAKEFILE_FW = new FileWriter(MAKEFILE_Path);
            MAKEFILE_FW.write("\n\nSOURCES = $(shell ls *.c)\nOBJECTS = $(SOURCES:.c=.o)\n\nall: $(OBJECTS)\n\n.c.o:\n\t${CC} -c $<\n\nclean:\n\trm -rf $(OBJECTS) *.xml *.o infer-out \n");
            MAKEFILE_FW.flush();
            MAKEFILE_FW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
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
    
    private void getErrors(){
        
        String quickFixOSX = "ls /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk && export SDKROOT=/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk";
        runCMD0("cd " + FILEDIR + " && make clean && " + quickFixOSX + " && infer run -- gcc -c " + FILEBASE);
        JSONParser parser = new JSONParser();
        try {
            Object JsonObj = parser.parse(new FileReader(FILEDIR + "/infer-out/report.json"));
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
        
    }
    
    private void clean(){
        runCMD0("cd " + FILEDIR + " && make clean && rm Makefile");
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

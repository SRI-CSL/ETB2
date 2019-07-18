/*
 an auto-generated user wrapper template for the service 'cppCheck'
*/

package etb.wrappers;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

import org.json.XML;
import java.io.*;

public class cppCheckWRP extends cppCheckETBWRP {

	@Override
	public void run(){
		if (mode.equals("+-")) {
			//do something
            apply();
            
		}
		else {
			System.out.println("unrecognized mode for cppCheck");
		}
	}
    
    private String filterErrorType(String Type) throws IOException {
        switch (Type) {
            case "nullPointer":
                return "NULL DEREFERENCE";
            case "RESOURCE_LEAK":
                return "RESOURCE LEAK";
            case "memleak":
                return "MEMORY LEAK";
            default:
                return "OTHER";
        }
    }
    
    private String xml2json(String xml) {
        try {
            org.json.JSONObject jsonObj = XML.toJSONObject(xml);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            System.out.println(jsonObj);
            System.out.flush();
            System.setOut(old);
            return baos.toString();
            
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    private String getFileContent(String fileName) {
        String s, Out = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while((s = br.readLine()) != null) {
                Out = Out + "\n" + s;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Out;
    }
    
    private void runCMD0(String cmd0){
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
    
    private void apply(){
        
        File SourceFile = new File(in1);
        String FILEBASE = SourceFile.getName();
        String FILEDIR = SourceFile.getParent();
        
        runCMD0("cd " + FILEDIR + " && cppcheck --xml-version=2 --enable=all " + FILEBASE + " 2> cppcheck.xml");
        String xmlStrOUT = getFileContent(FILEDIR + "/cppcheck.xml");
        String jsonStrOUT = xml2json(xmlStrOUT);
        JSONParser parser = new JSONParser();
        JSONArray PartErrors = new JSONArray();
        
        try {
            Object ObjOUT = parser.parse(jsonStrOUT);
            JSONObject jsonObjOUT = (JSONObject) ObjOUT;
            JSONObject JsonOut = (JSONObject) jsonObjOUT.get("results");
            JSONObject JsonOut2 = (JSONObject) JsonOut.get("errors");
            JSONArray Errors = (JSONArray) JsonOut2.get("error");
            Iterator<JSONObject> iterator = (Iterator<JSONObject>) Errors.iterator();
            ArrayList<Integer> errorLines = new ArrayList<>();
            
            for (int i = 0 ; iterator.hasNext() ; ++i ){
                JSONObject Error = iterator.next();
                String type0 = (String) Error.get("id");
                String type = filterErrorType(type0);
                String file = (String) Error.get("file");
                Long line = (Long) Error.get("line");
                String qualifier = (String) Error.get("msg");
                
                
                JSONObject PartError = new JSONObject();
                PartError.put("type", type);
                PartError.put("file", file);
                PartError.put("line", line);
                PartError.put("procedure", " ");
                PartError.put("qualifier", qualifier);
                
                String variable = "NO_VAR";
                int errClass = 5;
                
                boolean strongerEntry = false;
                boolean addErrorEntry = true;
                Integer oldIndex = 0;
                
                if (line == null) {
                    //System.out.println("No line number for the error ***");
                    
                }
                else {
                    Pattern pattern = Pattern.compile("Possible null pointer dereference: (?:(\\w+))");
                    Matcher m = pattern.matcher(qualifier);
                    if (m.matches()) {
                        variable = m.group(1);
                        errClass = 2;
                    }
                    else {
                        errClass = 4;
                    }
                    
                    
                    String strLine =  "" + line;
                    Integer intLine = Integer.parseInt(strLine);
                    
                    if (errorLines.contains(intLine)) {
                        addErrorEntry = false;
                        //System.out.println("Error entry found on the same line");
                        oldIndex = errorLines.indexOf(intLine);
                        //System.out.println("intLine : " + intLine + ", oldIndex : " + oldIndex + ", and size = " + errorLines.size());
                        //System.out.println("Current error object: " + Error.toJSONString());
                        JSONObject ExistingError = (JSONObject) PartErrors.get(oldIndex);
                        //System.out.println("Already existing error object: " + ExistingError.toJSONString());
                        Integer oldErrorClass0 = (Integer) ExistingError.get("class");
                        String oldErrorClass1 = "" + oldErrorClass0;
                        //System.out.println("strOldErrorClass : " + oldErrorClass1);
                        int oldErrorClass = Integer.parseInt(oldErrorClass1);
                        
                        if (errClass < oldErrorClass){
                            strongerEntry = true;
                        }
                        
                    } else {
                        errorLines.add(intLine);
                        //System.out.println("Error entry not found on the same line");
                    }
                    
                }
                
                PartError.put("variable", variable);
                PartError.put("class", errClass);
                
                
                if (addErrorEntry == true){
                    //System.out.println("Addint fresh error object: " + PartError.toJSONString());
                    PartErrors.add(PartError);
                }
                else if (strongerEntry == true) {
                    //System.out.println("Addint error object [replacing old]: " + PartError.toJSONString());
                    PartErrors.add(oldIndex, PartError);
                }
                else {
                    //System.out.println("Not adding error object");
                }
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        //return PartErrors;
        out2 = FILEDIR + "/cppCheckRes.json";
        try {
            FileWriter fw = new FileWriter(out2);
            fw.write(PartErrors.toJSONString());
            fw.flush();
            fw.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}

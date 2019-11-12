package etb.wrappers;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public abstract class genericWRP {
    
    //inputs common for all wrappers
    protected String serviceName;
    protected String signatureStr;
    protected String modesStr;
    
    //run specific inputs
    protected String mode;
    protected List<String> argList;
    protected String workSpaceDirPath;
    
    //outputs common for all wrappers
    protected String evidence;
    protected List<String> outParams = new ArrayList();
    
    public abstract void run();
    public abstract void initialise();
    
    //gets an arg of etbType list (string_list or file_list) at a given position as ArrayList<String>
    public abstract ArrayList<String> getListOutput(int pos);
    //gets an arg of etbType string (string or file) at a given position as String
    public abstract String getStrOutput(int pos);
    
    public void invoke(String mode, List<String> argList, String workSpaceDirPath) {
        this.mode = mode;
        this.argList = argList;
        this.workSpaceDirPath =  workSpaceDirPath;
        
        File workSpaceDir = new File(workSpaceDirPath);
        if (!workSpaceDir.isDirectory()) {
            workSpaceDir.mkdir();
        }
        
        initialise();
        run();
        java2DatalogConst();
    }
    
    protected String datalog2JavaStrConst(String mode, List<String> argList, int pos) {
        if (mode.length() != argList.size() || mode.length() < pos) {
            System.out.println("\u001B[31m[warning]\u001B[30m service mode does not match with arguments for the given tool wrapper");
            return null;
        }
        if (mode.charAt(pos-1) == '+') {
            String arg = argList.get(pos-1);
            if (arg.contains("file("))
                return arg.substring(5, arg.length()-1);
            else
                return arg;
        }
        return null;
    }
    
    protected ArrayList<String> datalog2JavaListConst(String mode, List<String> argList, int pos) {
        if (mode.length() != argList.size() || mode.length() < pos) {
            System.out.println("\u001B[31m[warning]\u001B[30m service mode does not match with arguments for the given tool wrapper");
            return null;
        }
        
        if (mode.charAt(pos-1) == '+') {
            ArrayList<String> listArg = new ArrayList();
            List<String> filesArgLS = Arrays.asList(argList.get(pos-1).split(" "));
            if (argList.get(pos-1).contains("file(")) {//file list
                for (int i=1; i < filesArgLS.size(); i++) {
                    listArg.add(filesArgLS.get(i).substring(5, filesArgLS.get(i).length()-1));
                }
            }
            else {//normal list
                for (int i=1; i < filesArgLS.size(); i++) {
                    listArg.add(filesArgLS.get(i));
                }
            }
            return listArg;
        }
        return null;
    }
    
    protected void java2DatalogConst() {
        //TODO: better way of handling cases when service can not execute -- currently returning the original args -- inefficient!!
        for (int i=0; i < mode.length(); i++) {
            outParams.add(argList.get(i));
        }
        
        ArrayList<String> modes = new ArrayList(Arrays.asList(modesStr.split(" ")));
        ArrayList<String> signature = new ArrayList(Arrays.asList(signatureStr.split(" ")));
        
        if (!modes.contains(mode)) {//TODO: do we need it?
            System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m invocation mode '" + mode + "'is not defined for '" + serviceName + "'");
        }
        else if (signature.size() != mode.length()) {//TODO: do we need it?
            System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m mismatch between service signature and invocation mode for '" + serviceName + "'");
        }
        else {
            for (int i=0; i < mode.length(); i++) {
                if (mode.charAt(i) == '-') {
                    if (signature.get(i).equals("file_list")) {
                        outParams.set(i, list2String(getListOutput(i+1), "file(", ")"));
                    }
                    else if (signature.get(i).equals("string_list")) {
                        outParams.set(i, list2String(getListOutput(i+1), "", ""));
                    }
                    else if (signature.get(i).equals("file")) {
                        outParams.set(i, "file(" + getStrOutput(i+1) + ")");
                    }
                    else if (signature.get(i).equals("string")) {
                        outParams.set(i, getStrOutput(i+1));
                    }
                    else {
                        outParams.set(i, "UNK_SIGN");//TODO: should it continue?
                        System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m unknown signature value '" + signature.get(i) + "'");
                    }
                }
                else if (mode.charAt(i) != '+') {
                    outParams.set(i, "UNK_MODE");//TODO: should it continue?
                    System.out.println("\t\t ->\u001B[31m [warning]\u001B[30m unknown mode value '" + mode.charAt(i) + "'");
                }
            }
        }
    }

    public List<String> getOutParams() {
        return this.outParams;
    }
    
    public String getEvidence() {
        return this.evidence;
    }
    
    //bundles java ArrayList<String> type to ETB's file_list or string_list String
    private String list2String(ArrayList<String> arg, String pre, String post) {
        String listStr = "listIdent";
        for (int i=0; i < arg.size(); i++) {
            listStr += " " + pre + arg.get(i) + post;
        }
        return listStr;
    }

    
}

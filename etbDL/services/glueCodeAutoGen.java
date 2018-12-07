package etb.etbDL.services;

import etb.etbDL.utils.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public class glueCodeAutoGen {
    
    public static void removeToolWrapper(String toolName) {
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/wrappers/ && rm -f " + toolName + "WRP.java " + toolName + "ETBWRP.java");
        utils.runCMD0("cd " + System.getProperty("user.dir") + "/etb/wrappers/ && rm -f " + toolName + "WRP.class " + toolName + "ETBWRP.class");
    }

    private static ArrayList<String> getToolModes(int paramsCount, int modesCount, String toolExecModes) {
        ArrayList<String> modeList = new ArrayList();
        String modePatternUnit = "([+-]{" + paramsCount + "})";
        String modePattern = modePatternUnit;
        for (int i=1; i<modesCount; i++) {
            modePattern += "\\s*[,]{0,1}\\s*" + modePatternUnit;
        }
        Pattern p1 = Pattern.compile(modePattern);
        Matcher m1 = p1.matcher(toolExecModes);
        if (m1.find()) {
            for( int i=1; i <= m1.groupCount(); i++) {
                //System.out.println("m1.group(" + i + ") : " + m1.group(i));
                modeList.add(m1.group(i));
            }
        } else {
            System.out.println("Error: tool modes are not given in a valid syntax.");
        }
        return modeList;
    }
    
    private static Set<String> getToolModes2(int paramsCount, int modesCount, String toolExecModes) {
        Set<String> modeSet = new HashSet();
        String modePatternUnit = "([+-]{" + paramsCount + "})";
        String modePattern = modePatternUnit;
        for (int i=1; i<modesCount; i++) {
            modePattern += "\\s*[,]{0,1}\\s*" + modePatternUnit;
        }
        Pattern p1 = Pattern.compile(modePattern);
        Matcher m1 = p1.matcher(toolExecModes);
        if (m1.find()) {
            for( int i=1; i <= m1.groupCount(); i++) {
                //System.out.println("m1.group(" + i + ") : " + m1.group(i));
                modeSet.add(m1.group(i));
            }
        } else {
            System.out.println("Error: tool modes are not given in a valid syntax.");
        }
        return modeSet;
    }
    
    public static String getMode(List<String> params) {
        String mode = "";
        for (int i = 0; i< params.size(); i++) {
            if (utils.isVariable(params.get(i)))
                mode += "-";
            else
                mode += "+";
        }
        return mode;
    }
    
    public static void updateExternPredBridgeFile(ArrayList<String> etbTools) {
        
        if (etbTools.size() == 0) {
            updateExternPredBridgeFile();
            return;
        }
        
        String filePath = System.getProperty("user.dir") + "/etbDL/services/externPred2ServiceInstance.java";
        try {
            FileWriter NewFileFW = new FileWriter(filePath);
            NewFileFW.write("/*\n implements a mechanism for translating external predicates to corresponding tool invocation (auto-generated code)\n*/");
            NewFileFW.write("\n\npackage etb.etbDL.services;");
            NewFileFW.write("\nimport java.util.ArrayList;");
            NewFileFW.write("\nimport etb.wrappers.*;");
            NewFileFW.write("\n\npublic class externPred2ServiceInstance extends externPred2Service {");
            NewFileFW.write("\n\t@Override");
            NewFileFW.write("\n\tpublic genericWRP getGroundParams(String toolName, ArrayList<String> args, String mode) {");
            NewFileFW.write("\n\t\tgenericWRP genWRP = null;");
            
            //Iterator<String> it = etbTools.iterator();
            //String toolName = it.next();
            NewFileFW.write("\n\t\tif(toolName.equals(\"" + etbTools.get(0) + "\")){");
            //NewFileFW.write("\n\t\t\tgenWRP = new " + etbTools.get(0) + "WRP(mode, args);");
            NewFileFW.write("\n\t\t\tgenWRP = new " + etbTools.get(0) + "WRP();");
            NewFileFW.write("\n\t\t}");
            
            for (int i=1; i < etbTools.size(); i++) { //while( it.hasNext()) {
                //toolName = it.next();
                NewFileFW.write("\n\t\telse if(toolName.equals(\"" + etbTools.get(i) + "\")){");
                //NewFileFW.write("\n\t\t\tgenWRP = new " + etbTools.get(i) + "WRP(mode, args);");
                NewFileFW.write("\n\t\t\tgenWRP = new " + etbTools.get(i) + "WRP();");
                NewFileFW.write("\n\t\t}");
            }
            
            NewFileFW.write("\n\t\telse{");
            NewFileFW.write("\n\t\t\tSystem.out.println(\"no external service found with name: \" + toolName);");
            //NewFileFW.write("\n\t\t\treturn new ArrayList();");
            NewFileFW.write("\n\t\t}");
            NewFileFW.write("\n\t\treturn genWRP;");
            NewFileFW.write("\n\t}");
            NewFileFW.write("\n}");
            NewFileFW.flush();
            NewFileFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  etbDL/services/externPred2ServiceInstance.java");
    }

    public static void updateExternPredBridgeFile() {
        String filePath = System.getProperty("user.dir") + "/etbDL/services/externPred2ServiceInstance.java";
        try {
            FileWriter NewFileFW = new FileWriter(filePath);
            NewFileFW.write("/*\n implements a mechanism for translating external predicates to corresponding tool invocation (auto-generated code)\n*/");
            NewFileFW.write("\n\npackage etb.etbDL.services;");
            NewFileFW.write("\nimport java.util.ArrayList;");
            NewFileFW.write("\n\npublic class externPred2ServiceInstance extends externPred2Service {");
            NewFileFW.write("\n\t@Override");
            NewFileFW.write("\n\tpublic genericWRP getGroundParams(String toolName, ArrayList<String> args, String mode) {");
            //NewFileFW.write("\n\n\t\treturn new ArrayList();");
            NewFileFW.write("\n\n\t\treturn null;");
            NewFileFW.write("\n\t}");
            NewFileFW.write("\n}");
            NewFileFW.flush();
            NewFileFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  etbDL/services/externPred2ServiceInstance.java");
    }

    public static ArrayList<String> addToolWrapper(String toolName, ArrayList<String> signature, int modesCount, String toolExecModes) {
        int paramsCount = signature.size();
        ArrayList<String> modeSet = getToolModes(paramsCount, modesCount, toolExecModes);
        String wrapperFilePath = System.getProperty("user.dir") + "/wrappers/" + toolName + "WRP.java";
        //genToolWrapper(wrapperFilePath, toolName, signature, modeSet);
        genToolWrapperNEW(toolName, signature, modeSet);
        //utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  wrappers/" + toolName + "WRP.java");
        return modeSet;
    }
    
    public static void genToolWrapperNEW(String toolName, ArrayList<String> signature, ArrayList<String> modeSet) {
        //preparing content of ETBWRP file
        String initBody = "\n\t\tserviceName = \"" + toolName + "\";";
        //preparing runBody for user WRP file
        String modesStr = modeSet.get(0), signatureStr = signature.get(0);
        String runBody = "\n\t\tif (mode.equals(\"" + modeSet.get(0)  + "\")) {\n\t\t\t//do something\n\t\t}";
        for (int i=1; i < modeSet.size(); i++) {
            modesStr += " " + modeSet.get(i);
            //signatureStr += " " + signature.get(i);
            runBody += "\n\t\telse if (mode.equals(\"" + modeSet.get(i)  + "\")) {\n\t\t\t//do something\n\t\t}";
        }

        for (int i=1; i < signature.size(); i++) {
            signatureStr += " " + signature.get(i);
        }

        runBody += "\n\t\telse {\n\t\t\tSystem.out.println(\"unrecognized mode for " + toolName + "\");\n\t\t}";
        runBody = "\n\n\t@Override\n\tpublic void run(){" + runBody + "\n\t}";
        
        initBody += "\n\t\tsignatureStr = \"" + signatureStr + "\";" + "\n\t\tmodesStr = \"" + modesStr + "\";";
        
        String inVarsDecl = "\n\t//input variables declaration", outVarsDecl = "\n\t//output variables declaration", retMethods = "";
        String listRetMethods = "\n\n\t@Override\n\tpublic ArrayList<String> getListOutput(int pos) {";
        String strRetMethods = "\n\n\t@Override\n\tpublic String getStrOutput(int pos) {";
        String inVarsInst = "\n\t\t//input variables instantiation";
        String outVarsInst = "\n\t\t//output variables default instantiation";

        for (int i=1; i <= signature.size(); i++) {
            if (signature.get(i-1).contains("list")) {
                inVarsDecl += "\n\tprotected ArrayList<String> in" + i + ";";
                outVarsDecl += "\n\tprotected ArrayList<String> out" + i + ";";
                inVarsInst += "\n\t\tin" + i + " = datalog2JavaListConst(mode, argList, " + i + ");";
                listRetMethods += "\n\t\tif (pos == " + i + ") {\n\t\t\treturn this.out" + i + ";\n\t\t}";
            }
            else {
                inVarsDecl += "\n\tprotected String in" + i + ";";
                outVarsDecl += "\n\tprotected String out" + i + ";";
                inVarsInst += "\n\t\tin" + i + " = datalog2JavaStrConst(mode, argList, " + i + ");";
                strRetMethods += "\n\t\tif (pos == " + i + ") {\n\t\t\treturn this.out" + i + ";\n\t\t}";
            }
            outVarsInst += "\n\t\tout" + i + " = in" + i + ";";
        }
        
        initBody = "\n\n\t@Override\n\tpublic void initialise(){" + initBody + inVarsInst + outVarsInst + "\n\t}";
        listRetMethods += "\n\t\treturn null;\n\t}";
        strRetMethods += "\n\t\treturn null;\n\t}";
        
        String etbWrapperFilePath = System.getProperty("user.dir") + "/wrappers/" + toolName + "ETBWRP.java";
        String etbWrapperClass = "/*\n an auto-generated ETB wrapper template for the service '" + toolName + "'\n*/";
        etbWrapperClass += "\n\npackage etb.wrappers;\n\nimport java.util.ArrayList;\nimport etb.etbDL.services.genericWRP;";
        etbWrapperClass += "\n\npublic abstract class " + toolName + "ETBWRP extends genericWRP {";
        etbWrapperClass += inVarsDecl + outVarsDecl + initBody + listRetMethods + strRetMethods + "\n}";
        
        String userWrapperFilePath = System.getProperty("user.dir") + "/wrappers/" + toolName + "WRP.java";
        String userWrapperClass = "/*\n an auto-generated user wrapper template for the service '" + toolName + "'\n*/";
        userWrapperClass += "\n\npackage etb.wrappers;";
        userWrapperClass += "\n\npublic class " + toolName + "WRP extends " + toolName + "ETBWRP {";
        userWrapperClass += runBody + "\n}";
        
        try {
            FileWriter etbWrapperFW = new FileWriter(etbWrapperFilePath);
            etbWrapperFW.write(etbWrapperClass);
            etbWrapperFW.flush();
            etbWrapperFW.close();

            FileWriter userWrapperFW = new FileWriter(userWrapperFilePath);
            userWrapperFW.write(userWrapperClass);
            userWrapperFW.flush();
            userWrapperFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        utils.runCMD0("cd " + System.getProperty("user.dir") + " && javac -d .  wrappers/" + toolName + "WRP.java wrappers/" + toolName + "ETBWRP.java");
    }

    private static void getClassVarsDecl(ArrayList<String> signature, String toolName) {
        String inVarsDecl = "", outVarsDecl = "", signatureStr = "", retMethods = "";
        String constBody = "\n\tpublic " + toolName + "WRP(String mode, ArrayList<String> argList){";
        
        for (int i=1; i <= signature.size(); i++) {
            if (signature.get(i).contains("list")) {
                inVarsDecl += "\n\tArrayList<String> in" + i + ";";
                outVarsDecl += "\n\tArrayList<String> out" + i + ";";
                retMethods += "\n\n\tpublic ArrayList<String> getVar" + i + "() {\n\t\treturn this.out" + "i" + ";\n\t}";
            }
            else {
                inVarsDecl += "\n\tString in" + i + ";";
                outVarsDecl += "\n\tString out" + i + ";";
                retMethods += "\n\n\tpublic String getVar" + i + "() {\n\t\treturn this.out" + "i" + ";\n\t}";
            }
            
            if (signatureStr.equals(""))
                signatureStr = signature.get(i);
            else
                signatureStr += " " + signature.get(i);
            
            constBody += "\n\t\tin" + i + " = getConstArg(mode, argList, " + i + ");";
        }
        
        constBody += "\n\t}";
        
    }
    /*
    public static ArrayList<String> datalogList2JavaConst(String mode, ArrayList<String> argList, int pos) {
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

    public static String datalogString2JavaConst(String mode, ArrayList<String> argList, int pos) {
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
     */
}

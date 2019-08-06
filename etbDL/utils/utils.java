package etb.etbDL.utils;

import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.XML;
import java.util.Iterator;
import java.util.Scanner;
import java.lang.InterruptedException;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class utils {
    
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
    
    public static boolean isVariable(String term) {
        return Character.isUpperCase(term.charAt(0));
    }
    
    public static String getSHA1(String file) {
        StringBuffer sb = new StringBuffer();
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[1024];
            int read = 0;
            while ((read = fis.read(data)) != -1) {
                sha1.update(data, 0, read);
            };
            byte[] hashBytes = sha1.digest();
            
            for (int i = 0; i < hashBytes.length; i++) {
                sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sb.toString();
    }
    
    public static boolean existsInRepo(String maybeChildPath, String repoDirPath){
        File maybeChild = new File(maybeChildPath);
        File possibleParent = new File(repoDirPath);
        try {
            
            if (!maybeChild.exists()) {
                return false;
            }
            
            final File parent = possibleParent.getCanonicalFile();
            File child = maybeChild.getCanonicalFile();
            while (child != null) {
                if (child.equals(parent)) {
                    return true;
                }
                child = child.getParentFile();
            }
        } catch (IOException e) {
            System.out.println("could not find file '" + maybeChildPath + "' in the git repo");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static String getFilePathInDirectory(String filePath, String repoDirPath) {
        
        Path p = Paths.get(filePath);
        File file = new File(filePath);
        if (p.isAbsolute() && file.exists()) {
            return filePath;
        }
        else {
            filePath = repoDirPath + "/"+ filePath;
            file = new File(filePath);
            if (file.exists()) {
                return file.getAbsolutePath();
                //return filePath;
            }
            else
                return null;
        }
    }

    public static String getMode(List<String> params) {
        
        return String.join("", Arrays.asList(params.stream().map(param -> getMode(param)).toArray(String[]::new)));
    }
    
    public static String getMode(String param) {
        if (utils.isVariable(param))
            return "-";
        else
            return "+";
    }
    
    public static String fromETBfile(String etbFilePath) {
        return etbFilePath.substring(5, etbFilePath.length()-1);
    }
    
}

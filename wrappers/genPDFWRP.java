/*
 an auto-generated user wrapper template for the service 'genPDF'
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

public class genPDFWRP extends genPDFETBWRP {

	@Override
	public void run(){
		if (mode.equals("++-")) {
            File SourceFile = new File(in1);
            String FILEDIR = SourceFile.getParent();
            out3 = FILEDIR + "/" + in2 + "PDFreport.pdf";
            try {
                
                JSONParser parser = new JSONParser();
                Object JsonObj = parser.parse(new FileReader(in1));
                JSONArray Errors = (JSONArray) JsonObj;
                getReport(Errors, FILEDIR);
                
                
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
		}
		else {
			System.out.println("unrecognized mode for genPDF");
		}
	}
    
    private void getReport(JSONArray Errors, String FILEDIR) {
        String genPDFTEMP = FILEDIR + "/genPDFTEMP";
        
        File genPDFTEMPDir = new File(genPDFTEMP);
        if(!genPDFTEMPDir.exists()){
            genPDFTEMPDir.mkdir();
        }
        
        //File genPDFTEMPDir = new File(genPDFTEMP);
        String texFilePath = genPDFTEMP + "/main.tex";
        try {
            
            FileWriter texFW = new FileWriter(texFilePath);

            texFW.write("\n\n\\documentclass{article} \n\\usepackage{tabularx} \n\\usepackage{multirow} \n\\usepackage[colorlinks]{hyperref} \n\\usepackage{longtable} \n\\usepackage{fancyhdr} \n\\usepackage[usenames, dvipsnames]{color} \n\\pagestyle{fancy} \n\\renewcommand{\\headrulewidth}{0pt} \n\\fancyfoot[L]{Copyright \\copyright 2019 fortiss. All rights reserved.}");
            texFW.write("\n\\fancyfoot[C]{} \n\\fancyfoot[R]{Page \\thepage} \n\\renewcommand{\\footrulewidth}{0pt} \n\\newcommand{\\mcNN}[1]{\\multicolumn{3}{m{13cm}|}{#1}}");
            texFW.write("\n\\newcommand{\\mcRR}[1]{\\multicolumn{3}{m{13cm}|}{\\textcolor{red}{\\textbf{#1}}}} \n\\newcommand{\\mcBB}[1]{\\multicolumn{3}{m{13cm}|}{\\textcolor{blue}{\\textbf{#1}}}} \n\\newcommand{\\mcTODO}[1]{\\multicolumn{3}{m{13cm}|}{\\textcolor{BurntOrange}{\\textbf{#1}}}} \n\\begin{document} \n\\title{Review report} \n\\maketitle \n");
            
            texFW.write(genReportBody(Errors) + "\n");
            texFW.write("\\end{document}\n");
            
            texFW.flush();
            texFW.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.print("Generating PDF reports...");
        runCMD0("cd " + genPDFTEMP + " && pdflatex main.tex && cp main.pdf " + FILEDIR + "/" + in2 + "PDFreport.pdf");
        System.out.println("done");
        System.out.println("\u001B[1m\u001B[34m==> review written to " + FILEDIR + "/" + in2 + "PDFreport.pdf \u001B[0m");
    }
    
    private String genReportBody(JSONArray Errors) {
        
        if (Errors.toJSONString().equals("[]"))
            return "";
        
        
        String RepTop, RepBottom;
        RepTop = "\\section{" + in2 + " report} \n % \n";
        RepBottom = "\\caption{Refined report } \n \\end{longtable} \n \\newpage \n";
        
        String TableTop = "\\begin{longtable}{|p{2cm}|p{5cm}|p{5cm}|p{2cm}|} \n \\hline \n Error No. & Error Type & File & Line \\\\ \n ";
        
        String Report = RepTop + TableTop;
        
        Iterator<JSONObject> iterator = Errors.iterator();
        for (int i = 0 ; iterator.hasNext() ; ++i ){
            JSONObject Error = iterator.next();
            String type = (String) Error.get("type");
            String file = (String) Error.get("file");
            if (file != null){
                file =  file.replace("_", "\\_");
            }
            Long line = (Long) Error.get("line");
            
            String procedure = (String) Error.get("procedure");
            if (procedure != null) {
                procedure =  procedure.replace("_", "\\_");
            }
            String qualifier0 = (String) Error.get("qualifier");
            String  qualifier1 =  qualifier0.replace("_", "\\_");
            String tools = (String) Error.get("tools");
            String  qualifier =  qualifier1 + " [{\\bf Analysis tools: " + tools + "}]";
            String ErrorEntry1 = "\\hline \n \\multirow{2}{*}{E"+ (i+1) + "} & " + type + " & $" + file + "$ & " + line  + " \\\\ \n";
            String ErrorEntry2 = "\\cline{2-4} \n & \\mcNN{" + qualifier + " ( Procedure: $" + procedure + "$)} \\\\ ";
            
            String severity = (String) Error.get("severity");
            
            if (severity != null) {
                //String severity = (String) Error.get("severity");
                String status = (String) Error.get("status");
                if (status.equals("0"))
                    ErrorEntry2 = ErrorEntry2 + "\n \\cline{2-4} \n & \\mcBB{ ERROR VERIFIED } \\\\ ";
                else if (status.equals("1"))
                    ErrorEntry2 = ErrorEntry2 + "\n \\cline{2-4} \n & \\mcRR{ FALSE ALARM ***} \\\\ ";
                else
                    ErrorEntry2 = ErrorEntry2 + "\n \\cline{2-4} \n & \\mcBB{ ???ERROR VERIFIED } \\\\ ";
            }
            ErrorEntry2 = ErrorEntry2 + "\\hline \n ";
            Report = Report + ErrorEntry1 + ErrorEntry2;
        }
        Report = Report + RepBottom;
        return Report;
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

    
}

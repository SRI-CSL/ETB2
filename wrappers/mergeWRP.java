/*
 an auto-generated user wrapper template for the service 'merge'
*/

package etb.wrappers;

import java.io.IOException;
//import java.lang.InterruptedException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.File;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

public class mergeWRP extends mergeETBWRP {

	@Override
	public void run(){
		if (mode.equals("++-")) {
            
            JSONParser parser = new JSONParser();
            JSONArray Errors1 = new JSONArray(), Errors2 = new JSONArray();
            try {
                Object JsonObj1 = parser.parse(new FileReader(in1));
                Errors1 = (JSONArray) JsonObj1;
                
                parser = new JSONParser();
                Object JsonObj2 = parser.parse(new FileReader(in2));
                Errors2 = (JSONArray) JsonObj2;
                
                Iterator<JSONObject> iterator = Errors2.iterator();
                while (iterator.hasNext()){
                    Errors1.add(iterator.next());
                }
                
                File file1 = new File(in1);
                String FILEDIR = file1.getParent();
                
                out3 = FILEDIR + "/mergedRes.json";
                FileWriter fw = new FileWriter(out3);
                fw.write(Errors1.toJSONString());
                fw.flush();
                fw.close();
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
		}
		else {
			System.out.println("unrecognized mode for merge");
		}
	}
}

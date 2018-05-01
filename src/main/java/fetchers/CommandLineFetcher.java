package fetchers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import runner.OutputWriter;

public class CommandLineFetcher {
	
	OutputWriter writer;
	
	public CommandLineFetcher(OutputWriter writer) {
		this.writer = writer;
	}
	
	public String fetch(String command, String outputFile){
		String output = "";
		try {
		    String line;
		    Process p = Runtime.getRuntime().exec(command);
		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) {
		        output += line;
		        output += System.getProperty("line.separator");
		    }
		    input.close();
		    
		    input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    while ((line = input.readLine()) != null) {
		        System.out.println(line);
		    }
		    input.close();
		    
			writer.write(output,outputFile);
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return output;
	}
	
	public void writeError(String command, String errorFile){
		try {
			String output = "";
		    String line;
		    Process p = Runtime.getRuntime().exec(command);

		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    while ((line = input.readLine()) != null) {
		        output += line;
		        output += System.getProperty("line.separator");
		    }
		    input.close();
		    
			writer.write(output,errorFile);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
}

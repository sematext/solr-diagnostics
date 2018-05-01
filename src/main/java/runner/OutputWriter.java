package runner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class OutputWriter {
	
	public String outputDir;
	public String timestamp = String.valueOf(System.currentTimeMillis());
	
	public OutputWriter(String parentDir) throws IOException {
		File outputDirHandle = new File(parentDir + File.separator + "solr-diagnostics_" + timestamp);
		outputDir = outputDirHandle.getAbsolutePath();
		if (!outputDirHandle.mkdir()) {
			throw new IOException("Can't create output dir " + outputDirHandle.getAbsolutePath());
		}
	}

	public void write(String output, String file) {
		try {
			Files.write( Paths.get(outputDir + File.separator + file),
			output.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		} catch (IOException e) {
			System.out.println("Can't write to " + file + ". Ouput: " + output);
			e.printStackTrace();
		}
	}
	
	public String getOutputDir() {
		return outputDir;
	}
}

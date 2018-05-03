package fetchers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import runner.OutputWriter;

public class SolrStuffFetcher {
	OutputWriter writer;
	public String cmdLine;
	
	public SolrStuffFetcher(OutputWriter writer, String cmdLine){
		this.writer = writer;
		this.cmdLine = cmdLine;
	}
	
	public String getParamRegex(String regex) throws Exception {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(cmdLine);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new Exception("Can't find a match for " + regex);
		}
	}
	
	public String getSolrHome() throws Exception {
		return getParamRegex("\\-Dsolr\\.solr\\.home=(.*?) ");
	}
	
	public void fetchSolrXML() {
		String outputDir = writer.getOutputDir();
		File solrHome;
		try {
			solrHome = new File(getSolrHome());
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		try {
			System.out.println("* Copying " + solrHome.getAbsolutePath() + "/solr.xml");
			Files.copy(Paths.get(solrHome.getAbsolutePath() + "/solr.xml"),
					Paths.get(outputDir + "/solr.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fetchGCLogs() {
		String outputDir = writer.getOutputDir();
		String solrLogsOutputDir = outputDir + File.separator + "logs";
		
		Path solrLogsOutputDirHandle = Paths.get(solrLogsOutputDir);
		try {
			Files.createDirectory(solrLogsOutputDirHandle);
		} catch (IOException e1) {
			System.out.println("Can't create the directory" + solrLogsOutputDir);
			return;
		}
		
		File GCLog;
		try {
			GCLog = new File(getParamRegex("\\-Xloggc:(.*?) "));
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		String GCbaseName = GCLog.getName();
		File GCLogDir = GCLog.getParentFile();
		try {
			System.out.println("* Copying logs from " + GCLogDir.getAbsolutePath() + " to " + solrLogsOutputDir);
			for (Path gclog : Files.newDirectoryStream(GCLogDir.toPath(), GCbaseName + "*")) {
				Files.copy(gclog, Paths.get(solrLogsOutputDir, gclog.getFileName().toString()));
			}
		} catch (IOException e) {
			System.out.println("Can't copy GC logs from " + GCLogDir.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	public void fetchLogs() {
		String outputDir = writer.getOutputDir();
		String solrLogsOutputDir = outputDir + File.separator + "logs";
		
		File solrLogsOutputDirHandle = new File(solrLogsOutputDir);
		if (!solrLogsOutputDirHandle.mkdir()) {
			System.out.println("Can't create output dir " + solrLogsOutputDir);
		}
		
		String solrLogsSourceDir;
		try {
			solrLogsSourceDir = getParamRegex("\\-Dsolr\\.log\\.dir=(.*?) ");
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
	    System.out.println("* Copying logs from " + solrLogsSourceDir + " to " + solrLogsOutputDir);
	    try {
			FileUtils.copyDirectory(new File(solrLogsSourceDir), solrLogsOutputDirHandle);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getBinSolr() {
		String binSolr;
		try {
			binSolr = getParamRegex("\\-Dsolr\\.install\\.dir=(.*?) ") + File.separator
					+ "bin" + File.separator + "solr";
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return binSolr;
	}
}

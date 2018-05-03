package runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import addresses.SolrAddress;
import fetchers.CommandLineFetcher;
import fetchers.HTTPFetcher;
import fetchers.SolrStuffFetcher;

public class DiagnosticsRunner {

	static Options options = new Options();
	
	public static void main(String[] args) throws Exception {
		CommandLine cmd = getCommandLine(args);
		
		String outputBaseDir = cmd.getOptionValue("outputdir");
		if (outputBaseDir == null) {
			outputBaseDir = "/tmp";
		}
		
		System.out.println("Gathering info...");
		
		OutputWriter writer = new OutputWriter(outputBaseDir);
		String outputDir = writer.outputDir; // add a timestamped suffix here
		CommandLineFetcher commandLineFetcher = new CommandLineFetcher(writer);
		
		/*
		 * System-related info
		 */
		
		System.out.println("* ps output");
		//saving the ps output because we'll need it later to detect Solr's command line
		String ps = commandLineFetcher.fetch("ps ax", "psax.txt");
		
		System.out.println("* dmesg output");
		commandLineFetcher.fetch("dmesg -Tx", "dmesg.txt");
		
		System.out.println("* netstat output");
		commandLineFetcher.fetch("netstat -pantu", "netstat.txt");
		
		System.out.println("* sysctl output");
		commandLineFetcher.fetch("sysctl -a", "sysctl.txt");
		
		System.out.println("* uname output");
		commandLineFetcher.fetch("uname -a", "uname.txt");
		
		System.out.println("* top output (this should take a bit)");
		commandLineFetcher.fetch("top -H -n3 -b", "top.txt");
		
		if (cmd.hasOption("getVarLog")) {
			String varLogDestination = outputDir + File.separator + "var_log";
			System.out.println("* Copying logs from /var/log to " + varLogDestination);
		    try {
				FileUtils.copyDirectory(new File("/var/log"), new File(varLogDestination));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * Solr-related info. Requires finding the command line of Solr. Exit if this info can't be found
		 */
		
		String solrCmdLine = getSolrCmdLineOrExit(ps);

		SolrStuffFetcher solrFetcher = new SolrStuffFetcher(writer, solrCmdLine);
		
		if (!cmd.hasOption("noSolrLogs")) {
			solrFetcher.fetchLogs();
			solrFetcher.fetchGCLogs();
		}
		
		System.out.println("* /proc/SolrPID/limits output");
		String solrPID = getSolrPID(solrCmdLine);
		commandLineFetcher.fetch("cat /proc/" + solrPID + "/limits", "solr_limits.txt");
		
		System.out.println("* java version (for the Solr process)");
		String javaPath = getJavaPath(solrCmdLine);
		commandLineFetcher.writeError(javaPath + " -version", "java_version.txt");
		
		solrFetcher.fetchSolrXML();
		
		SolrAddress solrAddress = getSolrAddress(solrCmdLine);
		String binSolr = solrFetcher.getBinSolr();

		HTTPFetcher httpFetcher = new HTTPFetcher(solrAddress);
		
		if (solrAddress.isCloud()){
			System.out.println("--> Found zkHost or zkRun in the command line - assuming SolrCloud");
			if (binSolr != null) {
				// create dir
				File configDirHandle = new File(writer.getOutputDir() + File.separator + "configs");
				if (configDirHandle.mkdir()) {
					//run bin/solr zk cp -r zk:/configs THAT DIR THAT WE CREATED -z localhost:9983
					String configLocation = "zk:/configs";
					String configLocalLocation = configDirHandle.getAbsolutePath();
					System.out.println("* Copying configsets from " + configLocation + " to " + configLocalLocation);
					commandLineFetcher.fetch(binSolr
								+ " zk cp -r " + configLocation + " " + configLocalLocation
								+ " -z " + solrAddress.getZkAddress(),
							"zk_cp_outerr.out");
				} else {
					System.out.println("Can't create config output dir " + configDirHandle.getAbsolutePath());
				}
			} else {
				System.out.println("Couldn't find bin/solr. Maybe -Dsolr.install.dir isn't present?");
			}
			
			
			System.out.println("* Getting collections list");
			writer.write(httpFetcher.get("admin/collections?action=LIST"), "collections.json");
			
			System.out.println("* Getting aliases");
			writer.write(httpFetcher.get("admin/collections?action=LISTALIASES"), "aliases.json");

			System.out.println("* Getting cluster status");
			writer.write(httpFetcher.get("admin/collections?action=CLUSTERSTATUS"), "clusterstatus.json");
			
			System.out.println("* Getting overseer status");
			writer.write(httpFetcher.get("admin/collections?action=OVERSEERSTATUS"), "overseerstatus.json");
		} else {
			System.out.println("--> No zkHost or zkRun in the command line - assuming Master/Slave");
			
			String configsSource = solrFetcher.getSolrHome() + File.separator + "configsets";
			String configsDestination = outputDir + File.separator + "configsets";
			System.out.println("* Copying configsets from " + configsSource + " to " + configsDestination);
		    try {
				FileUtils.copyDirectory(new File(configsSource), new File(configsDestination));
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("* Getting cores status");
			writer.write(httpFetcher.get("admin/cores?action=STATUS"), "cores.json");
		}	

		System.out.println("* Getting metrics");
		writer.write(httpFetcher.get("admin/metrics"), "metrics.json");
		
		String zipFile = outputBaseDir + File.separator + writer.timestamp + ".zip";
		System.out.println("* Packing " + outputDir + " to " + zipFile);
		zip(outputDir, zipFile);
	}
	
	public static SolrAddress getSolrAddress(String solrCmdLine) throws Exception {
		SolrAddress solr = new SolrAddress();
		Pattern pattern = Pattern.compile("\\-Djetty\\.port=(.*?) ");
		Matcher matcher = pattern.matcher(solrCmdLine);
		if (matcher.find()) {
			Integer jettyPort = Integer.parseInt(matcher.group(1));
			solr.setJettyPort(jettyPort);

			pattern = Pattern.compile("\\-Dhost=(.*?) ");
			matcher = pattern.matcher(solrCmdLine);
			if (matcher.find()) {
				solr.setHost(matcher.group(1));
			} else {
				solr.setHost("localhost");
			}
			
			if (solrCmdLine.contains("zkRun")){
				// Embedded Zookeeper
				solr.setCloud(true);
				// Zookeeper port is HTTP port + 1000 on the same host
				Integer zkPort = jettyPort + 1000;
				solr.setZkAddress(solr.getHost() + ":" + String.valueOf(zkPort));
			} else if (solrCmdLine.contains("zkHost")) {
				solr.setCloud(true);
				pattern = Pattern.compile("\\-DzkHost=(.*?) ");
				matcher = pattern.matcher(solrCmdLine);
				if (matcher.find()) {
					solr.setZkAddress(matcher.group(1));
				} else {
					throw new Exception("Can't extract zkHost, though the zkHost option seems to be there. Strange.");
				}
			} else {
				solr.setCloud(false);
			}
			return solr;
		} else {
			throw new Exception("Can't find Jetty Port. Bailing out");
		}
	}
	
	public static String getJavaPath(String solrCmdLine) {
		String javaPath = "";
		String[] tmp = solrCmdLine.split("\\s");
		for (String s : tmp) {
			if (s.contains("java")) {
				javaPath = s;
				break;
			}
		}
		return javaPath;
	}
	
	public static String getSolrPID(String solrCmdLine) {
		String solrPID = "";
		String[] tmp = solrCmdLine.split("\\s");
		for (String s : tmp) {
			if (s.length() > 0) {
				solrPID = s;
				break;
			}
		}
		return solrPID;
	}
	
	public static String getSolrCmdLineOrExit(String ps) {
		String solrCmdLine = null;
		Scanner scanner = new Scanner(ps);
		while (scanner.hasNextLine()) {
		  String line = scanner.nextLine();
		  if (line.contains("-jar start.jar") && line.contains("solr")) {
			  solrCmdLine = line;
			  break;
		  }
		}
		scanner.close();
		
		if (solrCmdLine == null) {
			System.out.println("Can't find the Solr command line. Skipping Solr-related info");
			System.exit(2);
		}
		
		return solrCmdLine;
	}
	
	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "solr-diagnostics", options );
	}
	
	private static CommandLine getCommandLine(String[] args) {
		options.addOption("help", "Print this message");
		options.addOption("noSolrLogs", "Skip fetching Solr logs");
		options.addOption("getVarLog", "Fetch all logs from /var/log (there might be a lot of data)");
		options.addOption(Option.builder("outputdir")
                .desc( "Where to write the diagnostics files. A timestamp-based directory will be created there. Defaults to /tmp" )
                .hasArg(true)
                .build());
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			printHelp(options);
			System.exit(1);
		}
		
		if (cmd.hasOption("help")) {
			printHelp(options);
			System.exit(0);
		}
		
		return cmd;
	}
	
	public static void zip(final String folderName, String zipFileName) throws IOException {
		final Path folder = Paths.get(folderName);
		Path zipFilePath = Paths.get(zipFileName);
        try (
            FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
            ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
	        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	                zos.putNextEntry(new ZipEntry(folder.relativize(file).toString()));
	                Files.copy(file, zos);
	                zos.closeEntry();
	                return FileVisitResult.CONTINUE;
	            }
	
	            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	                zos.putNextEntry(new ZipEntry(folder.relativize(dir).toString() + File.separator));
	                zos.closeEntry();
	                return FileVisitResult.CONTINUE;
	            }
	        });
        }
	}
	

}

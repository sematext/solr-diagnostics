package fetchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import addresses.SolrAddress;

public class HTTPFetcher {
	
	String baseURL;
	
	public HTTPFetcher(SolrAddress solrAddress) throws MalformedURLException {
		baseURL = "http://" + solrAddress.getHost() + ":" + solrAddress.getJettyPort() + "/solr/";
	}
	
	public String get(String endpoint) throws IOException {
		URL url = new URL(baseURL + endpoint);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(
		  new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
		    content.append(inputLine);
			content.append(System.lineSeparator());
		}
		in.close();
		con.disconnect();
		return content.toString();
	}
}

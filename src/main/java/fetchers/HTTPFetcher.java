package fetchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import addresses.SolrAddress;

public class HTTPFetcher {

	String baseURL;
	Boolean ssl = false;

	public HTTPFetcher(SolrAddress solrAddress, Boolean ssl,
			final String user, final String pass) throws MalformedURLException {
		this.ssl = ssl;
		String proto = "http";
		if (ssl) {
			proto = "https";
		}
		baseURL = proto + "://" + solrAddress.getHost() + ":" + solrAddress.getJettyPort() + "/solr/";
		
		if (user != null) {
			Authenticator.setDefault (new Authenticator() {
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication (user, pass.toCharArray());
			    }
			});
		}
	}

	public String get(String endpoint) throws IOException {
		
		URL url = new URL(baseURL + endpoint);
		try {
			HttpURLConnection con;
			if (ssl) {
				con = (HttpsURLConnection) url.openConnection();
			} else {
				con = (HttpURLConnection) url.openConnection();
			}
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
		} catch (IOException e) {
			return e.getMessage();
		}
	}
}

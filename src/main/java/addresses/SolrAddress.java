package addresses;

public class SolrAddress {
	String host;
	int jettyPort;
	String zkAddress;
	boolean isCloud;
	
	public boolean isCloud() {
		return isCloud;
	}
	public void setCloud(boolean isCloud) {
		this.isCloud = isCloud;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getJettyPort() {
		return jettyPort;
	}
	public void setJettyPort(int jettyPort) {
		this.jettyPort = jettyPort;
	}
	public String getZkAddress() {
		return zkAddress;
	}
	public void setZkAddress(String zkHost) {
		this.zkAddress = zkHost;
	}
}

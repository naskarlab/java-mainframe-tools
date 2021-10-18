package com.naskar.jmft.cics;

/**
 *   Ex.:
 * 		new Configuration("tcp://host", 35500, "CICS1", "CICS1", "CICS1")
 *   refs.:
 *   	https://www.ibm.com/support/knowledgecenter/en/SSGMCP_5.3.0/com.ibm.cics.ts.installation.doc/topics/dfha1lf.html
 */
public class Configuration {

	private String url;
	private int port;
	private String region;
	private String user;
	private String pw;

	public Configuration(String url, int port, String region, String user, String pw) {
		this.url = url;
		this.port = port;
		this.region = region;
		this.user = user;
		this.pw = pw;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

}

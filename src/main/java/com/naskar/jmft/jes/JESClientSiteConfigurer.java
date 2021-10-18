package com.naskar.jmft.jes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JESClientSiteConfigurer {
	
	public static final String DEFAULT_PARAMETERS = "/ftp/quote_site.txt";
	
	public void configureFromFile(JESClient client, String file) {
		String lastLine = null;
		try {
			List<String> lines =  getText(file);
			for(String line : lines) {
				if(line != null) {
					
					line = line.trim();
					if(!line.isEmpty()) {
						lastLine = line;
						client.siteWithCheck(lastLine);
					}
					
				}
			}
		} catch(Exception e) {
			throw new RuntimeException("Error on SITE command: " + lastLine, e);
		}
		
	}
	
	public List<String> getText(String filename) throws IOException {
		List<String> lines = new ArrayList<String>();
		InputStream in = this.getClass().getResourceAsStream(filename);
		if(in == null) {
			in = new FileInputStream(filename); 
		}
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
			    lines.add(line);
			}
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch(Exception e) {
					// @ignore
				}
			}
		}
		
		return lines;
	}

}

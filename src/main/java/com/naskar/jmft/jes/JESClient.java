package com.naskar.jmft.jes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class JESClient extends FTPClient {
	
	private static final Logger logger = Logger.getLogger(JESClient.class.getName());
	
	private String username = null;
	private String nameFilter = "*";
	private String ownerFilter = "*";

	public JESClient() {
		FTPClientConfig config = new FTPClientConfig();
		config.setUnparseableEntries(true);
		configure(config);
	}
	
	public void configureLogger() {
		addProtocolCommandListener(new ProtocolCommandListener() {
			
			@Override
			public void protocolCommandSent(ProtocolCommandEvent event) {
				log(event, ">> ");
			}
			
			@Override
			public void protocolReplyReceived(ProtocolCommandEvent event) {
				log(event, "<< ");
			}
			
			private void log(ProtocolCommandEvent event, String prefix) {
				String msg = prefix + " ";
				if(!"PASS".equals(event.getCommand())) {
					msg = prefix + " " + event.getMessage();
				}
				msg = msg.replace("\n", " ").replace("\r", " ");
				logger.info(msg);
			}
		});
	}

	@Override
	public boolean login(String username, String password) throws IOException {
		boolean result = super.login(username, password);
		if (result) {
			this.username = username;
			this.ownerFilter = username;
			this.nameFilter = "*";
			defineFilter();
		} else {
			checkAndThrowError();
		}
		return result;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void resetFilter() throws IOException {
		this.ownerFilter = username;
		this.nameFilter = "*";
		defineFilter();
	}
	
	private void defineFilter() throws IOException {
		// quote site FILETYPE=JES JESJOBNAME=* JESOWNER=*
		site(String.format("FILETYPE=JES JESJOBNAME=%s JESOWNER=%s", nameFilter, ownerFilter));
		checkAndThrowError();
		String reply = getReplyString();
		if(reply.contains("is not authorized")) {
			throw new RuntimeException(reply);
		}
	}

	public void setOwnerFilter(String owner) throws IOException {
		this.ownerFilter = owner;
		defineFilter();
	}

	public void setNameFilter(String name) throws IOException {
		this.nameFilter = name;
		defineFilter();
	}

	public List<JESJob> listJobs() throws IOException {
		return listJobs(false);
	}

	public List<JESJob> listJobs(boolean detailed) throws IOException {
		if (detailed) {
			return listJobsDetailed();
		} else {
			return listJobsSummary();
		}
	}

	public List<JESJob> listJobsSummary() throws IOException {
		List<JESJob> jobs = new ArrayList<JESJob>();
		String[] names = listNames();
		if (names == null) {
			return jobs;
		}
		for (String name : names) {
			JESJob job = new JESJob(this, name);
			jobs.add(job);
		}
		return jobs;
	}

	public List<JESJob> listJobsDetailed() throws IOException {
		List<JESJob> jobs = new ArrayList<JESJob>();
		for (FTPFile file : listFiles()) {
			JESJob job = new JESJob(this);
			if (job.parseDetails(file.getRawListing())) {
				jobs.add(job);
			}
		}
		return jobs;
	}

	public String readFile(String handle) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		retrieveFile(handle, outputStream);
		return outputStream.toString();
	}
	
	public JESJob submit(InputStream jclFile) throws IOException {
		OutputStream out = storeFileStream("job");
		copy(jclFile, out);
		jclFile.close();
		out.close();
		completePendingCommand();
		
		return send();
	}
	
	public JESJob submit(String sourceJCL) throws IOException {
		OutputStream out = storeFileStream("job");
		out.write(sourceJCL.getBytes());
		out.close();
		completePendingCommand();

		JESJob job = send();
		job.setJcl(sourceJCL);
		
		return job;
	}

	private JESJob send() {
		String reply = getReplyString();
		Pattern pattern = Pattern.compile("It is known to JES as (?<handle>.+)");
		Matcher matcher = pattern.matcher(reply);
		JESJob job = null;
		if (matcher.find()) {
			String handle = matcher.group(1);
			if(!handle.startsWith("JOB")) {
				throw new RuntimeException("Error on submmit job: " + reply);
			}
			job = new JESJob(this, handle);
		}
		return job;
	}

	public JESJob execute(String datasetName) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		retrieveFile(String.format("'%s'", datasetName), outputStream);
		JESJob job = new JESJob(this);
		job.setSpool(outputStream.toString());
		return job;
	}

	public String retrieveFile(String fileName) throws IOException {
		site("FILETYPE=SEQ");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		retrieveFile(fileName, outputStream);
		defineFilter();
		return outputStream.toString("ASCII");
	}
	
	@Override
	public boolean storeFile(String remote, InputStream local) throws IOException {
		site("FILETYPE=SEQ");
		boolean result = false;
		
		try {
			result = super.storeFile(remote, local);
		} catch(Exception e) {
			StringBuilder sb = new StringBuilder(getReplyString());
			getReply();
			sb.append(getReplyString());
			throw new RuntimeException(sb.toString(), e);
		}
		
		if(result) {
			defineFilter();	
		} else {
			checkAndThrowError();			
		}
		return result;
	}
	
	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int bytesRead = in.read(buffer);
			if (bytesRead == -1) {
				break;
			}
			out.write(buffer, 0, bytesRead);
		}
	}

	@Override
	public String toString() {
		return "JESClient [_hostname_=" + _hostname_ + "]";
	}
	
	public void siteWithCheck(String command) throws IOException {
		site(command);
		checkAndThrowError();
	}
	
	private void checkAndThrowError() {
		if(!FTPReply.isPositiveCompletion(getReplyCode())) {
			throw new RuntimeException(getReplyString());
		}
	}
	
}

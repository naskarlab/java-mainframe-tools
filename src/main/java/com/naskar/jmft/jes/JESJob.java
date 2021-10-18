package com.naskar.jmft.jes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPFile;

// https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.3.0/com.ibm.zos.v2r3.halu001/dispstatusdirlevel2.htm
public class JESJob {
	
	public static final Integer CC_SUCCESS = 0;
	public static final Integer CC_WARN = 4;
	
	private JESClient jesClient;
	
	private String name;
	private String handle;
	private String owner;
	private String status;
	private String clazz;
	private Integer returnCode;
	private Integer abendCode;
	private Integer spoolFileCount;
	private List<JESSpoolFile> spoolFiles;
	private String jcl;
	private Boolean jclError;
	private String rcResult;
	private Boolean rcError;
	private String spool;
	private String result;
	
	public JESJob(JESClient jesClient) {
		this.jesClient = jesClient;
	}

	public JESJob(JESClient jesClient, String handle) {
		this(jesClient);
		this.handle = handle;
	}

	boolean parseDetails(String raw) {
		
		String details = raw;
		
		Pattern commonPattern = Pattern.compile("(?<common>(?<name>[^ ]+) +(?<handle>[^ ]+) +(?<owner>[^ ]+) +(?<status>[^ ]+) +(?<clazz>[^ ]+)).*");
		Matcher commonMatcher = commonPattern.matcher(details);
		if (!commonMatcher.matches()) {
			return false;
		}
		
		String jobId = commonMatcher.group(3);
		if(jobId == null || jobId.trim().isEmpty() || 
				jobId.equals("JOBID") || !jobId.startsWith("JOB") ) {
			return false;
		}
		
		details = details.substring(commonMatcher.group(1).length());

		name = commonMatcher.group(2); // "name"
		handle = jobId; // "handle"
		owner = commonMatcher.group(4); // "owner"
		status = commonMatcher.group(5); // "status"
		clazz = commonMatcher.group(6); // "clazz"
		
		
		Pattern resultCodePattern = Pattern.compile(" *(?<return> +(?<result>[^=]+)=(?<code>\\d+)).*");
		Matcher resultCodeMatcher = resultCodePattern.matcher(details);
		if (resultCodeMatcher.matches()) {
			details = details.substring(resultCodeMatcher.group(1).length());
			
			String resultReturn = resultCodeMatcher.group(2); // "result"
			if (resultReturn.equals("RC")) {
				returnCode = Integer.parseInt(resultCodeMatcher.group(3)); // "code"
				
			} else if (resultReturn.equals("ABEND")) {
				abendCode = Integer.parseInt(resultCodeMatcher.group(3)); // "code"
				
			}
		} else {
			Pattern jclErrorPattern = Pattern.compile("(?<error> +\\(JCL error\\) +).*");
			Matcher jclErrorMatcher = jclErrorPattern.matcher(details);
			
			if (jclErrorMatcher.matches()) {
				details = details.substring(jclErrorMatcher.group(1).length());
				jclError = true;
			}
		}
		
		Pattern spoolPattern = Pattern.compile(".*(?<files>\\d+) +spool files +");
		Matcher spoolMatcher = spoolPattern.matcher(details);
		if (spoolMatcher.matches()) { // "spool" 
			spoolFileCount = Integer.parseInt(spoolMatcher.group(1)); // "files"
		}

		/* TODO: analisar qual o tratamento, quando ocorrer.
		if(details.contains("RC unknown")) {
			rcResult = details;
			rcError = true;
		}
		*/
		
		return true;
	}
	
	public boolean refreshDetails() throws IOException {
		boolean readStatus = false;
		FTPFile[] files = jesClient.listFiles(this.handle);
		if(files != null && files.length > 1) {
			String listing = files[1].getRawListing();
			if(files.length > 2) {
				listing += " " + files[files.length-1];
			}
			readStatus = parseDetails(listing);
		}
		return readStatus;
	}

	public List<JESSpoolFile> getSpoolFiles() throws IOException {
		if(this.spoolFiles == null) {
			this.spoolFiles = new ArrayList<JESSpoolFile>();
			
			FTPFile[] files = this.jesClient.listFiles(this.handle);
			for (int i = 1; i < files.length-1; i++) {
				FTPFile file = files[i];
				String rawListing = file.getRawListing();

				Pattern pattern = Pattern.compile("^ +\\d+");
				Matcher matcher = pattern.matcher(rawListing);
				if (matcher.find()) {
					JESSpoolFile spoolFile = new JESSpoolFile(this);
					
					spoolFile.setHandle(Integer.parseInt(rawListing.substring(9, 12).trim()));
					spoolFile.setStep(rawListing.substring(13, 21).trim());
					spoolFile.setProcedure(rawListing.substring(22, 30).trim());
					spoolFile.setType(rawListing.substring(31, 32).trim());
					spoolFile.setNameDD(rawListing.substring(33, 41).trim());
					spoolFile.setByteCount(Integer.parseInt(rawListing.substring(42).trim()));
					
					spoolFiles.add(spoolFile);
				}
				
			}	
		}
		return spoolFiles;
	}
	
	public void waitComplete(int seconds) throws IOException, TimeoutException {
		refreshDetails();
		int time = 1;
		while(getReturnCode() == null && 
				getAbendCode() == null && 
				getJclError() == null && 
				getRcError() == null) {
			
			time++;
			if(time > seconds) {
				throw new TimeoutException();
			}
			
			try { Thread.sleep(1000); } catch(Exception e) {}
			
			refreshDetails();
		}
	}

	void setSpool(String spool) {
		this.spool = spool;
	}
	
	void setJcl(String jcl) {
		this.jcl = jcl;
	}
	
	public String getJcl() {
		return jcl;
	}
	
	public String getRcResult() {
		return rcResult;
	}
	
	public String waitSpool(int seconds) throws IOException, TimeoutException {
		spool = waitFile(seconds, null);
		return spool;
	}
	
	public String waitFile(int seconds, String file) throws IOException, TimeoutException {
		String filter = handle;
		if(file != null) {
			filter += "." + file;
		}
		int time = 1;
		String tmp = null;
		do {
			this.jesClient.setDataTimeout(seconds * 1000);
			tmp = this.jesClient.readFile(filter);
			if(isEmpty(tmp)) {
				time++;
				if(time > seconds) {
					throw new TimeoutException();
				}
				try { Thread.sleep(1000); } catch(Exception e) {}
			} else {
				break;
			}
		} while(isEmpty(tmp));
		return tmp;
	}
	
	private static boolean isEmpty(String tmp) {
		return tmp == null || tmp.trim().isEmpty();
	}
	
	public boolean isError() {
		return (jclError != null && jclError) ||
				(rcError != null && rcError);
	}

	public void purge() throws IOException {
		this.jesClient.deleteFile(this.handle);
	}
	
	public JESClient getClientJES() {
		return jesClient;
	}
	
	public String getName() {
		return name;
	}
	
	public String getHandle() {
		return handle;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getClazz() {
		return clazz;
	}
	
	public Integer getReturnCode() {
		return returnCode;
	}
	
	public Integer getAbendCode() {
		return abendCode;
	}
	
	public Integer getSpoolFileCount() {
		return spoolFileCount;
	}
	
	public String getSpool() {
		return spool;
	}
	
	public String getResult() {
		return result;
	}
	
	public Boolean getJclError() {
		return jclError;
	}
	
	public Boolean getRcError() {
		return rcError;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public void setReturnCode(Integer returnCode) {
		this.returnCode = returnCode;
	}

	public void setAbendCode(Integer abendCode) {
		this.abendCode = abendCode;
	}

	public void setSpoolFileCount(Integer spoolFileCount) {
		this.spoolFileCount = spoolFileCount;
	}

	public void setJclError(Boolean jclError) {
		this.jclError = jclError;
	}

	public void setRcResult(String rcResult) {
		this.rcResult = rcResult;
	}

	public void setRcError(Boolean rcError) {
		this.rcError = rcError;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "JESJob [name=" + name + ", handle=" + handle + ", owner=" + owner
				+ ", status=" + status + ", clazz=" + clazz + ", returnCode=" + returnCode + ", abendCode="
				+ abendCode + ", spoolFileCount=" + spoolFileCount + ", spoolFiles=" + spoolFiles
				+ ", jclError=" + jclError + ", rcResult=" + rcResult + ", rcError=" + rcError
				+ "]";
	}

}

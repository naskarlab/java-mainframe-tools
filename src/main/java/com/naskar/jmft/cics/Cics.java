package com.naskar.jmft.cics;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ctg.client.ECIRequest;
import com.ibm.ctg.client.JavaGateway;

/**
 * Call the CICS transaction using CICS Transaction Gateway (CTG) 
 * using an External Call Interface (ECI) over CICS TCPIPSERVICE Resource.
 * 
 *  refs.:
 * 		https://www.ibm.com/support/knowledgecenter/en/SSZHFX/welcome.html
 *  
 */
public class Cics {
	
	private static final Logger logger = Logger.getLogger(Cics.class.getName());

	private Configuration config;
	private String transaction;
	private String program;

	public Cics(Configuration config, String transaction, String program) {
		this.config = config;
		this.transaction = transaction;
		this.program = program;
	}

	public void runECIRequest(byte[] commarea) {
		JavaGateway jg = null;
		try {
			ECIRequest req = new ECIRequest(ECIRequest.ECI_SYNC, 
					config.getRegion(), 
					config.getUser(), 
					config.getPw(),
					program, 
					transaction, 
					commarea);
			
			jg = new JavaGateway(config.getUrl(), config.getPort());
			int rc = jg.flow(req);
			
			if(rc != 0) {
				
				StringBuilder msg = new StringBuilder("Error: RC [" + rc + "]: [" + req.getRcString() + "] CICS: [" + req.getCicsRcString() + "]");
				
				if (req.getCicsRc() == ECIRequest.ECI_ERR_SECURITY_ERROR 
                        || (req.Abend_Code != null && req.Abend_Code.equalsIgnoreCase("AEY7"))) {
					msg.append("Invalid username or password.");
				}
				
				if (req.getCicsRc() == ECIRequest.ECI_ERR_TRANSACTION_ABEND) { 
					msg.append(" ABEND: [" + req.Abend_Code + "]");
				}
				
				throw new JavaCicsException(msg.toString());
			}
			
		} catch (Exception e) {
			throw new JavaCicsException(e);
			
		} finally {
			if(jg != null && jg.isOpen()) {
				try {
					jg.close();
				} catch(Exception e) {
					logger.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

}

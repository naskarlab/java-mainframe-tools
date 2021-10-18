package com.naskar.jmft.cics.jes;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import com.naskar.jmft.jes.JESClient;
import com.naskar.jmft.jes.JESJob;

public class CompileCobol {
	
	private static final Logger logger = Logger.getLogger(CompileCobol.class.getName());
	
	@Test
	public void testRDZCOMPI() {
		
		try {
			JESClient ftp = new JESClient();
			ftp.connect("192.168.15.101");
			if(!ftp.login("IBMUSER","SYS1")) {
				throw new IllegalArgumentException("Error on connect.");
			}
			
			ftp.storeFile("'NK.SOURCE(PG01)'", this.getClass().getResourceAsStream("/cbl/PG01.cbl"));
			
			JESJob job1 = ftp.submit(this.getClass().getResourceAsStream("/jcl/NKCOMP.jcl"));
			logger.info(job1.toString());
			job1.waitComplete(120);
			
			logger.info(job1.waitSpool(3));
			logger.info(job1.getSpool());
			
			logger.info(job1.toString());
			
			job1.purge();
			ftp.quit();
			
			Assert.assertEquals(new Integer(0), job1.getReturnCode());
			
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
	}

}

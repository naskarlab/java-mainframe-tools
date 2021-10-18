package com.naskar.jmft.jes;

import org.junit.Assert;
import org.junit.Test;

public class JESJobTest {
	
	@Test
	public void testRC0000() {
		// Arrange
		JESJob target = new JESJob(null, null);
		String details = "USER1   JOB00054 USER1 OUTPUT A RC=0000 5 spool files              ";
		
		// Act
		target.parseDetails(details);
		
		// Assert
		Assert.assertEquals("USER1", target.getName());
		Assert.assertEquals("JOB00054", target.getHandle());
		Assert.assertEquals("USER1", target.getOwner());
		Assert.assertEquals("OUTPUT", target.getStatus());
		Assert.assertEquals("A", target.getClazz());
		Assert.assertEquals(new Integer(0), target.getReturnCode());
		Assert.assertEquals(new Integer(5), target.getSpoolFileCount());
	}
	
	@Test
	public void testABEND() {
		// Arrange
		JESJob target = new JESJob(null, null);
		String details = "USER1A  JOB00083 USER1 OUTPUT A ABEND=806 3 spool files            ";
		
		// Act
		target.parseDetails(details);
		
		// Assert
		Assert.assertEquals("USER1A", target.getName());
		Assert.assertEquals("JOB00083", target.getHandle());
		Assert.assertEquals("USER1", target.getOwner());
		Assert.assertEquals("OUTPUT", target.getStatus());
		Assert.assertEquals("A", target.getClazz());
		Assert.assertEquals(new Integer(806), target.getAbendCode());
		Assert.assertEquals(new Integer(3), target.getSpoolFileCount());
	}
	
	@Test
	public void testJCLError() {
		// Arrange
		JESJob target = new JESJob(null, null);
		String details = "USER1J  JOB00082 USER1 OUTPUT A (JCL error) 3 spool files          ";
		
		// Act
		target.parseDetails(details);
		
		// Assert
		Assert.assertEquals("USER1J", target.getName());
		Assert.assertEquals("JOB00082", target.getHandle());
		Assert.assertEquals("USER1", target.getOwner());
		Assert.assertEquals("OUTPUT", target.getStatus());
		Assert.assertEquals("A", target.getClazz());
		Assert.assertEquals(true, target.getJclError());
		Assert.assertEquals(new Integer(3), target.getSpoolFileCount());
	}
	

}

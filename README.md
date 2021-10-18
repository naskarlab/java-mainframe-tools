# JMFT - Java Mainframe Tools


#### CICS (Customer Information Control System)

Create a COMMAREA to call CICS programs using just POJO with PICTURE annotations.

Call the CICS transaction or programs using CICS Transaction Gateway (CTG) using an External Call Interface (ECI) over CICS TCPIPSERVICE Resource.

refs.: https://www.ibm.com/support/knowledgecenter/en/SSZHFX/welcome.html



### JES (Job Entry Subsystem)

Submit jobs and gets status and spool output

refs.: https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.3.0/com.ibm.zos.v2r3.halu001/dispstatusdirlevel2.htm



## Examples


### CICS
  
 ```
 public class Data {
  
 	@PIC9(8)
 	private Long id;
  
 	@PIC9(5)
 	private Integer code1;
  
 	@PIC9(9)
 	private Integer code2;

 	...
}
```

Call:

```
Configuration config = new Configuration("tcp://host", 35500, "CICS1", "CICS1", "CICS1"); // CTG Connection
CommArea comm = new CommArea();

Cics cics = new Cics(config, "PRG1", "PROG1"); // CICS transaction or program name
Data data = new Data(); // your POJO

byte[] commArea = comm.to(data); // transforms POJO to comm area
cics.runECIRequest(commArea);
comm.from(commArea, data); // injects the values from comm area to POJO

```

refs.:
	PICTURE: https://www.ibm.com/support/knowledgecenter/en/SS6SG3_5.2.0/com.ibm.cobol52.ent.doc/PGandLR/ref/rlddepic.html
		


### JES - Submit job

```

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

```

## Usage with Maven

```
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
    <groupId>com.github.naskarlab</groupId>
    <artifactId>java-mainframe-tools</artifactId>
    <version>1.0</version>
</dependency>

```

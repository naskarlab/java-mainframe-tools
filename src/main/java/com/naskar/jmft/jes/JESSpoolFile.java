package com.naskar.jmft.jes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JESSpoolFile {

	private JESJob jobJES;

	private Integer handle;
	private String step;
	private String procedure;
	private String type;
	private String nameDD;
	private Integer byteCount;

	public JESSpoolFile(JESJob jobJES) {
		this.jobJES = jobJES;
	}

	public String read() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		this.jobJES.getClientJES().retrieveFile(String.format("%s.%d", this.jobJES.getHandle(), this.handle),
				outputStream);
		return outputStream.toString();
	}

	void setHandle(Integer handle) {
		this.handle = handle;
	}

	void setStep(String step) {
		this.step = step;
	}

	void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	void setType(String type) {
		this.type = type;
	}

	void setNameDD(String nameDD) {
		this.nameDD = nameDD;
	}

	void setByteCount(Integer byteCount) {
		this.byteCount = byteCount;
	}

	public JESJob getJobJES() {
		return jobJES;
	}

	public Integer getHandle() {
		return handle;
	}

	public String getStep() {
		return step;
	}

	public String getProcedure() {
		return procedure;
	}

	public String getType() {
		return type;
	}

	public String getNameDD() {
		return nameDD;
	}

	public Integer getByteCount() {
		return byteCount;
	}

	@Override
	public String toString() {
		return "JESSpoolFile [handle=" + handle + ", step=" + step + ", procedure=" + procedure + ", type=" + type
				+ ", nameDD=" + nameDD + ", byteCount=" + byteCount + "]";
	}

}

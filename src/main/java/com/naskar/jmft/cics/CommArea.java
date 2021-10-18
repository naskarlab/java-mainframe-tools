package com.naskar.jmft.cics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Create a COMMAREA to call CICS programs using just POJO with PICTURE annotations:
 * 
 * 		Example:
 * 
 *  		public class Data {
 *  
 *  			@PIC9(8)
 *  			private Long id;
 *  
 *  			@PIC9(5)
 *  			private Integer code1;
 *  
 *				@PIC9(9)
 *				private Integer code2;
 *
 *				...
 *			}
 *
 *		Call:
 *
 *			Configuration config = new Configuration("tcp://host", 35500, "CICS1", "CICS1", "CICS1");
 *			CommArea comm = new CommArea();
 *
 *			Cics cics = new Cics(config, "PRG1", "PROG1");
 *			Data data = new Data();
 *
 *			byte[] commArea = comm.to(data); // transforms POJO to comm area
 *			cics.runECIRequest(commArea);
 *			comm.from(commArea, data); // injects the values from comm area to POJO
 *
 *		refs.:
 *			PICTURE
 *
 *			https://www.ibm.com/support/knowledgecenter/en/SS6SG3_5.2.0/com.ibm.cobol52.ent.doc/PGandLR/ref/rlddepic.html
 *		
 */
public class CommArea {
	
	public byte[] to(Object o) {
		try {

			List<Object> sb = new ArrayList<Object>();

			for (Field f : getFields(o)) {

				appendPIC9(o, sb, f);
				appendPIC9Comp3(o, sb, f);
				appendPICX(o, sb, f);
				appendPICS9(o, sb, f);
				appendPICS9Comp3(o, sb, f);
				appendREDEFINES(o, sb, f);

			}

			return convert(sb);

		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}
	
	private <T> List<Field> getFields(T t) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> clazz = t.getClass();
        while (clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
	
	private byte[] convert(List<Object> sb) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		for(Object o : sb) {
			if(o instanceof String) {
				out.write(toBytes((String)o));
			} else if(o instanceof byte[]) {
				out.write((byte[])o);
			} else {
				throw new JavaCicsException("Unsupported type.");				
			}
		}
		
		return out.toByteArray();
	}

	private void appendPIC9(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			PIC9 pic9 = f.getAnnotation(PIC9.class);
			if (pic9 != null && pic9.usage() == Usage.DISPLAY) {
				f.setAccessible(true);
				Object value = f.get(o);
				
				if (value == null) {
					value = "";
				}
				if (value instanceof Double) {
					value = formatDouble((Double) value, pic9.value(), pic9.decimal());
				}
	
				lpad(sb, pic9.value() + pic9.decimal(), String.valueOf(value), "0");
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private void appendPIC9Comp3(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			PIC9 pic9 = f.getAnnotation(PIC9.class);
			if (pic9 != null && pic9.usage() == Usage.COMP_3) {
				
				f.setAccessible(true);
				Object value = f.get(o);
				
				if (value == null) {
					value = "";
				}
				
				String signal = "F";
				
				if (value instanceof Double) {
					
					if(((Double)value) < 0) {
						signal = "D";
					}
					value = formatDouble((Double) value, pic9.value(), pic9.decimal());
					
				} else {
					
					if(value instanceof Integer) {
						if(((Integer)value) < 0) {
							signal = "D";
						}
						
					} else if(value instanceof Long) {
						if(((Long)value) < 0) {
							signal = "D";
						}
					}
					
				}
	
				value = value + signal;
				int count = pic9.value() + pic9.decimal() + 1;
				if(count % 2 != 0) {
					count++;
				}
				StringBuilder valueStr = lpad(count, String.valueOf(value), "0");
				
				hexpad(sb, valueStr.length() / 2, valueStr.toString());
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private void appendPICS9(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			PICS9 pics9 = f.getAnnotation(PICS9.class);
			if (pics9 != null && pics9.usage() == Usage.DISPLAY) {
				f.setAccessible(true);
				Object value = f.get(o);
				
				if (value == null) {
					value = "";
				}
				if (value instanceof Double) {
					value = formatDouble((Double) value, pics9.value(), pics9.decimal());
					value = unconvertSignal(String.valueOf(value));
				}
	
				lpad(sb, pics9.value() + pics9.decimal(), String.valueOf(value), "0");
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private void appendPICS9Comp3(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			PICS9 pics9 = f.getAnnotation(PICS9.class);
			if (pics9 != null && pics9.usage() == Usage.COMP_3) {
				
				f.setAccessible(true);
				Object value = f.get(o);
				
				if (value == null) {
					value = "";
				}
				
				String signal = "F";
				
				if (value instanceof Double) {
					
					if(((Double)value) < 0) {
						signal = "D";
					} else {
						signal = "C";
					}
					value = formatDouble((Double) value, pics9.value(), pics9.decimal());
					
				} else {
					
					if(value instanceof Integer) {
						if(((Integer)value) < 0) {
							signal = "D";
						} else {
							signal = "C";
						}
						
					} else if(value instanceof Long) {
						if(((Long)value) < 0) {
							signal = "D";
						} else {
							signal = "C";
						}
					}
					
				}
	
				value = value + signal;
				int count = pics9.value() + pics9.decimal() + 1;
				if(count % 2 != 0) {
					count++;
				}
				StringBuilder valueStr = lpad(count, String.valueOf(value), "0");
				
				hexpad(sb, valueStr.length() / 2, valueStr.toString());
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}

	private void appendPICX(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			PICX picx = f.getAnnotation(PICX.class);
			if (picx != null) {
				f.setAccessible(true);
				Object value = f.get(o);
				if (value == null) {
					value = "";
				}
	
				if(picx.encoding() == Encoding.BASE64) {
					b64pad(sb, picx.value(), (String)value);
					
				} else if(picx.encoding() == Encoding.HEX) {
					hexpad(sb, picx.value(), (String)value);
					
				} else {
					rpad(sb, picx.value(), String.valueOf(value), " ");
					
				}
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private void b64pad(List<Object> sb, int size, String value) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			int s = 0;
			if(value != null && value.length() > 0) {
				byte[] b = Base64.getDecoder().decode(value);
				s = b.length;
				out.write(b);
			}
			
			byte[] b = new byte[size-s];
			Arrays.fill(b, (byte)0);
			out.write(b);
			
			sb.add(out.toByteArray());
		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}
	
	private void hexpad(List<Object> sb, int size, String value) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			int s = 0;
			if(value != null && value.length() > 0) {
				for(int i = 0; i < value.length(); i += 2) {
					String v = value.substring(i, i + 1) + value.substring(i + 1, i + 2);
					out.write(new Integer(Integer.parseInt(v, 16)).byteValue());
				}
				s = value.length() / 2;
			}
			
			byte[] b = new byte[size-s];
			Arrays.fill(b, (byte)0);
			out.write(b);
			
			sb.add(out.toByteArray());
		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}
	
	private String hex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < data.length; i++) {
			sb.append(String.format("%02X", data[i]));
		}
		return sb.toString();
	}

	private void appendREDEFINES(Object o, List<Object> sb, Field f) throws IllegalAccessException {
		try {
			REDEFINES redefines = f.getAnnotation(REDEFINES.class);
			if (redefines != null) {
				f.setAccessible(true);
				Object value = f.get(o);
				
				if (value == null) {
					throw new JavaCicsException("REDEFEINES required.");
				} 
				
				sb.add(to(value));
			}
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}

	private void lpad(List<Object> sb, int size, String value, String pad) {
		StringBuilder s = lpad(size, value, pad);
		sb.add(s.toString());
		if(s.length() > size) {
			throw new JavaCicsException("Value invalid size: [" + value + "] : [" + size + "]");
		}
	}

	private StringBuilder lpad(int size, String value, String pad) {
		StringBuilder s = new StringBuilder();
		for (int i = value.length(); i < size; i++) {
			s.append(pad);
		}
		s.append(value);
		return s;
	}
	
	private void rpad(List<Object> sb, int size, String value, String pad) {
		StringBuilder s = new StringBuilder();
		s.append(value);
		for (int i = value.length(); i < size; i++) {
			s.append(pad);
		}
		sb.add(s.toString());
	}
	
	private String formatDouble(Double value, int integ, int dec) {
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(dec);
		df.setMaximumFractionDigits(dec);
		df.setMinimumIntegerDigits(integ);
		df.setMaximumIntegerDigits(integ);
		df.setDecimalSeparatorAlwaysShown(false);
		df.setGroupingUsed(false);
		return df.format(value).replaceAll(",", "").replaceAll("\\.", "");
	}

	private byte[] toBytes(String input) {
		try {

			byte[] result = new byte[input.length()];
			System.arraycopy(input.getBytes("CP1047"), 0, result, 0, result.length);
			return result;

		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}

	public String fromBytes(byte[] commarea) {
		try {
			return new String(commarea, "CP1047");
		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}

	public int from(byte[] output, Object o) {
		try {

			int pos = 0;
			for (Field f : getFields(o)) {

				pos = configurePIC9(output, pos, o, f);
				pos = configurePIC9Comp3(output, pos, o, f);
				pos = configurePICS9(output, pos, o, f);
				pos = configurePICS9Comp3(output, pos, o, f);
				pos = configurePICX(output, pos, o, f);
				pos = configureREDEFINES(output, pos, o, f);
				
			}
			
			return pos;

		} catch (Exception e) {
			throw new JavaCicsException(e);
		}
	}
	
	private int configurePIC9(byte[] output, int pos, Object o, Field f) {
		try {
			PIC9 pic9 = f.getAnnotation(PIC9.class);
			if (pic9 != null && pic9.usage() == Usage.DISPLAY) {
				
				f.setAccessible(true);
				
				String value = fromBytes(Arrays.copyOfRange(output, pos, pos + pic9.value() + pic9.decimal()));
				value = value.trim();
				if(value.isEmpty()) {
					value = null;
					f.set(o, value);
				} else {
					configureNumeric(o, f, pic9.decimal(), value);
				}
				
				pos += pic9.value()+pic9.decimal();
				
			}

			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private int configurePIC9Comp3(byte[] output, int pos, Object o, Field f) {
		try {
			PIC9 pic9 = f.getAnnotation(PIC9.class);
			if (pic9 != null && pic9.usage() == Usage.COMP_3) {
				
				int count = Math.round((float)(pic9.value() + pic9.decimal() + 1) / 2);
				byte[] buf = Arrays.copyOfRange(output, pos, pos + count);
				
				String value = hex(buf);
				
				f.setAccessible(true);
				
				value = value.trim();
				if(value.isEmpty()) {
					value = null;
					f.set(o, value);
				} else {
					
					String signal = value.substring(value.length()-1, value.length());
					
					value = value.substring(0, value.length()-1);
					if("D".equals(signal)) {
						value = "-" + value;
					}
					
					configureNumeric(o, f, pic9.decimal(), value);
				}
				
				pos += count;
			}
			
			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private int configurePICS9(byte[] output, int pos, Object o, Field f) {
		try {
			PICS9 pics9 = f.getAnnotation(PICS9.class);
			if (pics9 != null && pics9.usage() == Usage.DISPLAY) {
				
				f.setAccessible(true);
				
				String value = fromBytes(Arrays.copyOfRange(output, pos, pos + pics9.value() + pics9.decimal()));
				value = value.trim();
				if(value.isEmpty()) {
					value = null;
					f.set(o, value);
				} else {
					value = convertSignal(value);
					configureNumeric(o, f, pics9.decimal(), value);
				}
				
				pos += pics9.value() + pics9.decimal();
				
			}

			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private int configurePICS9Comp3(byte[] output, int pos, Object o, Field f) {
		try {
			PICS9 pics9 = f.getAnnotation(PICS9.class);
			if (pics9 != null && pics9.usage() == Usage.COMP_3) {
				
				int count = Math.round((float)(pics9.value() + pics9.decimal() + 1) / 2);
				byte[] buf = Arrays.copyOfRange(output, pos, pos + count);
				
				String value = hex(buf);
				
				f.setAccessible(true);
				
				value = value.trim();
				if(value.isEmpty()) {
					value = null;
					f.set(o, value);
				} else {
					
					String signal = value.substring(value.length()-1, value.length());
					
					value = value.substring(0, value.length()-1);
					if("D".equals(signal)) {
						value = "-" + value;
					}
					
					configureNumeric(o, f, pics9.decimal(), value);
				}
				
				pos += count;
			}
			
			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}

	private void configureNumeric(Object o, Field f, int decimal, String value) throws IllegalAccessException {
		if (f.getType().equals(Integer.class)) {
			f.set(o, Integer.parseInt(value));
			
		} else if (f.getType().equals(Double.class)) {
			String v = value.substring(0, value.length()-decimal);
			String d = value.substring(value.length()-decimal, value.length());
			f.set(o, Double.parseDouble(v + "." + d));
			
		} else if (f.getType().equals(Long.class)) {
			f.set(o, Long.parseLong(value));
			
		} else {
			f.set(o, value);
		}
	}
	
	/**
	 * Table converter to signal values: PICS9.
	 */
	private static final String DIGITS    = "0123456789";
	private static final String POSITIVES = "{ABCDEFGHI";
	private static final String NEGATIVES = "}JKLMNOPQR";

	/**
	 * Converts the ASCII value converted from EBCDIC with signal to ASCII value with signal.
	 * 		Ex.: 0001239I -> +00012399 
	 * 			 0001239R -> -00012399
	 * 
	 * ref.: https://www.ibm.com/support/knowledgecenter/pt-br/SSKM8N_8.0.0/com.ibm.etools.mft.doc/ad06900_.htm
	 */
	private String convertSignal(String value) {
		String newValue = value.substring(0, value.length()-1);
		String signal = value.substring(value.length()-1);
		
		boolean positive = true;
		int pos = POSITIVES.indexOf(signal);
		if(pos == -1) {
			pos = NEGATIVES.indexOf(signal);
			positive = false;
		} 
		
		if(pos == -1) {
			pos = 0;
		}
		
		String signedValue = String.valueOf(DIGITS.charAt(pos));
		if(positive) {
			newValue = "+" + newValue + signedValue;
		} else {
			newValue = "-" + newValue + signedValue;
		}
		
		return newValue;
	}
	
	/**
	 * +000123.99 -> 0001239I
	 * -000123.99 -> 0001239R
	 */
	private String unconvertSignal(String value) {
		String newValue = value;
		
		if(value != null && !value.isEmpty()) {
			newValue = value.substring(1, value.length()-1).replace(".", "");
			String last = value.substring(value.length()-2, value.length()-1);
			
			int pos = DIGITS.indexOf(last);
			if(pos == -1) {
				pos = 0;
			}
			
			if(value.startsWith("-")) {
				newValue += NEGATIVES.charAt(pos);
						
			} else {
				newValue += POSITIVES.charAt(pos);
				
			}
		}
		
		return newValue;
	}

	private int configurePICX(byte[] output, int pos, Object o, Field f) {
		try {
			PICX picx = f.getAnnotation(PICX.class);
			if (picx != null) {
				
				byte[] buf = Arrays.copyOfRange(output, pos, pos + picx.value());
				Object value = null;
				
				if(picx.encoding() == Encoding.BASE64) {
					value = Base64.getEncoder().encodeToString(buf);
					
				} else if(picx.encoding() == Encoding.HEX) {
					value = hex(buf);
					
				} else {
					String v = fromBytes(buf);
					if(v != null) {
						value = v.trim();
					}
				}
				
				f.setAccessible(true);
				f.set(o, value);
				
				pos += picx.value();
			}

			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}
	
	private int configureREDEFINES(byte[] output, int pos, Object o, Field f) {
		try {
			REDEFINES redefines = f.getAnnotation(REDEFINES.class);
			if (redefines != null) {
				f.setAccessible(true);
				pos += from(Arrays.copyOfRange(output, pos, output.length), f.get(o));
			}

			return pos;
		} catch (Exception e) {
			throw new JavaCicsException("ERROR on field: " + f.getName(), e);
		}
	}

}

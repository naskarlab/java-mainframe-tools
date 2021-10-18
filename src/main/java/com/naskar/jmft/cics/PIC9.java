package com.naskar.jmft.cics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PIC9 {

	int value();

	int decimal() default 0;
	
	Usage usage() default Usage.DISPLAY;

}

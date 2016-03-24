package com.pau101.paintthis.util.nbtassist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NBTMutatorProperty {
	String name();

	Class<?> type();

	String setter() default "";

	String getter() default "";
}

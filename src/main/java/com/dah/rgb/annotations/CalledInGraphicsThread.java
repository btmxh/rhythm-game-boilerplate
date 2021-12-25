package com.dah.rgb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

// needs to call OpenGL functions
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface CalledInGraphicsThread {
}

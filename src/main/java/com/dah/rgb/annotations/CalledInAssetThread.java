package com.dah.rgb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

// usually involves some I/O operations, which can took a lot of time.
// But can still be called in other threads if needed.
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface CalledInAssetThread {
}

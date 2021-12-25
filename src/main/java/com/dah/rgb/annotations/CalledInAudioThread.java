package com.dah.rgb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

// need to call OpenAL functions.
// But if EXT_thread_local_context is present, all threads have a OpenAL context,
// @CalledInAudioThread functions can be called everywhere
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface CalledInAudioThread {
}

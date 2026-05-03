package de.fau.tf.lgdv.runtime.annotations;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface JSEvent { String value() default ""; String[] params() default {}; }

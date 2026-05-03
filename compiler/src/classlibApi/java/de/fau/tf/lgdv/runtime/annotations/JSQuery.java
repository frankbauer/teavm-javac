package de.fau.tf.lgdv;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSQuery { String value() default ""; String[] params() default {}; }

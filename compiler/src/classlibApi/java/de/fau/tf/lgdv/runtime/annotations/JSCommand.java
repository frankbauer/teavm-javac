package de.fau.tf.lgdv;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSCommand { String value() default ""; String[] params() default {}; }

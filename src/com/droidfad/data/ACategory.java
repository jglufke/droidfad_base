package com.droidfad.data;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ACategory {
	public enum Category {
		global, project, user, model, notPersistent;
	}
	public Category category() default Category.model;
}

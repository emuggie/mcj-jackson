package com.emuggie.mcj.jackson.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonFilter;

import com.emuggie.mcj.jackson.filter.JsonOnlyPropertyFilter;

@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD ,ElementType.TYPE })
@JacksonAnnotationsInside
@JsonFilter(JsonOnlyPropertyFilter.FILTER_ID)
public @interface JsonOnly {
	String[] value();
}

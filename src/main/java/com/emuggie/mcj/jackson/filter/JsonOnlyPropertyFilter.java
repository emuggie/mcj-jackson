package com.emuggie.mcj.jackson.filter;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emuggie.mcj.jackson.annotation.JsonOnly;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.FilterExceptFilter;
/**
 * Works with com.emuggie.spring.luncheon.application.controller.annotation.JsonOnly.
 * configuration at com.emuggie.spring.luncheon.application.config.JsonConfig.
 * @author emuggie
 *
 */
public class JsonOnlyPropertyFilter extends FilterExceptFilter {
	public static final String FILTER_ID = "JsonOnlyFilter";

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(JsonOnlyPropertyFilter.class);
	
	// for Set -> Array type conversion
	private static String[] fieldArray= new String[] {};

	// Object based filter rule map ideal for temporary(fields, method annotation + dynamic) rules.
	private Map<Object, JsonOnly> objectFilterRuleMap;
	
	// Class based filter rule map ideal for fixed on runtime.
	private Map<Class<?>, JsonOnly> globalFilterRuleMap;
	
	/**
	 * Add Global filter rule on initialization.
	 * Usage filter.addFilterRule(clazz, field1, field2).addFIlterRule(....
	 * @param clazz	Class to filter
	 * @param fields JsonOnlyFilter for serialization.
	 */
	public JsonOnlyPropertyFilter addFilterRule(
			Class<?> clazz
			, String...fields) {
		if(clazz == null || fields == null || fields.length == 0) {
			logger.error(String.format("Adding rule ignored. \nCause : Class[%s],fields[%s]."
					, clazz
					, fields
			));
			return this;
		}
		JsonOnly rule = this.globalFilterRuleMap.get(clazz);
		JsonOnly newRule = getNewRule(fields);
		
		if(rule != null) {
			newRule = getNewRule(
					getCoExistFields(rule, newRule)
					.toArray(fieldArray));
		}
		this.globalFilterRuleMap.put(clazz, newRule);
		return this;
	}
	
	public JsonOnly getGlobalFilterRule(Class<?> clazz) {
		return this.globalFilterRuleMap.get(clazz);
	}

	private JsonOnly getObjectRule(Object target) {
		return this.objectFilterRuleMap.get(target);
	}
	
	private void removeObjectRule(Object target) {
		this.objectFilterRuleMap.remove(target);
	}
	
	// Joins two object rules if rule previously registered.
	private void addObjectFilterRule(
			Object target
			, JsonOnly targetAnnotation) {
		
		if(this.objectFilterRuleMap.containsKey(target)) {
			targetAnnotation = getNewRule(
				getCoExistFields(
					this.objectFilterRuleMap.get(target)
					, targetAnnotation).toArray(fieldArray)
				);
		}
		this.objectFilterRuleMap.put(target, targetAnnotation);
	}

	public JsonOnlyPropertyFilter() {
		super(new HashSet<String>());
		// prevent memory leak.
		this.objectFilterRuleMap = new WeakHashMap<Object, JsonOnly>(); 
		this.globalFilterRuleMap = new HashMap<Class<?>, JsonOnly>();
	}

	@Override
	protected boolean include(PropertyWriter writer) {
		logger.debug("include ivoke");
		return this.getGlobalFilterRule(writer.getMember().getRawType())!=null;
	}

	@Override
	protected boolean include(BeanPropertyWriter writer) {
		logger.debug("Redirect to include(PropertyWriter)");
		return this.include((PropertyWriter) writer);
	}
	
	@Override
	public void serializeAsElement(Object elementValue, JsonGenerator jgen, SerializerProvider provider,
			PropertyWriter writer) throws Exception {
		logger.debug("serializeAsElement");
		super.serializeAsElement(elementValue, jgen, provider, writer);
	}
	
	@Override
	public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
			throws Exception {
		try {
			this.serializeAsFieldInternal(pojo, jgen, provider, writer);
		}catch(Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Wrapper for serializeAsField to catch throwable. 
	 * @param pojo
	 * @param jgen
	 * @param provider
	 * @param writer
	 * @throws Exception
	 */
	public void serializeAsFieldInternal(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
			throws Exception {
		Class<?> wrapperClass = writer.getMember().getDeclaringClass();
		Object wrapperObject = jgen.getOutputContext().getCurrentValue();
		String fieldName = writer.getName();
		Class<?> fieldType = writer.getMember().getRawType();
		Object fieldValue = writer.getMember().getValue(pojo);
		JsonOnly classAnnotation = wrapperClass.getAnnotation(JsonOnly.class);
		JsonOnly fieldAnnotation = writer.getAnnotation(JsonOnly.class);
		
		logger.debug(
			String.format("serializeAsField : field : %s:%s with [%s,%s] from [%s]"
				, fieldName
				, fieldType
				, classAnnotation
				, fieldAnnotation
				, wrapperClass
			)
		);
		// Wrapper class annotation : Add globalRule;
		
		this.addFilterRule(wrapperClass, classAnnotation==null?null:classAnnotation.value());
		// Add Object Rule(from field annotation) (will be used when child process)
		this.addTemporaryRule(
				fieldValue
				, fieldAnnotation);
		
		// Get target fields from global rules + object rules.
		Set<String> targetFields = getCoExistFields(
				this.getGlobalFilterRule(wrapperClass)
				, this.getObjectRule(wrapperObject));
		
		// Add sub node field rules for child processing.
		Set<String> subFields = this.getSubFields(targetFields, fieldName);
		this.addTemporaryRule(fieldValue, subFields.toArray(fieldArray));
		
		logger.debug("Applying rule : "+ targetFields);
		if(targetFields.stream()
				.anyMatch(each -> writer.getName().equals(each))) {
			writer.serializeAsField(pojo, jgen, provider);
			// Remove object rules : Clear temporary rule for next encounter.
			this.removeObjectRule(fieldValue);
		}else if (!jgen.canOmitFields()) { // since 2.3
            writer.serializeAsOmittedField(pojo, jgen, provider);
        }
	}
	
	/**
	 * Register Object to Serialization Info map for further processing.
	 * If Object is 
	 * 	- Iteratable or Arrays : Each object will be registered.
	 * 
	 * @param target Object to register.(null ignored)
	 * @param desc JsonOnly Annotation for field info.(null ignored)
	 */
	public void addTemporaryRule(Object target, JsonOnly annotation) {
		if(target == null || annotation == null) {
			return;
		}
			
		if(target.getClass().isArray()){
			for(Object each : (Object[])target) {
				this.addObjectFilterRule(each, annotation);
			}
		}else if(target instanceof Iterable) {
			((Iterable<?>)target).forEach(each->this.addObjectFilterRule(each, annotation));
		}else {
			this.addObjectFilterRule(target, annotation);
		}
	}
	
	/**
	 * temporary serialization rules for object(One time only).
	 * @param target Object to serialize
	 * @param fields field should only be.
	 */
	public void addTemporaryRule(Object target, String...fields) {
		if(target == null || fields == null || fields.length ==0) {
			return;
		}
		this.addTemporaryRule(target, getNewRule(fields));
	}
	
	/**
	 * Get co-existing field set from annotations given.
	 * @param annotations Set of annotations to get co-existing fields.
	 * @return Set of fields to process.
	 */
	private static Set<String> getCoExistFields(JsonOnly...annotations) {
		Set<String> fieldSet = new HashSet<String>();
		if(annotations == null || annotations.length == 0) {
			return fieldSet;
		}
		Stream.of(annotations).filter(each->each!=null).findFirst().ifPresent(
				first->Stream.of(first.value()).forEach(field->fieldSet.add(field))
		);
		// Init minimum fields with first.
		
		Set<String> remove = new HashSet<String>();
		for(JsonOnly each : annotations) {
			if(each == null || each.value() == null || each.value().length == 0) {
				continue;
			}
			fieldSet.stream()
				.filter(f1->Stream.of(each.value()).allMatch(f2->!f2.equals(f1)))
				.forEach(field->remove.add(field));
		}
		fieldSet.removeAll(remove);
		return fieldSet;
	}
	
	/**
	 * Retrieve object field specified as [field].[name].
	 * @param fields
	 * @param field
	 * @return Subfields to be serialized.
	 */
	private Set<String> getSubFields(Set<String> fields, String field){
		Set<String> subFields = new HashSet<String>();
		fields.stream().filter(each -> each.startsWith(field+".")).forEach(subField->subFields.add(subField.replaceFirst(field+".", "")));
		if(!subFields.isEmpty()) {
			fields.add(field);
		}
		return subFields;
	}
	
	private static JsonOnly getNewRule(String...fields) {
		if(fields == null || fields.length == 0) {
			return null;
		}
		return  new JsonOnly() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return JsonOnly.class;
			}

			@Override
			public String[] value() {
				return fields;
			}
		};
	}
}

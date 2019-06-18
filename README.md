# My Controversial Java : jackson

## Contents
* [Overview](#overview)
* [Usage](#usage)
* [How rules applied](#rule)

<a name="overview"></a>
## Overview

Jackson's custom JsonFilter to serialize only certain fields.
Java version 8 required.

<a name="usage"></a>
## Usage

First, add filter to your ObjectMapper.
```java
Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
SimpleFilterProvider fp = new SimpleFilterProvider();
fp.add(JsonOnlyPropertyFilter.FILTER_ID, new JsonOnlyPropertyFilter());
builder.filters(fp);
ObjectMapper mapper = builder.build;
```
Annotate Class or field with @JsonOnly with field to serialize.
```java
@JsonOnly({"field1", "field2",...})
Class Test {
    String field1;
    String field2;
    ...
}

```

Or, if you want to apply one-time-only rules for serialization, add new rules before serialization.
```java
JsonOnlyPropertyFilter filter = (JsonOnlyPropertyFilter) this.mapper.getSerializerProviderInstance().getFilterProvider().findPropertyFilter(JsonOnlyPropertyFilter.FILTER_ID, target);
filter.addObjectFilterRule(target, "field1","field2"...);
``

And finally, serialize with ObjectMapeer.
```java
String result = mapper.writeValueAsString(target);
```

<a name="rule"></a>
## How rules applied.

Ex 1. When class has annotated.
```java
@JsonOnly({"a", "b", "c"})
Class Test {
    String a = "a";
    String b = "b";
    String c = "c";
    int d = 1;
}
```
Result will be : {a:"a",b:"b",c:"c"}
      
Ex 2. Can target field's field too.
```java
@JsonOnly({"a", "b", "c.a"})
Class Test2 {
    String a = "a";
    String b = "b";
    Test c  = new Test();
}
```
Result will be : {a:"a",b:"b",c:{a:"a"}
  
Ex 3. Fields can be annotated too.
```java
@JsonOnly({"a", "b"})
Class Test2 {

    String a = "a";
    String b = "b";

    @JsonOnly({a, b})
    Test c  = new Test();
}
```
Result will be : {a:"a",b:"b",c:{a:"a",b:"b"}
      
Rules for same object will be merged.
  
For Spring framework (to annotate controller method for response handling too), go to mcj-jackson-spring.

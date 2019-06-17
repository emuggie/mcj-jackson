# mcj-jackson
################# My Controversial Java : jackson. #################
Contents : 
  @JsonOnly : Custom filter for serializing refered fields only.
  Usage : 
    Annotate Class or field with @JsonOnly with field to serialize.
    ex1> When class has annotated.
      @JsonOnly({a, b, c})
      Class Test {
        String a = "a";
        String b = "b";
        String c = "c";
        int d = 1;
      }
      -> result will be : {a:"a",b:"b",c:"c"}
      
   ex2> Works with field object.
      @JsonOnly({a, b})
      Class Test2 {
        String a = "a";
        String b = "b";
        Test c  = new Test();
      }
      -> result will be : {a:"a",b:"b"}
      
   ex3> Can target field's field too.
      @JsonOnly({a, b, c.a})
      Class Test2 {
        String a = "a";
        String b = "b";
        Test c  = new Test();
      }
      -> result will be : {a:"a",b:"b",c:{a:"a"}
  
  ex4> Fields can be annotated too.
      @JsonOnly({a, b})
      Class Test2 {
        String a = "a";
        String b = "b";
        @JsonOnly({a, b})
        Test c  = new Test();
      }
      -> result will be : {a:"a",b:"b",c:{a:"a",b:"b"}
      
  Rules for same object can be merged.
  
  For spring framework (to annotate method too), go to mcj-jackson-spring.

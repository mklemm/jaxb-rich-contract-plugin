## fluent-builder
### Motivation
There already is the widely used "fluent-api" plugin for XJC. That, however isn't a real builder pattern since there is no distinction between initialization and state change in fluent-api.

fluent-builder now creates a real "Builder" pattern, implemented as an inner class to the generated classes.

### Function
fluent-builder creates a builder class with a "[fluent interface](https://en.wikipedia.org/wiki/Fluent_interface)", and a number of methods to create builder instances.
The builder class is generated as a static inner class to all of the value object classes generated with XJC.
It supports the "episode" mechanism to generate builder code seamlessly across multiple compilation schema modules.

Example use in code:

        MyElement newElement = MyElement.builder().withPropertyA(...).withPropertyB(...).addCollectionPropertyA(...).build();

#### Additional Features

##### "Choice Expansion"
In standard JAXB, if you define a `<choice>` group in an XSD complexType definition with cardinality "many", the generated code will only contain a generic collection of "java.lang.Object" type, named something like "AorBorC...".

However, fluent-builder will determine exactly which types are actually possible in this collection, and will generate individual "addXXX" methods for each of them.

So, imagine you have generated code from the XHTML 1.0 schema, and you wish to use fluent-builder to generate an XHTML document programmatically.
Now, again imagine you have already created the "html" and "head" elements, and you are about to populate the "body" eith a table.

Without fluent-builder, you would do something like:

``` java
Body body = new Body();
Table table = new Table();
body.getPorH2orH2().add(table);
Tr tr = new Tr();
table.getTheadOrTrOrTdata().add(tr);
Td td = new Td();
tr.getTd().add(td);
td.setContent("Hello World");
```

With fluent-builder, you can achieve the same more intuitively:

```java
Body.builder().addTable().addTr().addTd().withContent("Hello World").end().end().end().build();
```


##### Object Deep-Copy strategies and Behaviors
In addition, new instances can be created as copies of existing instances using the builder, with an optional modification by other builder methods:

###### Static Deep Copy
        MyElement newElement = MyElement.copyOf(oldElement).withPropertyA(...).withPropertyB(...).build();

Or, similar to the java `clone()` method, creating a runtime copy of a reference:

###### Polymorphic Deep Copy
		MyObj myObj = oldObj.newCopyBuilder().with... .build();


###### Partial Copy (Static and Polymorphic)
The "partial" copy introduced in the "copy" plugin will work here as well, with both static (`copyOf()`) as well as polymorphic (`newCopyBuilder()`) behaviors:

        PropertyTree selection = MyElement.Select.root().propertyA().propertyAB().build();
        MyElement newElement = MyElement.copyExcept(oldElement, selection).withPropertyA(...).withPropertyB(...).build();
		MyObj myObj = oldObj.newCopyBuilder(selection, PropertyTreeUse.EXCLUDE).with.... .build();


###### Static vs. Polymorphic Deep Copy

The difference between `copyOf()` and `newCopyBuilder()` is their respective polymorphic behavior.
`newCopyBuilder()` always returns a builder instance that corresponds to the current runtime type of the object upon which the `newCopyBuilder()` method was invoked.
I.e., using `newCopyBuilder()`, you always get an object of exactly the same type as before as soon as you call `build()`.

In contrast, `MyClass.copyOf()`, being a static method, always returns an object of the class on which it is called, `MyClass` in this case.
You can pass an object of any base type (from the same XSD model or one referenced via "episode") or any derived type of `MyClass` to `copyOf()`, and you still get an instance of `MyClass` as
soon as you call `build()`.
If you pass an instance of a more general class than `MyClass` to `MyClass.copyOf()`, the generated code will only copy the fields that exist in the argument object, and will leave all additional fields uninitialized.
You should then initialize them with the other builder methods.

##### Chained Builder Support
Often, properties of generated classes represent containment or references to generated classes in the same model.
The fluent-builder plugin lets you initialise properties of such a type (and of types declared in upstream modules
via the "episode" feature) - if it isn't an abstract type - by using sub-builders ("chained" builders) in the following
way, given that both A and B are types defined in the XSD model, and A has a property of type B, and B has three
properties of type String, x,y, and z:

        A newA = A.builder().withB().withX("x").withY("y").withZ("z").end().build();

Of course, this plugin is most useful if `immutable` is also activated.


### Limitations
* It generates a large amount of code.
* Note: Shared builder instances are NOT thread-safe by themselves.

### Usage
#### -Xfluent-builder

#### Options

##### -rootSelectorClassName=`<string>` (Select)
Name of the generated nested static "Select" entry point class to be used by client code for the "partial copy" feature. This setting will only have an effect if the "deep-copy-plugin" isn't also active. If it is, the "copy" plugin's settings will take precedence.


##### -newBuilderMethodName=`<string>` (builder)
Name of the generated static method to instantiate a new fluent builder. Can be set to handle naming conflicts.


##### -newCopyBuilderMethodName=`<string>` (newCopyBuilder)
Name of the generated instance method to instantiate a new fluent builder intitialized with a copy of the current instance.


##### -copyToMethodName=`<string>` (copyTo)
Name of the generated "copyTo" method.


##### -builderFieldSuffix=`<string>` (_Builder)
Suffix to append to the field holding the builder, change to  prevent name clashes.


##### -generateTools=`{y|n}` (y)
Generate utility classes as static source code artifacts. If no, the plugin JAR must be in compile- and runtime classpath.


##### -narrow=`{y|n}` (n)
Uses copy constructors for all child nodes in the object tree as long as they are available. This will cause the new instance to be as narrow as possible to the declared types.
Abstract types and types not generated from this XSD-model will always be copied by their "clone()"-method.


##### -copyPartial=`{y|n}` (y)
Generates an additional 'copyOf'-method  that takes a PropertyTree instance to restrict the copy operation to selected nodes in the object tree.


##### -selectorClassName=`<string>` (Selector)
Name of the generated nested "Selector" builder class, used to build up a property tree for partial copy functionality. This setting will only have an effect if the "deep-copy-plugin" isn't also active. If it is, the "copy" plugin's settings will take precedence.


##### -builderClassName=`<string>` (Builder)
Name of the generated nested builder class. Can be set to handle naming conflicts.


##### -builderInterfaceName=`<string>` (BuildSupport)
Name of the generated nested builder interface. Can be set to handle naming conflicts.


##### -copyAlways=`{y|n}` (n)
If true, generate code of fluent-builder "withXXX" methods so that all objects passed to the builder are inherently deep-copied.


##### -buildMethodName=`<string>` (build)
Name of the generated "build" method that concludes building and returns the product. Can be set here to handle naming conflicts.


##### -endMethodName=`<string>` (end)
Name of the generated "end" method that concludes a nested builder and returns to the outer builder. Can be set here to handle naming conflicts.


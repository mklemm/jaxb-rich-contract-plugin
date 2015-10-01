## fluent-builder
### Motivation
There already is the widely used "fluent-api" plugin for XJC. That, however isn't a real builder pattern since there is no distinction between initialization and state change in fluent-api.

fluent-builder now creates a real "Builder" pattern, implemented as an inner class to the generated classes.

### Function
fluent-builder creates a static inner class for every generated class representing the builder, and a static method on the generated class to create a builder.

Example use in code:

        MyElement newElement = MyElement.builder().withPropertyA(...).withPropertyB(...).addCollectionPropertyA(...).build();

In addition, new instances can be created as copies of existing instances using the builder, with an optional modification by other builder methods:

        MyElement newElement = MyElement.copyOf(oldElement).withPropertyA(...).withPropertyB(...).build();

Or, similar to the java `clone()` method, creating a runtime copy of a reference:

		MyObj myObj = oldObj.newCopyBuilder().with... .build();

The "partial" copy introduced in the "copy" plugin will work here as well:

        PropertyTree selection = MyElement.Select.root().propertyA().propertyAB().build();
        MyElement newElement = MyElement.copyExcept(oldElement, selection).withPropertyA(...).withPropertyB(...).build();
		MyObj myObj = oldObj.newCopyBuilder(selection, PropertyTreeUse.EXCLUDE).with.... .build();

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


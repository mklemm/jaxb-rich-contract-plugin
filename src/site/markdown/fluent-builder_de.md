## fluent-builder
### Motivation
There already is the widely used "fluent-api" plugin for XJC. That, however isn't a real builder pattern since there is no distinction between initialization and state change in fluent-api.

fluent-builder now creates a real "Builder" pattern, implemented as an inner class to the generated classes.

### Funktion
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

#### Javadoc genration from schema annotations
By default _xjc_ will add class level Javadoc content from the `xs:annotation/xs:documentation` element of the corresponding named/anonymous complex type or root element.
If you enable `generateJavadocFromAnnotations` fluent-builder will additionally generate javadoc comments for class getters/setters and builder add/with methods.
This enables you to add rich documentation to the schema that will also be available in the JAXB generated code.
The content of the `<xs:documentaion>` element will be added to the head of the existing javadoc comment section as a new paragraph.

##### Class level Javadoc
Class level Javadoc comments are generated by _xjc_ from anonymous compex types as follows:

```xml
  <element name="root-element">
    <complexType>
      <annotation>
        <documentation>This is my Anonymous complex type annotation.</documentation>
      </annotation>
      <sequence>
        <element name="child-element" type="string">
```

This will produce Javadoc similar to:

```java
/**
 * This is my Anonymous complex type annotation.
 * 
 * <p>Java class for anonymous complex type.
 * ...
 */
 // ...
 public class RootElement implements Cloneable
```

Class level javadoc comments are also generated from named complex types as follows:

```xml
  <complexType name="some-complex-type">
    <annotation>
      <documentation>This is my named complex type annotation</documentation>
    </annotation>
    <sequence>
      <element name="some-element" type="string">
```

This will produce Javadoc similar to:

```java
/**
 * This is my named complex type annotation
 * 
 * <p>Java class for some-complex-type complex type.
 * ...
 */
 // ...
 public class SomeComplexType implements Cloneable
```

##### Method level Javadoc

Method level Javadoc will be added to the get.../set... methods that corresponds to the schema `element` or `attribute` that contains the `annotation/documentation` element. It will also be added to the with.../add... builder methods.

For example, the schema annotation should be as follows:

```xml
    <sequence>
      <element name="some-element" type="string">
        <annotation>
          <documentation>This is my element annotation</documentation>
        </annotation>
      </element>
```

This will produce java code in the corresponding class along the lines of:

```java
    /**
     * This is my element annotation
     * <P>
     * Gets the value of the someElement property.
     * ...
     */
    public String getSomeElement() {
        return someElement;
    }

    /**
     * This is my element annotation
     * <P>
     * Sets the value of the someElement property.
     * ...
     */
    protected void setSomeElement(String value) {
        this.someElement = value;
    }
```

And java code in the corresponding Builder class as follows:

```java
        /**
         * This is my element annotation
         * <P>
         * Sets the new value of "someElement" (any previous value will be replaced)
         * ...
         */
        public SomeComplexType.Builder<_B> withSomeElement(final String someElement) {
            // ...
        }
```



### Enschränkungen
* It generates a large amount of code.
* Note: Shared builder instances are NOT thread-safe by themselves.

### Aktivierung
#### -Xfluent-builder

#### Optionen

##### -rootSelectorClassName=`<string>` (Select)
Name der generierten inneren "Select" -Klasse, die vom aufrufenden Code als Einstieg in den Aufbau eines Property-Baumes für das partielle Kopieren verwendet werden kann. Diese Einstellung wird nur dann berücksichtigt, wenn das "Deep Copy"-Plugin nicht aktiv ist, und "copy-partial=y" ist. Ansonsten gilt die Einstellung des "Deep Copy"-Plugins.


##### -newBuilderMethodName=`<string>` (builder)
Name der generierten statischen Methode zum Erzeugen eines neuen Builders. Kann hier gesetzt werden, um Namenskonflikte zu lösen.


##### -newCopyBuilderMethodName=`<string>` (newCopyBuilder)
Name der generierten Instanzmethode zum Erzeugen eines neuen Builders, der mit dem von dieser Instanz kopierten Zustand initialisiert ist.


##### -copyToMethodName=`<string>` (copyTo)
Name der generierten Methode zum kopieren des internen Zustands dieses Builders auf einen anderen Builder.


##### -builderFieldSuffix=`<string>` (_Builder)
Suffix, das an den Namen der generierten Sub-Builder Instanzvariablen angefügt wird.


##### -generateTools=`{y|n}` (y)
Generiere Hilfsklassen als Quelltext. Wenn dies ausgeschaltet ist, muss sich das Plugin-JAR zur Laufzeit im Klassenpfad der generierten Klassendefinitionen befinden.


##### -narrow=`{y|n}` (n)
Für untergeordnete Knoten im zu kopierenden Objektbaum werden ebenfalls die Copy-Konstruktoren der deklarierten Typen verwendet, soweit diese vorhanden sind und die Typen der entsprechenden Instanzen ebenfalls aus dem XSD-Model generierte Klassen sind. Dies erzeugt eine möglichst "schmale" Kopie des Ausgangsobjekts, was in bestimmten Fällen nützlich sein kann.
Ein Unterknoten, dessen Typ nicht im aktuellen XSD-Modell deklariert ist, wird immer wie bei der 'clone()'-Methode kopiert. Ist diese Option "no", gilt dies auch für generierte Typen.


##### -copyPartial=`{y|n}` (y)
Generiert zusätzlich eine 'copyOf()'-Methode mit der sich Objekte partiell kopieren lassen. Dabei wird ein PropertyTree-Objekt mitgegeben, welches die zu kopierenden Knoten des Objektbaumes angibt.


##### -selectorClassName=`<string>` (Selector)
Name der generierten inneren "Selector" Builder-Klasse, die intern zum Aufbau des Property-Baums für das partielle Kopieren benutzt wird. Diese Einstellung wird nur dann berücksichtigt, wenn das "Deep Copy"-Plugin nicht aktiv ist, und "copy-partial=y" ist. Ansonsten gilt die Einstellung des "Deep Copy"-Plugins.


##### -builderClassName=`<string>` (Builder)
Name der generierten inneren Builder-Klasse. Kann hier gesetzt werden, um Namenskonflikte zu lösen.


##### -builderMethodPrefix=`<string>` (with)
Prefix, das vor den Namen der Buildermethoden angehängt wird.


##### -builderInterfaceName=`<string>` (BuildSupport)
Name des generierten inneren Builder-Interfaces. Kann hier gesetzt werden, um Namenskonflikte zu lösen.


##### -copyAlways=`{y|n}` (n)
Ist diese Option 'yes', werden alle Variablen-Methoden, die JAXB-generierte Objekte akzeptieren, so generiert, dass die übergebenen Objekte kopiert werden.


##### -copyOfBuilder=`{y|n}` (y)
Ist diese Option 'yes', wird eine copyOf Methode für den fluent-builder generiert.


##### -buildMethodName=`<string>` (build)
Name der generierten "build"-Methode, die das gebaute Objekt zurückliefert.


##### -endMethodName=`<string>` (end)
Name der generierten "end"-Methode, die einen sub-Builder beendet.


##### -generateJavadocFromAnnotations=`{y|n}` (n)
TODO


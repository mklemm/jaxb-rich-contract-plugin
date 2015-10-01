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


##### -builderInterfaceName=`<string>` (BuildSupport)
Name des generierten inneren Builder-Interfaces. Kann hier gesetzt werden, um Namenskonflikte zu lösen.


##### -copyAlways=`{y|n}` (n)
Ist diese Option 'yes', werden alle withXXX-Methoden, die JAXB-generierte Objekte akzeptieren, so generiert, dass die übergebenen Objekte kopiert werden.


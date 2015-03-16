## fluent-builder
### Motivation
There already is the widely used "fluent-api" plugin for XJC. That, however isn't a real builder pattern since there is no distinction between initialization and state change in fluent-api.

fluent-builder now creates a real "Builder" pattern, implemented as an inner class to the generated classes.

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



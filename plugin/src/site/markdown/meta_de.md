## meta
### Motivation
Sometimes, you need information about the properties of a class, or you wish to have a constant for the names of properties.
The "meta" plugin creates an inner class (the name of which can be controlled by a command-line option), and adds a constant
field for each property. If the `-extended=y` command-line option is specified, these constants will hold instances of the
`PropertyInfo` class, on which the name, type, multiplicity (collection or not) and default value (from XSD) are exposed.
Without `-extended`, the constants are simply string constants holding the property names.

In Version 1.10 wurde neu ein "Visitor"-Muster eingeführt, über das sich der gesamte Objektgraph durchlaufen lässt.


### Aktivierung
#### -Xmeta

#### Optionen

##### -meta.generateTools=`{y|n}` (y)
Generiere die Hilfsklasse zur Darstellung der erweiterten Metadaten als Quelltext.
Wenn hier "n" angegeben wird und "extended=y", so muss das plugin JAR zur Laufzeit im Klassenpfad der client-Anwendung sein.


##### -meta.extended=`{y|n}` (n)
Generiere erweiterte Metadaten für jedes Property: Name, Typ, Multiplizität, Standardwert


##### -meta.camelCase=`{y|n}` (n)
Namen der Konstanten wie Feldnamen generieren, nicht nach Java-Konstanten-Konvention.


##### -meta.metaClassName=`<string>` (PropInfo)
Name der generierten inneren Metainfoklasse.


##### -meta.allowSet=`{y|n}` (y)
Generiere eine Möglichkeit, den Wert eines Propertys über die Metadaten zu ändern.


##### -meta.visitMethodName=`<string>` (visit)
Name der Methode, mit der ein Visitor durch das Objekt geschickt werden kann.


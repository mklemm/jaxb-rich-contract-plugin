## meta
### Motivation
Sometimes, you need information about the properties of a class, or you wish to have a constant for the names of properties.
The "meta" plugin creates an inner class (the name of which can be controlled by a command-line option), and adds a constant
field for each property. If the `-extended=y` command-line option is specified, these constants will hold instances of the
`PropertyInfo` class, on which the name, type, multiplicity (collection or not) and default value (from XSD) are exposed.
Without `-extended`, the constants are simply string constants holding the property names.

As a new feature in version 1.10, a visitor pattern has been added that allows to visit all properties of
an object graph.


### Usage
#### -Xmeta

#### Options

##### -meta.generateTools=`{y|n}` (y)
Generate helper class used to represent extended metadata as source code.
If this is set to "n" and "-extended=y", the plugin JAR will have to be in the runtime classpath of the client application.


##### -meta.extended=`{y|n}` (n)
Generate extended meta data for each property: Name, type, multiplicity, default value.


##### -meta.camelCase=`{y|n}` (n)
Generate names of constant meta fields like field names, instead of Java constant name convention.


##### -meta.metaClassName=`<string>` (PropInfo)
Name of the generated meta-information nested class.


##### -meta.allowSet=`{y|n}` (y)
Allow property values to be set via property meta information.


##### -meta.visitMethodName=`<string>` (visit)
Name of the method to apply a visitor.


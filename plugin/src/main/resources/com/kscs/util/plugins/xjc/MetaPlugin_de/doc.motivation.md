Sometimes, you need information about the properties of a class, or you wish to have a constant for the names of properties.
The "meta" plugin creates an inner class (the name of which can be controlled by a command-line option), and adds a constant
field for each property. If the `-extended=y` command-line option is specified, these constants will hold instances of the
`PropertyInfo` class, on which the name, type, multiplicity (collection or not) and default value (from XSD) are exposed.
Without `-extended`, the constants are simply string constants holding the property names.

In Version 1.10 wurde neu ein "Visitor"-Muster eingeführt, über das sich der gesamte Objektgraph durchlaufen lässt.

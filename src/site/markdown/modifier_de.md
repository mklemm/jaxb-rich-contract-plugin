## modifier
### Motivation
Sometimes you wish to make as many of your classes immutable as possible, but in some scenarios they should still be modifiable. This plugin allows you to create a controlled entry point for modifications like this.

### Function
This plugin simply creates an inner instance class that provides access through tne normal setXXX methods and getXXX methods for collections that return the mutable version of the collection property.

### Aktivierung
#### -Xmodifier

#### Optionen

##### -modifierClassName=`<string>` (Modifier)
Name der generierten Mutator-Klasse (wenn generateModifier=y)


##### -modifierMethodName=`<string>` (modifier)
Name der generierten methode zum Abruf einer Instanz der Mutator-Klasse (wenn generateModifier=y)



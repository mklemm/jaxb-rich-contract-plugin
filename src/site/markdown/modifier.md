## modifier
### Motivation
In general, you may wish to implement application logic in a way so that objects are initialized once
and then are immutable.
For traditional programming languages, like Java, for example, this is not always feasible in practice,
because legacy code and libraries have to be used.

With the `modifier` plugin, you can make the public interface of your classes immutable via the `immutable`
plugin, but at the same time provide a handle to modify the state of your objects anyway vi a reference that
needs to be queried explicitly.

This plugin is intended for use while refactoring existing code to a more "functional" and thread-friendly
code base. Eventually, your code should work so this plugin can be deactivated in your XJC configuration.


### Function
This plugin creates an inner class with public setXXX methods, and getXXX methods for collection properties that
return a writable version of the collection the property is implemented by.

If the `group-contract` plugin is also activated, these constructs will also be generated into the interfaces.


### Usage
#### -Xmodifier

#### Options

##### -modifierClassName=`<string>` (Modifier)
Name of the generated inner class that allows to modify the state of generated objects.


##### -modifierMethodName=`<string>` (modifier)
Name of the generated method that allows to instantiate the modifier class.



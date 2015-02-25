## clone
### Motivation
Another way to create a deep copy of an object tree. This adheres to the `java.lang.Cloneable` contract, but isn't as versatile as `-Xcopy`.

### Function
The `clone` plugin generates a deep clone method for each of the generated classes, based on the following assumptions:

* Objects implementing `java.lang.Cloneable` and are cloneable by their "clone" Method.
* Objects not implementing `java.lang.Cloneable` or primitive types are assumed to be immutable, their references are copied over, they are not cloned.

### Bugs
The `-cloneThrows` option should in fact never have existed.

### Limitations
There is currently no way for the plugin to determine whether an object in the object graph that isn't cloneable actually is immutable so its reference can be copied. So, there is no guarantee that cloned object graphs are really independent of each other, as mandated by the `java.lang.Cloneable` contract.

### Usage
#### -Xclone

#### Options

##### -cloneThrows=`{y|n}` (y)
Declare CloneNotSupportedException to be thrown by 'clone()' (yes), or suppress throws clause and wrap all `CloneNotSupportedException`s as `RuntimeException` (no).
If you set this to `no`, the resulting code will violate the `java.lang.Cloneable` contract, since it is stated that an object that cannot be cloned should throw CloneNotSupportedException, and nothing else. This option has been added, however, to support legacy code that doesn't catch CloneNotSupportedExceptions.



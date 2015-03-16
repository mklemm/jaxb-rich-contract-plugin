## meta
### Usage
#### -Xmeta

#### Options

##### -generateTools=`{y|n}` (y)
Generate helper class used to represent extended metadata as source code.
If this is set to "n" and "-extended=y", the plugin JAR will have to be in the runtime classpath of the client application.


##### -extended=`{y|n}` (n)
Generate extended meta data for each property: Name, type, multiplicity, default value.


##### -camelCase=`{y|n}` (n)
Generate names of constant meta fields like field names, instead of Java constant name convention.


##### -metaClassName=`<string>` (PropInfo)
Name of the generated meta-information nested class.



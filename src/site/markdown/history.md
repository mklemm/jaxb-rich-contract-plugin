###  Version History

* **1.0.0**: Initial Version
* **1.0.1**: Added constrained-property plugin
* **1.0.2**: Added partial clone method generation
* **1.0.3**: Improvements in partial clone
* **1.0.4**: Added fluent builder and immutable plugins
* **1.0.5**: Added chainable fluent builder support
* **1.1.0**: New: `-Ximmutable`, Copy constructor support, fluent-builder copy from instance support, general fixes. Removed option to generate fluent builders without chained builder support.
* **1.1.1**: New: Type-safe selector support for partial clone/copy logic.
* **1.1.2**: Big fixes in selector logic
* **1.1.3**: Minor bug fixes in fluent-builder
* **1.1.4**: Fixed an error in fluent-builder where an initialization method wasn't properly overridden in derived builder classes, leading to the wrong builder type being returned when using chained sub-builders.
* **1.1.5**: Fixed error in Release Build process
* **1.1.6**: Fixed bug in group-contract plugin: Property names customised via binding info were generated incorrectly in the interface definitions.
* **1.2.0**: Major changes to the logic of partial cloning. The partial clone `PropertyTree` pattern replaces the previous `PropertyPath`, which had pretty unclear semantics. The new `PropertyTree` builders now just create a property tree, and on invocation of the "clone()" or "copyOf()" methods or the copy constructor, it is decided by an additional parameter whether the property tree should be considered an exclusion or an inclusion pattern. Additionally, the group-interface plugin has been modified to create interfaces also for the fluent builders, if the fluent-builder plugin is activated.
* **1.2.3**: Added "Copyable" interface and "createCopy" method which does the same thing as the "clone()" method, but doesn't suffer from the defective-by-design java.lang.Cloneable contract. It is planned to als add a "copyFrom" method to copy the state of another object into an existing object.
* **1.3.1**: Made fluent-builder plugin work in an "episode" (modular generation and compilation) context by also integrating compiled classes on the XJC classpath in the search for base and property classes.
* **1.3.6**: Also made group-interface work in an "episode" context, and fixed bug where empty interfaces were created if no implementation class for them could be found in the current module.
* **1.4.0**: group-interface is using its own episode file to maintain relationships to definitions in upstream modules. Command-line options for a specific plugin must now be given immediately after the plugin activation option ("-X..."). This way, name conflicts between plugin options are avoided. Static source files are generated via the JCodeModel.addResourceFile API, so a bug where the source files ended up in the root of the project tree should be fixed now. group-interface and fluent-builder now are working together more reliably.
* **1.5.0**: Added new Plugin "-Xmeta" to generate an inner class containing static meta information about the properties of a class. Internally, a common base class for plugins was extracted to help in command-line parsing and command-line documentation.
* **1.5.1**: Major updates to documentation, improvements to `-Xmeta` to expose static information about XSD definitions of properties.
* **1.5.2**:
    * Now hosted on Central
    * More updates to documentation
    * Customization of names of many generated source elements
    * Improved handling of CloneNotSupportedException in clone, copy, and fluent-builder plugins
* **1.5.3**:
	* Added maven "site" hosted on github pages
	* Improvements to javadoc comment generation
	* Improvements to documentation
* **1.5.4**:
	* Updates to generated documentation
	* changed groupId to net.codesup.util
* **1.5.5**:
	* immutable plugin: Added command line option to specify access level of default constructor
* **1.5.6**:
	* Added instance "newCopyBuilder" method generation
* **1.5.7**:
	* Fixed bug where partial copying in a builder didn't work
* **1.5.8**:
	* Bugfix: When generating builder interface, not all superinterfaces were declared in the "extends" clause.
	* Added command-line option to configure whether methods that could cause type clashes should be ommitted.
	* Added command-line option to configure suffix for instance fields of a builder holding sub-builders
* **1.5.9**:
	* fluent-builder: Added methods to initialize collection properties with an "java.util.Iterable" instance instead of collection.
	* fluent-builder: Made "add..." and "with..." methods for collection properties fall through if they are given a NULL arg for the item collection.
* **1.6.0**:
	* immutable: You can now have a "modifier" class generated that provides methods to modify the state of an otherwise immutable object anyway.
* **1.6.1**:
	* minor bugfixes
* **1.6.2**:
	* immutable: Introduced alternate collection type when generating immutable collection properties
	* made more names of generated items configurable
* **1.6.3**:
	* Added "fake" mode for immutable, only for test purposes
* **1.6.4**:
	* group-contract: when generating methods that could conflict with each other in cases where two interfaces are
	used at the same time as generic type parameter boundaries, an extra level of interfaces is declared so that the
	potentially problematic methods are in their own interface definition which can be omitted in your code if desired.
	* Issue #16 resolved.
	* clone: Resolved an issue with generating the "throws CloneNotSupportedException" declarations. Now they are only generated
	if actually needed.
	* Put "modifier" generation into separate plugin class.
* **1.6.5**:
	* fluent-builder: Changed logic of static "copyOf" method to allow widening type conversion of input parameter.
* **1.6.6**:
	* fluent-builder: Changed type parameter names to make name conflicts less likely
* **1.6.7**:
	* Fixed a bug in fluent-builder generation that could prevent builder chaining under some circumstances
* **1.6.8**:
	* Integrated a fix from JulianPaoloThiry
	* Improved fluent builder to support building of sub-objects defined in a `choice`. All possible have a buidler method now,
	so, for example, in XHTML, instead of doing somethoin like `htmlBuilder.withPorH1orH2(H1.builder().withContent("bla").build())....` you can do
	`htmlBuilder.withH1().withContent("bla").end()....`.
* **1.6.9**:
	* Fixed errors in release workflow
* **1.6.10**:
	* Changes in group-contract plugin when handling XSD input sources.
* **1.7.0**:
	* Modified fluent builder behavior so that it creates a deep copy of all child objects passed to the builder methods,
	if the child object is an instance of a JAXB-generated class from the same compilation unit or one of its episode-dependencies.
* **1.8.0**:
	* Made behavior introduced in 1.7.0 switchable with command-line parameter, because it isn't desirable in many cases.
	* Improved site and documentation generation
* **1.9.0**:
	* Refactored out common classes to jaxb-plugin-lib module
	* Fixed override of choice expansion builder methods
	* Fixed possible name clashes with choice expansion
* **1.10.0**:
	* Added visitor pattern to meta plugin
	* Added property access via meta plugin
* **1.11.0**:
	* Made "visit" method return "this" for easier chaining.
* **1.11.1**:
	* Fixed bug in constant name generation
* **1.11.1**:
	* Fixed regression bug in builder where non-copied objects were not saved
* **1.12.0**
	* Enable visitor to visit root object
* **1.13.0**
	* Fixed instance access to static field generation in meta plugin
	* Fixed choice expansion to work with collections not only of complexTypes, but of simpleTypes as well
* **1.14.0**
	* Added "visit" Method to generated interfaces.
* **1.15.0**
	* Moved "visit" Method to "Lifecycle" interfaces.





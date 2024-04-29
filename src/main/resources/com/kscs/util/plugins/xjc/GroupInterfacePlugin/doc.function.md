The group-contract plugin now tries to model that case in the generated source code. For every `group`and `attributeGroup`
definition in the XSD model (or in any upstream XSD model that is included via the "episode" mechanism, for that matter),
it generates an `interface` definition with all the getter, and optionally setter, methods of the properties defined via
the `group` or `attributeGroup` definition.

Then, it declares every class that was generated from a `complexType` that uses the `group` or `attributeGroup` as implementing
just that interface. This way, all classes generated from XSD complexTypes that use the same group definitions, will
share a common contract and can be treated in a common way by client code.

If the "fluent-builder" plugin is also activated, the interface definition can optionally include the declarations of the "with..."
and "add..." methods of the generated builder class as a nested interface declaration, so you can even rely on a common
"builder" contract for classes using the same `group` and `attributeGroup` definitions.

For example, you may wish to add "XLink" functionality to your generated classes. If the group-contract plugin is
activated, you can define a complexType in XSD that supports the "simple" attributes by adding to its XSD definition:

``` xml
<complexType name="some-type">
	.... (model group of the type...)
	<attributeGroup ref="xlink:simpleAttrs"/>
</complexType>
```

Which will generate a class something like:

``` java
public class SomeType implements SimpleAttrs {
...
```

And an interface definition like:

``` java
public interface SimpleAttrs {
	String getHref();
	void setHref(final String value);
	// ... more properties ...

	// this part is generated only if fluent-builder is also active
	interface BuildSupport<TParentBuilder >{
            public SimpleAttrs.BuildSupport<TParentBuilder> withHref(final String href);
            //... more properties ...
	}
}
```

Similar effects could be achieved by subclassing complexTypes, but since there is no multiple inheritance, inheritance
hierarchies can get overly complex this way, and inheritance is less flexible than interface implementations.

**Note:** The group-contract plugin supports JAXB modular compilation, i.e. the "episode" mechanism implemented
in the JAXB reference implementation.
However, due to the lack of extensibility of the current default episode data structures and processing, this plugin
has to manage its own "episode" file. There are two command line options to control the  names of the "upstream" episode
file, i.e. the file name the plugin should look for when using other modules, and the "downstream" file, i.e. the file
name that should be generated for use by other modules.

**Note:** The group-contract-plugin supports, since 4.1.3, a rudimentary mechanism to modify the name of the generated
interface similar to the <class... customization supported by standard JAXB.
Unfortunately, XJC doesn't allow us to place a plugin customization on a group or attributeGroup definition, so the
approach is a bit awkward: The plugin customization has to be applied on the global (schema) level in the following way:

``` xml
<?xml version="1.0" encoding="utf-8" ?>

<jxb:bindings version="3.0" xmlns:jxb="https://jakarta.ee/xml/ns/jaxb" xmlns:xs="http://www.w3.org/2001/XMLSchema"
			  xmlns:gi="http://www.kscs.com/util/jaxb/bindings">

	<jxb:bindings schemaLocation="my-xml-schema-file.xsd" node="/xs:schema">
		<gi:interface groupNamespace="http://my-target-namespace" groupName="MyModelGroup" name="MyChangedModelGroupName"/>
	</jxb:bindings>

</jxb:bindings>

```


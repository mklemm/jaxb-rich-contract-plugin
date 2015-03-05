fluent-builder creates a static inner class for every generated class representing the builder, and a static method on the generated class to create a builder.

Example use in code:

        MyElement newElement = MyElement.builder().withPropertyA(...).withPropertyB(...).addCollectionPropertyA(...).build();

In addition, new instances can be created as copies of existing instances using the builder, with an optional modification by other builder methods:

        MyElement newElement = MyElement.copyOf(oldElement).withPropertyA(...).withPropertyB(...).build();

Or, similar to the java `clone()` method, creating a runtime copy of a reference:

		MyObj myObj = oldObj.newCopyBuilder().with... .build();

The "partial" copy introduced in the "copy" plugin will work here as well:

        PropertyTree selection = MyElement.Select.root().propertyA().propertyAB().build();
        MyElement newElement = MyElement.copyExcept(oldElement, selection).withPropertyA(...).withPropertyB(...).build();
		MyObj myObj = oldObj.newCopyBuilder(selection, PropertyTreeUse.EXCLUDE).with.... .build();

Often, properties of generated classes represent containment or references to generated classes in the same model.
The fluent-builder plugin lets you initialise properties of such a type (and of types declared in upstream modules
via the "episode" feature) - if it isn't an abstract type - by using sub-builders ("chained" builders) in the following
way, given that both A and B are types defined in the XSD model, and A has a property of type B, and B has three
properties of type String, x,y, and z:

        A newA = A.builder().withB().withX("x").withY("y").withZ("z").end().build();

Of course, this plugin is most useful if `immutable` is also activated.

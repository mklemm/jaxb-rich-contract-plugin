In most object-oriented programming languages, there are constructs to define a "contract", that concrete implementations of complex
types will implement. In Java, for example, there is the `interface`, in Scala there are "traits", and so on.
The XML Schema Definition Language (XSD) in contrast, has no explicit construct to ensure a complex type meets a
pre-defined contract. There are, however, the `group` and `attributeGroup` elements, that could be considered
a way to achieve just that: A complexType that uses a `<group>` or an `<attributeGroup>` will expose the
properties defined in these group definitions. Looking at it that way, you could say that the `complexType`
"implements" the contract defined by the `group` or `attributeGroup`.


## immutable
### Motivation
Generally it is advisable to make your business classes immutable as much as possible, to minimise side effects and allow for functional programming patterns.

### Function
This plugin simply makes all "setXXX" methods "protected", thus preventing API consumers to modify state of instances of generated classes after they have been created. This only makes sense together with another plugin that allows for initialization of the instances, like e.g. the included `fluent-builder` plugin. For collection-valued properties, `-Ximmutable` wraps all collections in a `Collections.unmodifiableCollection`, so collections are also made immutable. Because JAXB serialization has a number of constraints regarding the internals of JAXB serializable objects, it wasn't advisable to just remove the setter methods or replace the collections with unmodifiable collections. So, a bit of additional code will be created that leaves the old "mutable" structure of the class intact as much as is needed for JAXB, but modifies the public interface so objects appear immutable to client code.

### Limitations
* Access level "protected" may not be strict enough to prevent state changes.
* If you activate plugins like "fluent-api" or the like, these plugins may circumvent the protection provided by the `immutable` plugin.

### Aktivierung
#### -Ximmutable

#### Optionen

##### -fake=`{y|n}` (n)
Nur für Test und Debug: Es wird nichts wirklich unveränderlich gemacht, aber das Plugin bleibt aktiv.


##### -overrideCollectionClass=`<string>` (null)
Modify collection getters to be declared to return a custom type implementing java.lang.Iterable instead of List.


##### -constructorAccess=`<string>` (public)
Setzt die Sichtbarkeit des von JAXB geforderten argumentlosen Konstruktors auf den angegebenen Wert ("public", "private", "protected", "default"). Die JAXB-Spezifikation fordert eigentlich, dass der Konstruktor "public" sein soll, aber in vielen Implementierungen funktioniert auch "protected". Diese Option wurde eingeführt, da es normalerweise wenig sinnvoll ist, ein leeres Objekt zu erzeugen, das danach nicht mehr verändert werden kann. Dennoch ist dies nicht standardkonform und daher mit Vorsicht zu benutzen.



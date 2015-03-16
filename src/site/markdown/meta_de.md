## meta
### Aktivierung
#### -Xmeta

#### Optionen

##### -generateTools=`{y|n}` (y)
Generiere die Hilfsklasse zur Darstellung der erweiterten Metadaten als Quelltext.
Wenn hier "n" angegeben wird und "extended=y", so muss das plugin JAR zur Laufzeit im Klassenpfad der client-Anwendung sein.


##### -extended=`{y|n}` (n)
Generiere erweiterte Metadaten für jedes Property: Name, Typ, Multiplizität, Standardwert


##### -camelCase=`{y|n}` (n)
Namen der Konstanten wie Feldnamen generieren, nicht nach Java-Konstanten-Konvention.


##### -metaClassName=`<string>` (PropInfo)
Name der generierten inneren Metainfoklasse.



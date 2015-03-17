## modifier
### Motivation
Generell ist es vorteilhaft, Anwendungslogik so zu implementieren, dass Objekte in der Regel nach der
Initialisierung in ihrem Zustand unveränderlich sind. In traditionellen Programmiersprachen wie z.B.
Java bleibt dies jedoch oftmals ein akademischer Ansatz, da oft auf bestehendem Code und bestehenden Bibliotheken
aufgesetzt werden muss, die ein derartiges Programmiermodell nicht oder nur unzulänglich unterstützen.

Das `modifier`-Plugin schafft eine Möglichkeit, einerseits (z.B. durch das `immutable`-Plugin) die allgemeine
Schnittstelle einer Klasse so zu definieren, dass darüber keine Zustandsänderungen am Objekt möglich sind,
aber gleichzeitig für bestimmte Szenarien eine explizit abzurufende Referenz bereit zu stellen, über die das
Objekt dennoch einfach verändert werden kann.

Der Einsatz dieses Plugins ist hauptsächlich für eine Übergangszeit während der Refaktorierung von existierendem
Code vorgesehen, sodass zur Compilezeit die Stellen im Code deutlich werden, die zustandsveränderliche
Objekte voraussetzen. Ziel sollte es dann sein, dieses Plugin irgendwann im eigenen Projekt abschalten zu können.


### Function
Es wird eine innere Klasse generiert, die öffentliche setXXX-Methoden und getXXX-Methoden für Collections enthält, wobei
schreibbare Collection-Instanzen zurückgegeben werden.

Wenn das `group-contract`-Plugin ebenfalls aktiviert ist, werden diese Konstrukte auch in den Interfaces erzeugt.


### Aktivierung
#### -Xmodifier

#### Optionen

##### -modifierClassName=`<string>` (Modifier)
Name der generierten Mutator-Klasse


##### -modifierMethodName=`<string>` (modifier)
Name der generierten methode zum Abruf einer Instanz der Mutator-Klasse



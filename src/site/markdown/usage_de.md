###  Benutzung

####  Allgemein

jaxb-rich-contract-plugin ist ein Plugin für den XJC "XML to Java compiler" der JAXB API.
Um das Plugin zu aktivieren, sind folgende Schritte erforderlich:

* JAR des Plugins zum Klassenpfad des XJC-Compilers hinzufügen.
* Falls die Standard-JAXB-Version der Compiler-Umgebung kleiner als 2.3 ist, müssen die JAXB-Bibliotheken in Version 2.3 ebenfalls zum Klassenpfad hinzugefügt werden.
* Kommandozeilenoption zur Aktivierung des gewüschten Plugins zur XJC-Kommandozeile hinzufügen.
* Die meisten Plugins haben außerdem noch eigene Kommandozeilenoptionen. Diese müssen direkt nach der Aktivierungsoption ("-X...") angegeben werden, um Namenskonflikte zwischen Optionen verschiedener Plugins zu vermeiden.
* Die Plugins  "immutable" und "constrained-properties" schließen sich gegenseitig aus und können nicht beide in einer einzigen Kommandozeile aktiviert werden.
  Ein Objekt kann nicht gleichzeitig unveränderlich sein und Änderungsnachrichten schicken.

####  Benutzung mit Apache Maven

Das "maven-jaxb2-plugin" der `<build>` -Konfiguration hinzufügen. In dessen Konfigurationsabschnitt müssen dann die einzelnen Plugins aktiviert werden. Ebenso wird hier der Klassenpfad so gesetzt, dass bei der Ausführung von XJC das Plugin-JAR `jaxb2-rich-contract-plugin` im Klassenpfad ist.

Dieses "cheat sheet" gibt alle verfügbaren Plugin-Optionen an und zeigt, wie die Abhängigkeit zum Plugin-JAR in den XJC-Klassenpfad konfiguriert wird:

``` xml
   <build>
        <plugins>
            <plugin>
                <groupId>org.jvnet.jaxb</groupId>
                <artifactId>jaxb-maven-plugin</artifactId>
                <version>4.0.0</version>
                <executions>
                    <execution>
                        <id>xsd-generate</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <schemaIncludes>
                        <schemaInclude>**/*.xsd</schemaInclude>
                    </schemaIncludes>
                    <strict>true</strict>
                    <verbose>true</verbose>
                    <extension>true</extension>
                    <removeOldOutput>true</removeOldOutput>
                   <args>
                        <arg>-Xconstrained-properties</arg>
                            <arg>-constrained-properties.constrained=y</arg>
                            <arg>-constrained-properties.bound=y</arg>
                            <arg>-constrained-properties.setterThrows=n</arg>
                            <arg>-constrained-properties.generateTools=y</arg>
                        <arg>-Xclone</arg>
                            <arg>-clone.cloneThrows=y</arg>
                        <arg>-Xcopy</arg>
                            <arg>-copy.partial=y</arg>
                            <arg>-copy.generateTools=y</arg>
                            <arg>-copy.constructor=y</arg>
                            <arg>-copy.narrow=n</arg>
                            <arg>-copy.selectorClassName=Selector</arg>
                            <arg>-copy.rootSelectorClassName=Select</arg>
                        <arg>-Xgroup-contract</arg>
                            <arg>-group-contract.declareSetters=y</arg>
                            <arg>-group-contract.declareBuilderInterface=y</arg>
                            <arg>-group-contract.supportInterfaceNameSuffix=Lifecycle</arg>
                            <arg>-group-contract.upstreamEpisodeFile=META-INF/jaxb-interfaces.episode</arg>
                            <arg>-group-contract.downstreamEpisodeFile=META-INF/jaxb-interfaces.episode</arg>
                        <arg>-Ximmutable</arg>
                            <arg>-immutable.fake=n</arg>
                            <arg>-immutable.overrideCollectionClass=null</arg>
                            <arg>-immutable.constructorAccess=public</arg>
                        <arg>-Xmodifier</arg>
                            <arg>-modifier.modifierClassName=Modifier</arg>
                            <arg>-modifier.modifierMethodName=modifier</arg>
                        <arg>-Xfluent-builder</arg>
                            <arg>-fluent-builder.rootSelectorClassName=Select</arg>
                            <arg>-fluent-builder.newBuilderMethodName=builder</arg>
                            <arg>-fluent-builder.newCopyBuilderMethodName=newCopyBuilder</arg>
                            <arg>-fluent-builder.copyToMethodName=copyTo</arg>
                            <arg>-fluent-builder.builderFieldSuffix=_Builder</arg>
                            <arg>-fluent-builder.generateTools=y</arg>
                            <arg>-fluent-builder.narrow=n</arg>
                            <arg>-fluent-builder.copyPartial=y</arg>
                            <arg>-fluent-builder.selectorClassName=Selector</arg>
                            <arg>-fluent-builder.builderClassName=Builder</arg>
                            <arg>-fluent-builder.builderInterfaceName=BuildSupport</arg>
                            <arg>-fluent-builder.copyAlways=n</arg>
                            <arg>-fluent-builder.buildMethodName=build</arg>
                            <arg>-fluent-builder.endMethodName=end</arg>
                            <arg>-fluent-builder.generateJavadocFromAnnotations=n</arg>
                        <arg>-Xmeta</arg>
                            <arg>-meta.generateTools=y</arg>
                            <arg>-meta.extended=n</arg>
                            <arg>-meta.camelCase=n</arg>
                            <arg>-meta.metaClassName=PropInfo</arg>
                            <arg>-meta.allowSet=y</arg>
                            <arg>-meta.visitMethodName=visit</arg>
                   </args>
                    <plugins>
                        <plugin>
                            <groupId>net.codesup.util</groupId>
                            <artifactId>jaxb-rich-contract-plugin</artifactId>
                            <version>4.1.1</version>
                        </plugin>
                    </plugins>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
Hinweis: Das Flag `<extension/>` muss auf "true" gesetzt sein, damit XJC überhaupt plugins akzeptiert.

Hinweis: Ab Version 4.0.0 hat sich die Behandlung der Kommandozeilenargumente vollständig geändert, da das bisherige Verhalten für einige Leute Probleme verursachte.
Dem Namen jedes Kommadozeilenarguments muss nun der Name des Plugin, durch einen Punkt getrennt, vorangestellt werden (siehe Beispiel oben). Dafür ist die Reihenfolge der Argumente
nun unerheblich, sie können irgendwo auf der Kommandozeile auftauchen, nicht nur nach der "-X.."-Option, die das Plugin aktiviert.

Hinweis: jaxb2-rich-contract-plugin implementiert JAXB und XJC APIs in der Version 4.0. Falls Sie mit einem älteren JDK oder einer älteren JAXB-Version arbeiten, verwenden Sie bitte eine ältere Version des Plugins, bis 2.1.0.



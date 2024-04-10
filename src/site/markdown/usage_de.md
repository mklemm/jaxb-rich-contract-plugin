###  Benutzung

####  Allgemein

jaxb2-rich-contract-plugin ist ein Plugin für den XJC "XML to Java compiler" der JAXB API.
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
                            <arg>-constrained=y</arg>
                            <arg>-bound=y</arg>
                            <arg>-setterThrows=n</arg>
                            <arg>-generateTools=y</arg>
                        <arg>-Xclone</arg>
                            <arg>-cloneThrows=y</arg>
                        <arg>-Xcopy</arg>
                            <arg>-partial=y</arg>
                            <arg>-generateTools=y</arg>
                            <arg>-constructor=y</arg>
                            <arg>-narrow=n</arg>
                            <arg>-selectorClassName=Selector</arg>
                            <arg>-rootSelectorClassName=Select</arg>
                        <arg>-Xgroup-contract</arg>
                            <arg>-declareSetters=y</arg>
                            <arg>-declareBuilderInterface=y</arg>
                            <arg>-supportInterfaceNameSuffix=Lifecycle</arg>
                            <arg>-upstreamEpisodeFile=META-INF/jaxb-interfaces.episode</arg>
                            <arg>-downstreamEpisodeFile=/META-INF/jaxb-interfaces.episode</arg>
                        <arg>-Ximmutable</arg>
                            <arg>-fake=n</arg>
                            <arg>-overrideCollectionClass=null</arg>
                            <arg>-constructorAccess=public</arg>
                        <arg>-Xmodifier</arg>
                            <arg>-modifierClassName=Modifier</arg>
                            <arg>-modifierMethodName=modifier</arg>
                        <arg>-Xfluent-builder</arg>
                            <arg>-rootSelectorClassName=Select</arg>
                            <arg>-newBuilderMethodName=builder</arg>
                            <arg>-newCopyBuilderMethodName=newCopyBuilder</arg>
                            <arg>-copyToMethodName=copyTo</arg>
                            <arg>-builderFieldSuffix=_Builder</arg>
                            <arg>-generateTools=y</arg>
                            <arg>-narrow=n</arg>
                            <arg>-copyPartial=y</arg>
                            <arg>-selectorClassName=Selector</arg>
                            <arg>-builderClassName=Builder</arg>
                            <arg>-builderInterfaceName=BuildSupport</arg>
                            <arg>-copyAlways=n</arg>
                            <arg>-buildMethodName=build</arg>
                            <arg>-endMethodName=end</arg>
                            <arg>-generateJavadocFromAnnotations=n</arg>
                        <arg>-Xmeta</arg>
                            <arg>-generateTools=y</arg>
                            <arg>-extended=n</arg>
                            <arg>-camelCase=n</arg>
                            <arg>-metaClassName=PropInfo</arg>
                            <arg>-allowSet=y</arg>
                            <arg>-visitMethodName=visit</arg>
                    </args>
                    <plugins>
                        <plugin>
                            <groupId>net.codesup.util</groupId>
                            <artifactId>jaxb2-rich-contract-plugin</artifactId>
                            <version>4.0.0</version>
                        </plugin>
                    </plugins>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
Hinweis: Das Flag `<extension/>` muss auf "true" gesetzt sein, damit XJC überhaupt plugins akzeptiert.

Hinweis: jaxb2-rich-contract-plugin implementiert JAXB und XJC APIs in der Version 4.0. Falls Sie mit einem älteren JDK oder einer älteren JAXB-Version arbeiten, verwenden Sie bitte eine ältere Version des Plugins, bis 2.1.0.



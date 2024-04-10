###  Usage

####  General

jaxb2-rich-contract-plugin is a plugin to the XJC "XML to Java compiler" shipped with the reference implementation of JAXB. It is targeted on version 4.0 of the JAXB API. In order to make it work, you need to:

* Add the jar file to the classpath of XJC
* Add the corresponding activating command-line option to XJC's invocation, see below for details of each of the plugins
* Each of the plugins, except "-Ximmutable", has one or more sub-options to fine-control its behavior. These sub-option must be given after the corresponding main "-X..." activation option, to avoid naming conflicts. Names of sub-options can be given dash-separated or in camelCase.
* The "immutable" and "constrained-properties" plugins are mutually exclusive. An object cannot be both immutable and send change notifications.

####  From Maven

The plugin has been tested with the [highsource jaxb-maven plugin](https://github.com/highsource/jaxb-tools), version 4.0.0 or later. Other JAXB maven plugins may or may not work.
You should add the highsource jaxb-maven-plugin to your `<build>` configuration. Of course you must add the JAXB API and implementation dependencies as well.
The current version 4.0.0 of the plugin supports JAXB 4.0 or later. If you need compatibility with earlier JAXB versions, please use an older version of this plugin.
Then add "jaxb2-rich-contract-plugin" as an XJC plugin ("plugin for plugin") to the maven plugin declaration. The following cheat sheet shows all possible options reflecting their default values:

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
Note: the `<extension/>` flag must be set to "true" in order to make XJC accept any extensions at all.

Note: jaxb2-rich-contract-plugin implements JAXB and XJC APIs version 4.0. You most likely will have to add the dependencies to these libraries to your classpath effective at XJC runtime. See the `<dependencies>` element above on how to do this.


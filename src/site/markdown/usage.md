###  Usage

####  General

jaxb-rich-contract-plugin is a plugin to the XJC "XML to Java compiler" shipped with the reference implementation of JAXB. It is targeted on version 4.0 of the JAXB API. In order to make it work, you need to:

* Add the jar file to the classpath of XJC
* Add the corresponding activating command-line option to XJC's invocation, see below for details of each of the plugins
* Each of the plugins, except "-Ximmutable", has one or more sub-options to fine-control its behavior. These sub-option must be given after the corresponding main "-X..." activation option, to avoid naming conflicts. Names of sub-options can be given dash-separated or in camelCase.
* The "immutable" and "constrained-properties" plugins are mutually exclusive. An object cannot be both immutable and send change notifications.

####  From Maven

The plugin has been tested with the [highsource jaxb-maven plugin](https://github.com/highsource/jaxb-tools), version 4.0.0 or later. Other JAXB maven plugins may or may not work.
You should add the highsource jaxb-maven-plugin to your `<build>` configuration. Of course you must add the JAXB API and implementation dependencies as well.
The current version of the plugin supports JAXB 4.0 or later. If you need compatibility with earlier JAXB versions, please use an older version of this plugin.
Then add "jaxb-rich-contract-plugin" as an XJC plugin ("plugin for plugin") to the maven plugin declaration. The following cheat sheet shows all possible options reflecting their default values:

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
                            <version>4.1.3</version>
                        </plugin>
                    </plugins>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
Note: the `<extension/>` flag must be set to "true" in order to make XJC accept any extensions at all.

Note: Version 4.0.0 brings an entirely different handling of plugin-specific command-line arguments, since the previous behavior has caused trouble for a lot of people trying to use different plugins.
Now. every argument has to be prefixed with the plugin's name (see example above) and the order of appearance of arguments is completely arbitrary, i.e. it isn't necessary to put the "-X..."-Option
that activates the plugin before the argument in the argument list anymore.
 
Note: jaxb-rich-contract-plugin implements JAXB and XJC APIs version 4.0. You most likely will have to add the dependencies to these libraries to your classpath effective at XJC runtime. See the `<dependencies>` element above on how to do this.


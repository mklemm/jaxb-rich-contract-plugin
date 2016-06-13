## group-contract
### Usage
#### -Xgroup-contract

#### Options

##### -declareSetters=`{y|n}` (y)
Also generate property setter methods in interface declarations.


##### -declareBuilderInterface=`{y|n}` (y)
If the "fluent builder plugin" (-Xfluent-builder) is also active, generate interface for the internal builder classes as well.


##### -supportInterfaceNameSuffix=`<string>` (Lifecycle)
If this is set, methods that could cause type conflicts when two generated interfaces are used together as type parameter bounds, will be put in another interface named the same as the original interface, but with the suffix specified here.


##### -upstreamEpisodeFile=`<string>` (META-INF/jaxb-interfaces.episode)
Use the given resource file to obtain information about interfaces defined in an upstream module (refer to "-episode" option of XJC).


##### -downstreamEpisodeFile=`<string>` (/META-INF/jaxb-interfaces.episode)
Generate "episode" file for downstream modules in the given resource location.


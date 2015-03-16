In general, you may wish to implement application logic in a way so that objects are initialized once
and then are immutable.
For traditional programming languages, like Java, for example, this is not always feasible in practice,
because legacy code and libraries have to be used.

With the `modifier` plugin, you can make the public interface of your classes immutable via the `immutable`
plugin, but at the same time provide a handle to modify the state of your objects anyway vi a reference that
needs to be queried explicitly.

This plugin is intended for use while refactoring existing code to a more "functional" and thread-friendly
code base. Eventually, your code should work so this plugin can be deactivated in your XJC configuration.
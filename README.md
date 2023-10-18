# semgrep-scalafix

Implement scalafix rules that mimic some semgrep checks.

Check out the microsite for more information: https://banno.github.io/semgrep-scalafix

To use the latest version, include the following in your `build.sbt`:

```scala
ThisBuild / scalafixDependencies += 
  "com.banno" %% "semgrep-scalafix" % "0.0.1"
```

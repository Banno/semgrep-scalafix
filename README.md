# semgrep-scalafix

Implement scalafix rules that mimic some semgrep checks.

Check out the microsite for more information: https://banno.github.io/semgrep-scalafix

To use the latest version, include the following in your `build.sbt`:

```scala
ThisBuild / scalafixDependencies += 
  "com.banno" %% "semgrep-scalafix" % "0.1.0"
```

## Actions pins

The GitHub actions managed by sbt-typelevel must be declared in
[github-pins.json](./project/src/main/resources/github-pins.json).

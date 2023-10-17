## semgrep-scalafix

The goal of semgrep-scalafix is to provide scalafix rules for checks that exist as [semgrep rules](https://semgrep.dev/p/scala).

### Usage
These scalafix rules are cross-published for Scala versions 2.12 and 2.13.

To use the latest version, include the following in your `build.sbt`:

```scala
ThisBuild / scalafixDependencies += 
  "com.banno" %% "semgrep-scalafix" % "@VERSION@"
```

Once enabled, configure which rules scalafix will run by adding them to your `.scalafix.conf` file like so:
```
//.scalafix.conf
rules = [
  NoRsaWithoutPadding
]
```


### Available Rules

#### NoRsaWithoutPadding
Relevant semgrep rule: [scala.lang.security.audit.rsa-padding-set.rsa-padding-set](https://semgrep.dev/r?q=scala.lang.security.audit.rsa-padding-set.rsa-padding-set).

This scalafix rule will raise an error if a `javax.crypto.Cipher` is instantiated using RSA, any mode, with no padding. For example:

```scala
import javax.crypto.Cipher

val badCipher = Cipher.getInstance("RSA/None/NoPadding") // will raise scalafix error
val alsoBad   = Cipher.getInstance("RSA/ECB/NoPadding") // will raise scalafix error

val goodCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding") 
```

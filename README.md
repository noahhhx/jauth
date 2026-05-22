# JAuth

Inspired by [openconnect-saml](https://github.com/mschabhuettl/openconnect-saml), but simplified for
my need and written in Java.

## Requirements

- JDK 17+ (only needed to build, or to run the JAR; the GraalVM native build
  produces a single static binary with no JDK dependency)
- `[openconnect](https://gitlab.com/openconnect/openconnect)` on `PATH`
- `sudo`
- A browser

## Build

```shell
mvn package
```

Outputs:
- `target/jauth.jar`

GraalVM native-image:

```shell
mvn -Pnative -DskipTests package
```

Outputs `target/vpnauth` — single executable, no JVM needed.

## Usage

Java:
```shell
java -jar target/jauth.jar connect --host="vpnhost"
```

Binary:
```shell
./target/jauth connect --host="vpnhost"
```
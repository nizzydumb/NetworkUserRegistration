# Bouncy Castle offline integration

## Included version and artifacts

The repository includes Bouncy Castle Java `1.84` under `lib/bouncycastle-1.84`. Nothing needs to be installed or
downloaded on the offline computer.

| Artifact | Purpose |
|---|---|
| `bcprov-jdk18on-1.84.jar` | JCA/JCE provider and lightweight cryptographic APIs |
| `bcutil-jdk18on-1.84.jar` | Utility classes required by higher-level BC modules |
| `bcpkix-jdk18on-1.84.jar` | X.509, PKIX, PKCS, CMS, OCSP, TSP, CMP, CRMF, and certificate APIs |

Matching source JARs, Javadoc JARs, Maven POMs, and SHA-256 files are included for offline source navigation,
documentation, dependency provenance, and integrity verification. The POM metadata records the Bouncy Castle
license and upstream project details.

## IntelliJ IDEA

`NetworkUserRegistration.iml` declares the three runtime JARs and attaches their matching source and Javadoc
archives using project-relative paths. IntelliJ imports them automatically; verify the `Bouncy Castle 1.84` entry
under **File | Project Structure | Modules | Dependencies**.

## Registering the provider

The JAR being on the classpath makes its Java APIs available, but algorithms requested through standard JCA names
must also have the provider registered. Register it once before requesting a BC-specific service:

```java
import security.BouncyCastleSupport;

BouncyCastleSupport.ensureRegistered();
```

Then use standard JCA/JCE APIs with provider name `BC`:

```java
import java.security.MessageDigest;

BouncyCastleSupport.ensureRegistered();
MessageDigest digest = MessageDigest.getInstance("SHA-256", BouncyCastleSupport.PROVIDER_NAME);
byte[] hash = digest.digest(input);
```

Higher-level classes can be imported directly, for example:

```java
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
```

Do not register both the regular `bcprov` provider and the separate Bouncy Castle FIPS provider in the same design
without reviewing their compatibility and compliance requirements. The artifacts included here are the regular,
non-FIPS Bouncy Castle distribution; including them does not make an application FIPS validated.

## Offline integrity verification

Each downloaded artifact has a neighboring `.sha256` file obtained from Maven Central. To verify one manually:

```powershell
$expected = (Get-Content lib\bouncycastle-1.84\bcprov-jdk18on-1.84.jar.sha256 -Raw).Trim()
$actual = (Get-FileHash lib\bouncycastle-1.84\bcprov-jdk18on-1.84.jar -Algorithm SHA256).Hash
$actual.Equals($expected, [StringComparison]::OrdinalIgnoreCase)
```

The result must be `True`.

After building, verify provider registration and a JCA algorithm without starting the GUI:

```powershell
$bc = Get-ChildItem lib\bouncycastle-1.84\*.jar |
    Where-Object Name -NotLike "*-sources.jar" |
    Where-Object Name -NotLike "*-javadoc.jar" |
    ForEach-Object FullName
$cp = (@("out\production\NetworkUserRegistration") + $bc) -join [IO.Path]::PathSeparator
& lib\jdk-26.0.2\bin\java.exe -cp $cp security.BouncyCastleVerification
```

## Updating later

All three artifacts must use the same Bouncy Castle version. When updating, replace binaries, sources, Javadocs,
POMs, and checksums together; then update `build.ps1`, `run.ps1`, `setup-dependencies.ps1`, the IntelliJ module file,
and this document. Never mix `bcprov`, `bcutil`, and `bcpkix` versions.

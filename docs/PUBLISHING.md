# Publishing to Maven Central

The client is published to Maven Central via the **Central Portal** (central.sonatype.com).
This follows the current Portal flow — the legacy OSSRH / Nexus staging process was retired.

The publishing plumbing lives in the `release` profile in `pom.xml`. A normal
`mvn package` / `mvn install` is unaffected: no signing, no sources/javadoc jars, no upload.

## One-time setup (per person / machine)

1. **Central Portal account** — sign in at https://central.sonatype.com (GitHub login is fine).
2. **Namespace `de.hallerweb`** — register it in the Portal, then verify domain ownership
   by adding the Portal-provided **DNS TXT record on the exact host `hallerweb.de`**, then
   click *Verify Namespace*. Verification is usually done within minutes and is permanent
   (covers every `de.hallerweb.*` artifact, so all future client libs are covered too).
3. **Portal publishing token** — Portal → *View Account* → *Generate User Token*. This yields a
   token *username* and *password* (not your login password).
4. **GPG key** — generate a key pair and publish the public key to a keyserver:
   ```bash
   gpg --gen-key                      # name + email + passphrase
   gpg --list-secret-keys --keyid-format=long
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

## `~/.m2/settings.xml`

Add the Portal token as server `central`, and (optionally) the GPG passphrase so the
build is non-interactive:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>TOKEN_USERNAME</username>
      <password>TOKEN_PASSWORD</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
</settings>
```

## Cutting a release

1. Drop the `-SNAPSHOT` from `<version>` in `pom.xml` (release versions only for Central).
2. Deploy with the release profile:
   ```bash
   mvn clean deploy -Prelease
   ```
   This builds the main, sources and javadoc jars, GPG-signs everything, and uploads the
   bundle to the Portal. With `autoPublish=false` the deployment stops at *validated* — review
   it at https://central.sonatype.com/publishing/deployments and click **Publish** manually.
   Once the first release is confirmed clean, flip `autoPublish` to `true` in `pom.xml`.
3. Bump `<version>` back to the next `-SNAPSHOT`.

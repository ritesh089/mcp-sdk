# Maven Central Publishing Setup Guide

This guide will help you set up and publish the MCP SDK to Maven Central via OSSRH (Open Source Software Repository Hosting).

## Prerequisites

1. **Sonatype OSSRH Account**: You need an account at [OSSRH](https://issues.sonatype.org/)
2. **GPG Key**: A GPG key for signing your artifacts
3. **GitHub Account**: For the `io.github.ritesh089` namespace

## Step 1: Sonatype OSSRH Account Setup

1. Go to [OSSRH](https://issues.sonatype.org/) and create an account
2. Create a new issue requesting access to publish under `io.github.ritesh089`
3. Provide proof of ownership of the GitHub repository
4. Wait for approval (usually 1-2 business days)

## Step 2: GPG Key Setup

### Generate a GPG Key

```bash
# Generate a new GPG key
gpg --gen-key

# Choose:
# - RSA and RSA (default)
# - 4096 bits
# - 0 (does not expire)
# - Your name and email
# - A secure passphrase
```

### Export Your Public Key

```bash
# List your keys to get the key ID
gpg --list-keys

# Export your public key (replace YOUR_EMAIL with your email)
gpg --armor --export your-email@example.com > public-key.asc

# Upload to a key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### Export Your Private Key

```bash
# Export your private key (replace YOUR_EMAIL with your email)
gpg --armor --export-secret-keys your-email@example.com > private-key.asc
```

## Step 3: Configure gradle.properties

Create or update your `gradle.properties` file with the following:

```properties
# Maven Central / Sonatype OSSRH
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

# Signing (required for Maven Central)
signing.keyId=YOUR_GPG_KEY_ID
signing.key=YOUR_GPG_PRIVATE_KEY_CONTENT
signing.password=YOUR_GPG_PASSPHRASE

# GitHub Packages (optional)
gpr.user=ritesh089
gpr.key=your-github-personal-access-token
```

### Getting the Values

- **ossrhUsername/ossrhPassword**: Your Sonatype OSSRH credentials
- **signing.keyId**: The key ID from `gpg --list-keys`
- **signing.key**: The content of your `private-key.asc` file (the entire content, including headers)
- **signing.password**: The passphrase you used when creating the GPG key

## Step 4: Update build.gradle for Signing

Once you have your GPG keys configured, update your `build.gradle` to enable signing:

```gradle
plugins {
    id 'java'
    id 'maven-publish'
    id 'signing'
}

// ... existing configuration ...

// Configure signing for Maven Central
signing {
    useInMemoryPgpKeys(
        project.findProperty("signing.keyId"),
        project.findProperty("signing.key"),
        project.findProperty("signing.password")
    )
    sign publishing.publications.maven
}
```

## Step 5: Publishing Process

### Test Local Publishing

First, test that everything works locally:

```bash
./gradlew publishLocal
```

### Publish to Maven Central

Use the provided script:

```bash
./publish-to-maven-central.sh
```

Or manually:

```bash
./gradlew publishToMavenCentral
```

## Step 6: Complete the Release

After publishing a release version:

1. Go to [OSSRH](https://oss.sonatype.org/)
2. Login with your credentials
3. Go to "Staging Repositories"
4. Find your repository (it will be in "Open" state)
5. Click "Close" to validate the repository
6. After closing, click "Release" to promote to Maven Central
7. Wait for sync (usually 10-30 minutes)

## Version Management

### Snapshot Versions

- Use `-SNAPSHOT` suffix for development versions
- Snapshots are published to OSSRH snapshots repository
- Cannot be promoted to Maven Central

### Release Versions

- Use semantic versioning (e.g., 1.0.0, 1.1.0)
- Release versions are published to OSSRH staging
- Must be manually promoted to Maven Central

## Troubleshooting

### Common Issues

1. **Signing Failed**: Check your GPG key configuration
2. **Authentication Failed**: Verify your OSSRH credentials
3. **Repository Not Found**: Ensure you have access to the `io.github.ritesh089` namespace

### Getting Help

- Check the [OSSRH documentation](https://central.sonatype.org/pages/ossrh-guide.html)
- Review the [Gradle signing plugin documentation](https://docs.gradle.org/current/userguide/signing_plugin.html)
- Check the [Maven Central requirements](https://central.sonatype.org/pages/requirements.html)

## Security Notes

- Never commit `gradle.properties` to version control
- Keep your GPG private key secure
- Use environment variables for sensitive data in CI/CD environments
- Regularly rotate your credentials

## Next Steps

After successful publication:

1. Update your project documentation with the new dependency coordinates
2. Announce the release on your project page
3. Consider setting up automated publishing for future releases
4. Monitor the Maven Central sync status

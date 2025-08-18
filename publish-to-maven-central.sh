#!/bin/bash

# MCP SDK Publishing Script for Maven Central
# This script helps you publish the MCP SDK to Maven Central via OSSRH

set -e

echo "🚀 MCP SDK Publishing Script for Maven Central"
echo "================================================"

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: Please run this script from the mcp-sdk directory"
    exit 1
fi

# Check if gradle.properties exists and has required values
if [ ! -f "gradle.properties" ]; then
    echo "❌ Error: gradle.properties file not found"
    echo "Please create gradle.properties with your credentials"
    exit 1
fi

# Check for required properties
source gradle.properties 2>/dev/null || true

if [ -z "$ossrhUsername" ] || [ "$ossrhUsername" = "your-sonatype-username" ]; then
    echo "❌ Error: ossrhUsername not set in gradle.properties"
    echo "Please set your Sonatype OSSRH username"
    exit 1
fi

if [ -z "$ossrhPassword" ] || [ "$ossrhPassword" = "your-sonatype-password" ]; then
    echo "❌ Error: ossrhPassword not set in gradle.properties"
    echo "Please set your Sonatype OSSRH password"
    exit 1
fi

if [ -z "$signing.keyId" ] || [ "$signing.keyId" = "your-gpg-key-id" ]; then
    echo "❌ Error: signing.keyId not set in gradle.properties"
    echo "Please set your GPG key ID"
    exit 1
fi

if [ -z "$signing.key" ] || [ "$signing.key" = "your-gpg-private-key" ]; then
    echo "❌ Error: signing.key not set in gradle.properties"
    echo "Please set your GPG private key"
    exit 1
fi

if [ -z "$signing.password" ] || [ "$signing.password" = "your-gpg-password" ]; then
    echo "❌ Error: signing.password not set in gradle.properties"
    echo "Please set your GPG key password"
    exit 1
fi

echo "✅ All required credentials are configured"

# Check current version
CURRENT_VERSION=$(grep "version = " build.gradle | sed 's/.*version = '\''\(.*\)'\''/\1/')
echo "📦 Current version: $CURRENT_VERSION"

# Check if this is a snapshot version
if [[ "$CURRENT_VERSION" == *"-SNAPSHOT"* ]]; then
    echo "⚠️  Warning: This is a SNAPSHOT version"
    echo "SNAPSHOT versions will be published to OSSRH snapshots repository"
    echo "Only release versions can be promoted to Maven Central"
else
    echo "✅ This is a release version"
fi

# Build the project first
echo "🔨 Building the project..."
./gradlew clean build

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please fix the issues and try again."
    exit 1
fi

echo "✅ Build successful"

# Ask for confirmation before publishing
echo ""
echo "🚨 IMPORTANT: Publishing to Maven Central is irreversible!"
echo "Once published, you cannot delete or modify the artifacts."
echo ""
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Publishing cancelled"
    exit 0
fi

# Publish to Maven Central
echo "📤 Publishing to Maven Central..."
./gradlew publishToMavenCentral

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Publishing successful!"
    echo ""
    if [[ "$CURRENT_VERSION" == *"-SNAPSHOT"* ]]; then
        echo "📝 This was a SNAPSHOT version published to OSSRH snapshots."
        echo "   To release to Maven Central, you need to:"
        echo "   1. Change version to a release version (e.g., 1.0.0)"
        echo "   2. Run this script again"
        echo "   3. Promote the staging repository in OSSRH"
    else
        echo "📝 This was a release version published to OSSRH staging."
        echo "   To complete the release to Maven Central:"
        echo "   1. Go to https://oss.sonatype.org/"
        echo "   2. Login with your OSSRH credentials"
        echo "   3. Go to 'Staging Repositories'"
        echo "   4. Find your repository and click 'Close'"
        echo "   5. After closing, click 'Release'"
        echo "   6. Wait for sync to Maven Central (usually 10-30 minutes)"
    fi
else
    echo "❌ Publishing failed. Please check the error messages above."
    exit 1
fi

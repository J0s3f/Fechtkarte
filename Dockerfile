FROM cimg/android:2024.04

USER root
WORKDIR /app

# Warm the Gradle dependency cache from just the build-config files, so this layer
# survives source-only changes and does not re-download the world every time.
#
# Uses the project's own wrapper (gradlew), not the base image's bundled `gradle`
# command — AGP 8.13.0 requires Gradle 8.13, and cimg/android:2024.04 only bundles
# 8.7. Pinning via the wrapper decouples the build's Gradle version from whatever
# the base image happens to ship, which is the standard Android-project setup
# anyway. See docs/NEXT_STEPS.md T2.4.
COPY --chown=circleci:circleci gradlew ./
COPY --chown=circleci:circleci gradle/wrapper/ gradle/wrapper/
COPY --chown=circleci:circleci build.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=circleci:circleci gradle/libs.versions.toml gradle/libs.versions.toml
COPY --chown=circleci:circleci app/build.gradle.kts app/proguard-rules.pro app/
COPY --chown=circleci:circleci app/src/main/AndroidManifest.xml app/src/main/AndroidManifest.xml
RUN chmod +x gradlew && ./gradlew --no-daemon help > /dev/null

# Full source, full verification build. This is the commit gate.
COPY --chown=circleci:circleci . .
RUN mkdir -p /app/.gradle && chmod 777 /app/.gradle

# .dockerignore excludes .git from the build context (real VCS history has no
# place in an image, and would leak commit metadata into it). Konsist's project
# root detection needs *a* .git directory to exist, though — without one,
# ArchitectureTest fails with KoInternalException, reproducibly, even though the
# same test passes fine under scripts/test.sh's bind mount (which keeps the real
# .git). A bare `git init` with zero commits is sufficient — confirmed directly
# against the Konsist jar, not assumed. See docs/NEXT_STEPS.md T1.11.
RUN git init -q .

RUN ./gradlew --no-daemon clean build

RUN echo "Build completed successfully"

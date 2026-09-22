buildscript {
    // Pins AGP 9.4.0's own vulnerable transitive build-tool dependencies (flagged by
    // Dependabot against settings.gradle.kts, since it can't open a PR against a version it
    // doesn't declare directly) forward to patched releases. bcprov/bcpkix specifically need
    // the latest release (1.86), not just the advisory's minimum fixed version (1.84/1.85):
    // AGP's own signing validation references a BouncyCastle post-quantum OID constant that
    // 1.85 doesn't yet carry, which breaks validateSigningDebug -- confirmed by running the
    // full build gate against each candidate version.
    dependencies {
        constraints {
            add("classpath", "org.bouncycastle:bcprov-jdk18on:1.86")
            add("classpath", "org.bouncycastle:bcpkix-jdk18on:1.86")
            add("classpath", "org.bitbucket.b_c:jose4j:0.9.6")
            add("classpath", "org.jdom:jdom2:2.0.6.1")
            add("classpath", "org.apache.commons:commons-lang3:3.18.0")
            add("classpath", "org.apache.httpcomponents:httpclient:4.5.13")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.license.report) apply false
    alias(libs.plugins.roborazzi) apply false
}

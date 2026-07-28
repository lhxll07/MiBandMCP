# Ktor checks these desktop JVM APIs only while probing for an attached IntelliJ debugger.
# Android does not provide them, and normal server execution does not enter that optional path.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

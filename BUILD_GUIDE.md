# Build Guide

## Quick Reference

| Task | Command |
|------|---------|
| Build project | `./gradlew build` |
| Run application | `./gradlew bootRun` |
| Run tests | `./gradlew test` |
| Clean build | `./gradlew clean build` |
| Build without tests | `./gradlew build -x test` |
| Create JAR | `./gradlew bootJar` |

## Prerequisites

- Java 21 or higher installed
- No Gradle installation required (wrapper included)

**Set Java 21** (if not default):
```bash
# macOS/Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Windows
set JAVA_HOME=C:\Path\To\Java21
```

## Building the Project

### Standard Build

```bash
./gradlew build
```

This will:
1. Download dependencies (first time only)
2. Compile Java sources
3. Process resources
4. Run tests
5. Package the application

### Build Without Tests

For faster builds during development:

```bash
./gradlew build -x test
```

### Clean Build

To ensure a fresh build from scratch:

```bash
./gradlew clean build
```

## Running the Application

### Using Gradle

```bash
./gradlew bootRun
```

The application will start on http://localhost:8080

### Using the JAR

```bash
# First, build the JAR
./gradlew bootJar

# Then run it
java -jar build/libs/components-lifecycle-service-0.0.1-SNAPSHOT.jar
```

### Custom Port

```bash
./gradlew bootRun --args='--server.port=9090'
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test

```bash
./gradlew test --tests "CLSApplicationTests"
```

### Test with Coverage

```bash
./gradlew test jacocoTestReport
```

Coverage report will be in: `build/reports/jacoco/test/html/index.html`

## Development Workflow

### 1. Make code changes

Edit your Java/resource files in `src/main/` or `src/test/`

### 2. Quick verification

```bash
./gradlew compileJava
```

### 3. Run tests

```bash
./gradlew test
```

### 4. Run application

```bash
./gradlew bootRun
```

### 5. Build final artifact

```bash
./gradlew build
```

## Common Tasks

### View All Available Tasks

```bash
./gradlew tasks
```

### View Dependencies

```bash
./gradlew dependencies
```

### Refresh Dependencies

If you added/changed dependencies in `build.gradle`:

```bash
./gradlew build --refresh-dependencies
```

### Generate IDE Files

For Eclipse:
```bash
./gradlew eclipse
```

For IntelliJ (usually auto-detected):
- Just open the project folder

## Build Outputs

After building, generated files are in:

```
build/
├── classes/               # Compiled .class files
├── libs/                  # Generated JAR files
│   └── components-lifecycle-service-0.0.1-SNAPSHOT.jar
├── reports/              # Test and other reports
│   ├── tests/
│   └── jacoco/
├── resources/            # Processed resources
└── tmp/                  # Temporary build files
```

## Continuous Integration

For CI/CD pipelines:

```bash
# Standard build
./gradlew clean build

# Ensure tests pass
./gradlew test --no-daemon
```

## Performance Tips

### Parallel Builds

```bash
./gradlew build --parallel
```

### Gradle Daemon

The Gradle daemon speeds up builds. It starts automatically.

To stop all daemons:
```bash
./gradlew --stop
```

### Build Cache

Gradle caches build outputs. To clear:

```bash
./gradlew clean
rm -rf .gradle/
```

## Troubleshooting

### Java Version Error

```
Error: requires at least JVM runtime version 17
```

**Solution**: Set JAVA_HOME to Java 21:

```bash
# macOS/Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew build

# Windows
set JAVA_HOME=C:\Path\To\Java21
gradlew.bat build
```

### Build Failure After Dependency Update

```bash
./gradlew clean build --refresh-dependencies
```

### Out of Memory

Edit `gradle.properties` (create if not exists):

```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### Port 8080 Already in Use

```bash
./gradlew bootRun --args='--server.port=9090'
```

## Environment-Specific Builds

### Development

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Production Build

```bash
./gradlew clean build -Pprofile=prod
```

## Additional Resources

- Gradle docs: https://docs.gradle.org
- Spring Boot Gradle plugin: https://docs.spring.io/spring-boot/gradle-plugin
- View this project's dependencies: `./gradlew dependencies`
- View all tasks: `./gradlew tasks --all`


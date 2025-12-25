# octopus-components-lifecycle-service

Component lifecycle management service built with Spring Boot and Gradle.

## Prerequisites

- **Java 21** or higher
- No need to install Gradle - the project includes the Gradle wrapper

## Quick Start

### Build the project
```bash
./gradlew build
```

### Run the application
```bash
./gradlew bootRun
```

The application will start on http://localhost:8080

## Build Commands

```bash
# Build the project
./gradlew build

# Build without running tests
./gradlew build -x test

# Run tests only
./gradlew test

# Clean and rebuild
./gradlew clean build

# Run the application
./gradlew bootRun

# Create executable JAR
./gradlew bootJar
```

**Note**: If you see Java version errors, ensure Java 21 is active:

```bash
# macOS/Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Windows
set JAVA_HOME=C:\Path\To\Java21
```

### Build Outputs

After building, you'll find:
- **Compiled classes**: `build/classes/`
- **JAR file**: `build/libs/components-lifecycle-service-0.0.1-SNAPSHOT.jar`
- **Test results**: `build/reports/tests/`

### Running the JAR

```bash
# Build the JAR
./gradlew bootJar

# Run it
java -jar build/libs/components-lifecycle-service-0.0.1-SNAPSHOT.jar
```

## Useful Commands

```bash
# View all available tasks
./gradlew tasks

# View build tasks
./gradlew tasks --group=build

# List all dependencies
./gradlew dependencies

# Check dependency tree
./gradlew dependencies --configuration compileClasspath

# Clean build artifacts
./gradlew clean

# Run in debug mode
./gradlew bootRun --debug-jvm
```

## Configuration

The application can be configured via:
- Environment variables
- Command-line arguments: `./gradlew bootRun --args='--server.port=9090'`

### Key Configuration Arguments

- **Server Port**: `server.port=<server-port>`
- **PostgreSQL url**: `spring.datasource.url=jdbc:postgresql://<host>:<port>/<db-name>`
- **PostgreSQL username**: `spring.datasource.username=<username>`
- **PostgreSQL password**: `spring.datasource.password=<password>`
- **External API - Release Engineering Url**: `app.releaseEngineering.baseUrl=<baseUrl>`
- **External API - Components RegistryService Url**: `app.componentsRegistryService.baseUrl=<baseUrl>`

### Keycloak
As a temporary solution keycloak can be configurated only in `application.properties` file


## Development

### Project Structure

```
src/
├── main/
│   ├── org/octopusden/octopus/lifecycle/
│   │   ├── api/              # REST API controllers
│   │   ├── authorization/    # Security configuration
│   │   ├── db/              # Database entities and repositories
│   │   └── ui/              # UI controllers
│   └── resources/
│       ├── application.properties
│       ├── static/          # CSS, JS, images
│       └── templates/       # Thymeleaf templates
└── test/
    └── java/                # Test classes
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests "CLSApplicationTests"

# Run tests continuously (re-run on changes)
./gradlew test --continuous
```

### IDE Setup

#### IntelliJ IDEA
1. Open the project folder
2. IntelliJ will auto-detect the Gradle project and import it
3. Wait for dependency download to complete
4. Run the application from the `CLSApplication` class

#### VS Code
1. Install "Extension Pack for Java" and "Gradle for Java" extensions
2. Open the project folder
3. VS Code will detect the Gradle project automatically
4. Use the Gradle tasks view to run builds

#### Eclipse
1. Install Buildship Gradle plugin (usually pre-installed)
2. File → Import → Gradle → Existing Gradle Project
3. Select the project folder

## Docker Support

Docker configuration is available in the `docker/` directory for running dependencies:

```bash
cd docker
docker-compose up -d
```

## Troubleshooting

### Java Version Issues

If you see errors about Java version:

```bash
# Check your Java version
java -version

# Set JAVA_HOME to Java 21 (macOS/Linux)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Then run gradle
./gradlew build
```

### Dependency Resolution Issues

```bash
# Refresh dependencies
./gradlew build --refresh-dependencies

# Clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew clean build
```

### Build Cache Issues

```bash
# Clear local build cache
./gradlew clean

# Clear Gradle daemon cache
rm -rf .gradle/
./gradlew clean build
```

### Port Already in Use

If port 8080 is already in use:

```bash
# Run on a different port
./gradlew bootRun --args='--server.port=9090'
```

## License

See LICENSE file for details.


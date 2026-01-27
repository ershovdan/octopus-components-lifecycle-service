# Components Lifecycle Service

A Spring Boot application for managing component lifecycle stages and rules.

## Quick Start

See [project-doc/DEPLOYMENT-GUIDE.md](project-doc/DEPLOYMENT-GUIDE.md) for build and deployment instructions.

## Documentation

- [Deployment Guide](project-doc/DEPLOYMENT-GUIDE.md) - Build and deploy instructions
- [Keycloak Integration](project-doc/KEYCLOAK-INTEGRATION.md) - Authentication setup
- [Infrastructure Compatibility](project-doc/INFRASTRUCTURE-COMPATIBILITY.md) - Integration details
- [Project Guidelines](project-doc/PROJECT-GUIDELINES.md) - **Important:** Coding standards and security practices
- [Contributing Guide](project-doc/CONTRIBUTING.md) - How to contribute to this project
- [Setup Guide](project-doc/SETUP.md) - Quick setup for contributors

## Development

### Prerequisites
- Java 17+
- Gradle 7.x+
- PostgreSQL (for local development)

### Build
```bash
./gradlew clean build
```

### Run Locally
```bash
./gradlew bootRun
```

### API Documentation

Interactive API documentation is available via Swagger UI:

**Production/Test:**
```
https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui.html
```

**Local:**
```
http://localhost:8080/swagger-ui.html
```

See [project-doc/SWAGGER-API-DOCS.md](project-doc/SWAGGER-API-DOCS.md) for complete API documentation details.

## Contributing

Before committing:
1. Read [project-doc/PROJECT-GUIDELINES.md](project-doc/PROJECT-GUIDELINES.md)
2. Never use real domain names in documentation (use `acme.com` instead)
3. Don't commit sensitive information
4. Optionally install the pre-commit hook:
   ```bash
   cp scripts/pre-commit.sample .git/hooks/pre-commit
   chmod +x .git/hooks/pre-commit
   ```

See [project-doc/CONTRIBUTING.md](project-doc/CONTRIBUTING.md) for detailed guidelines.

## Security

- Authentication via Keycloak/OAuth2
- JWT token validation (optional)
- Role-based access control (future)

## License

See [LICENSE](LICENSE) file.


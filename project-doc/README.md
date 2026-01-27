# Documentation Organization

This directory contains all project documentation except the main README.

## Files

### Setup & Guidelines
- **[SETUP.md](SETUP.md)** - Quick setup guide for contributors
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Detailed contribution guidelines
- **[PROJECT-GUIDELINES.md](PROJECT-GUIDELINES.md)** - Coding standards and security practices

### API Documentation
- **[SWAGGER-API-DOCS.md](SWAGGER-API-DOCS.md)** - Swagger/OpenAPI documentation and URLs
- **[SWAGGER-QUICK-REF.md](SWAGGER-QUICK-REF.md)** - Quick reference for Swagger URLs

### Deployment & Integration
- **[DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)** - Build and deployment instructions
- **[KEYCLOAK-INTEGRATION.md](KEYCLOAK-INTEGRATION.md)** - Authentication setup with Keycloak
- **[INFRASTRUCTURE-COMPATIBILITY.md](INFRASTRUCTURE-COMPATIBILITY.md)** - Infrastructure integration details

### Implementation Details
- **[AUTHENTICATION-FIXED-FINAL.md](AUTHENTICATION-FIXED-FINAL.md)** - Authentication implementation details
- **[LOGOUT-AND-AUTH-FIXED.md](LOGOUT-AND-AUTH-FIXED.md)** - Logout implementation details
- **[REDIRECT-LOOP-FIX.md](REDIRECT-LOOP-FIX.md)** - Fix for OAuth2 redirect loop (ERR_TOO_MANY_REDIRECTS)
- **[REDIRECT-LOOP-FIX-SUMMARY.md](REDIRECT-LOOP-FIX-SUMMARY.md)** - Quick summary of redirect loop fix

## Quick Links

- Back to [Main README](../README.md)
- [Setup for New Contributors](SETUP.md)
- [How to Contribute](CONTRIBUTING.md)

## Documentation Standards

When adding new documentation:
1. Place all .md files in this directory (except README.md which stays in root)
2. Use `acme.com` for example domain names (never use real company domains)
3. Update this index file with the new document
4. Link to it from the main README.md if it's a primary document

See [PROJECT-GUIDELINES.md](PROJECT-GUIDELINES.md) for detailed standards.

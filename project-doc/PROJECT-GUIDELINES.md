# Project Guidelines

## Documentation Standards

### Domain Names in Examples

**IMPORTANT:** Never use real company domain names in documentation or code examples.

**✅ DO:**
- Use `acme.com` for example domain names
- Use `example.com` for generic examples
- Use `localhost` or `127.0.0.1` for local development

**❌ DON'T:**
- Use actual company domain names like `acme.com`
- Use real internal hostnames
- Expose real infrastructure details in public repositories

### Examples:

```yaml
# GOOD - Using acme.com
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa

# BAD - Using real domain
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa
```

```bash
# GOOD - Using acme.com
curl https://f1-gateway-test.acme.com/api/health

# BAD - Using real domain
curl https://f1-gateway-test.acme.com/api/health
```

## Security

- Never commit real credentials, tokens, or API keys
- Use placeholders like `your-client-id`, `your-secret`, etc.
- Keep real configuration in external Config Server, not in repository

## Code Quality

- Follow Java naming conventions
- Write clear, self-documenting code
- Add comments for complex business logic
- Keep methods focused and concise

## Commit Messages

- Use clear, descriptive commit messages
- Reference issue/ticket numbers when applicable
- Use conventional commit format when possible

## Before Committing

- [ ] Check for hardcoded real domain names
- [ ] Remove any debug code
- [ ] Verify no sensitive information is included
- [ ] Run linter/formatter
- [ ] Ensure tests pass (if applicable)

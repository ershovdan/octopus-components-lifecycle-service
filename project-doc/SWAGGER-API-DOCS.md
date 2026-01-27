# Swagger/OpenAPI Documentation URLs

## Swagger UI URL

### Through API Gateway (Production/Test Environment)

```
https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui/index.html
```

Or the shorter version (auto-redirects):
```
https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui.html
```

### Local Development

```
http://localhost:8080/swagger-ui/index.html
```

Or:
```
http://localhost:8080/swagger-ui.html
```

## OpenAPI JSON/YAML

### OpenAPI Specification (JSON)

```
https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs
```

Local:
```
http://localhost:8080/v3/api-docs
```

### OpenAPI Specification (YAML)

```
https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs.yaml
```

Local:
```
http://localhost:8080/v3/api-docs.yaml
```

## Configuration

### Dependencies

SpringDoc OpenAPI is configured in `build.gradle`:

```gradle
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"
```

Version: `2.7.0` (from `gradle.properties`)

### Security Configuration

Swagger endpoints are now configured to be accessible without authentication in `SecurityConfig.java`:

```java
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/swagger-resources/**",
    "/webjars/**"
).permitAll()
```

## API Controllers with Swagger Annotations

Your API controllers use Swagger annotations:

### API Endpoints

- **AuthController** - `/auth/me` - Get current user info
- **ApiController** - `/api/**` - Main API endpoints (rules, components, builds)
- **UiController** - UI pages with @Operation annotations

## Customization (Optional)

If you want to customize Swagger UI, add to `application.properties`:

```properties
# Custom Swagger configuration (optional)
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.path=/v3/api-docs

# Show actuator endpoints in Swagger
springdoc.show-actuator=true

# API info
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha

# Enable try-it-out by default
springdoc.swagger-ui.tryItOutEnabled=true
```

## Common Issues

### Issue: 404 Not Found

**Cause:** Changes not deployed yet

**Fix:** 
1. Rebuild: `./gradlew clean build`
2. Redeploy the application
3. Wait for pod to restart

### Issue: 401 Unauthorized

**Cause:** Security configuration not allowing Swagger endpoints

**Fix:** Already fixed! SecurityConfig now permits Swagger URLs

### Issue: Empty API Documentation

**Cause:** No controllers with proper annotations

**Fix:** Your controllers already have `@RestController` and `@Operation` annotations, so this should work fine

## Testing After Deployment

### Step 1: Check OpenAPI JSON

```bash
curl https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs
```

Should return JSON with your API specification.

### Step 2: Access Swagger UI

Open in browser:
```
https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui.html
```

Should show interactive API documentation.

### Step 3: Test an Endpoint

1. Navigate to an endpoint (e.g., `/auth/me`)
2. Click "Try it out"
3. Click "Execute"
4. Should see the response

## Security Note

⚠️ **Swagger UI is now publicly accessible (no authentication required)**

This is common for development/testing environments, but for production you might want to:

**Option 1: Require Authentication**
```java
// In SecurityConfig - remove Swagger from permitAll
// Keep only in development profile
```

**Option 2: Restrict by IP**
```java
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
    .access("hasIpAddress('10.0.0.0/8')")
```

**Option 3: Use Spring Profiles**
```java
@Profile("dev")
@Configuration
public class SwaggerConfig {
    // Only enable in dev profile
}
```

## Summary

✅ **Swagger UI URL:** `https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui.html`

✅ **OpenAPI Spec:** `https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs`

✅ **Security:** Configured to allow public access (no authentication required)

✅ **Version:** SpringDoc 2.7.0

After deploying the updated `SecurityConfig.java`, Swagger will be accessible!

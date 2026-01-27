# API Gateway Configuration Fix for Authentication

## Problem

The API Gateway has OAuth2 configured, but it's not enforcing authentication for the `/components-lifecycle-service/**` route.

This means:
- Unauthenticated requests reach your service
- Your service correctly returns 401
- But the gateway doesn't redirect to Keycloak login

## Solution

Add the following to the **API Gateway's configuration** (NOT your service):

### Option 1: Spring Cloud Gateway (Java Configuration)

If the gateway uses Java configuration, create or update a `SecurityConfig` class:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (if any)
                .pathMatchers("/actuator/health/**").permitAll()
                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2Login(withDefaults())  // Enable OAuth2 login
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(withDefaults())  // Enable JWT validation
            );
        
        return http.build();
    }
}
```

### Option 2: Spring Cloud Gateway (YAML Configuration)

If the gateway uses YAML-based security, add this to `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - TokenRelay  # Already configured ✓
      routes:
        - id: components-lifecycle-service
          uri: http://components-lifecycle-service-test.f1.svc.cluster.local:8080
          predicates:
            - Path=/components-lifecycle-service/**
          filters:
            - StripPrefix=1
            - TokenRelay  # Forward OAuth2 token to backend
          # This route will inherit authentication from global security config
  
  security:
    oauth2:
      client:
        provider:
          keycloak:
            # Already configured ✓
            token-uri: ${auth-server.url}/realms/${auth-server.realm}/protocol/openid-connect/token
            authorization-uri: ${auth-server.url}/realms/${auth-server.realm}/protocol/openid-connect/auth
            userinfo-uri: ${auth-server.url}/realms/${auth-server.realm}/protocol/openid-connect/userinfo
            user-name-attribute: preferred_username
        registration:
          keycloak:
            # Already configured ✓
            client-id: f1-api-gateway
            provider: keycloak
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-name: Authentication Server
```

### Key Point: Security Configuration Must Require Authentication

The gateway must have security configuration that:

1. **Requires authentication** for all (or specific) routes
2. **Redirects to Keycloak** when unauthenticated
3. **Forwards tokens** to backend services

If the gateway currently has:
```java
.pathMatchers("/**").permitAll()  // ❌ BAD - allows everything
```

Change to:
```java
.anyExchange().authenticated()  // ✅ GOOD - requires authentication
```

## Verification

After updating the gateway configuration and redeploying:

```bash
# Test unauthenticated access
curl -I https://f1-gateway-test.acme.com/components-lifecycle-service/

# Expected: HTTP 302 redirect to Keycloak login page
# Location: https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/auth?...
```

## For Your Service

Your service configuration is **already correct**:

✅ OAuth2 Resource Server configured  
✅ Returns 401 for unauthenticated requests  
✅ Validates JWT tokens from gateway  
✅ Static resources and health checks are public  

**No changes needed** in your service's `SecurityConfig.java`.

## Summary

The fix needs to be in the **API Gateway**, not your service:

1. Ensure gateway has `@EnableWebSecurity` with `.anyExchange().authenticated()`
2. Ensure gateway has `.oauth2Login()` enabled
3. Ensure route for your service has `TokenRelay` filter

Once the gateway is configured to enforce authentication, it will:
- Intercept unauthenticated requests
- Redirect users to Keycloak login
- Forward JWT tokens to your service
- Your service validates the tokens

## Next Steps

1. Find the API Gateway's `SecurityConfig` class (or create one if it doesn't exist)
2. Add the security configuration shown above
3. Redeploy the API Gateway
4. Test authentication flow

The gateway repository is likely separate from your service. You'll need to update the gateway's codebase, not `octopus-components-lifecycle-service`.

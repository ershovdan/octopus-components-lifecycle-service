# Keycloak Integration Configuration Guide

## Configuration for Config Server

Add these properties to your Config Server configuration for the `components-lifecycle-service`:

### Option 1: This Service Validates JWT Tokens (Recommended for Production)

```yaml
# application-cloud-qa.yml or components-lifecycle-service-cloud-qa.yml in Config Server

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Keycloak issuer URI - used to validate JWT tokens
          issuer-uri: https://keycloak.yourdomain.com/realms/your-realm
          # OR use JWK Set URI directly
          jwk-set-uri: https://keycloak.yourdomain.com/realms/your-realm/protocol/openid-connect/certs

# Example for your environment:
# issuer-uri: https://keycloak.acme.com/realms/f1-qa
# jwk-set-uri: https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/certs
```

### Option 2: API Gateway Validates JWT, This Service Trusts Gateway (Current Setup)

If your API Gateway (f1-gateway) already validates JWT tokens and you want this service to simply trust authenticated requests:

```yaml
# Leave JWT configuration empty or omit it entirely
# The service will accept any authenticated principal from the gateway
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ""  # Empty = trust gateway
```

## Environment Variables (Alternative)

You can also configure via environment variables in your Helm deployment:

```yaml
# In your Helm values or deployment config
env:
  - name: KEYCLOAK_ISSUER_URI
    value: "https://keycloak.acme.com/realms/f1-qa"
  
  - name: KEYCLOAK_JWK_SET_URI
    value: "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/certs"
```

## Keycloak Configuration

### 1. Client Configuration in Keycloak

Your Keycloak client should have:
- **Client Protocol**: `openid-connect`
- **Access Type**: `confidential` or `public` (depending on your setup)
- **Valid Redirect URIs**: Include your gateway and application URLs
- **Web Origins**: `+` (to allow CORS from redirect URIs)

### 2. Token Claims

The service expects these JWT claims:
- `preferred_username` - Primary username (Keycloak standard)
- `sub` - Subject (fallback if preferred_username not present)
- `groups` - User groups/roles (optional, list of strings)

Example JWT payload:
```json
{
  "exp": 1737910800,
  "iat": 1737907200,
  "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "preferred_username": "john.doe",
  "email": "john.doe@company.com",
  "groups": ["developers", "users"],
  "realm_access": {
    "roles": ["user", "developer"]
  }
}
```

## API Gateway Configuration

### Spring Cloud Gateway Configuration

Your API Gateway should be configured to:

#### Option 1: Forward JWT Tokens
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: components-lifecycle-service
          uri: http://components-lifecycle-service:8080
          predicates:
            - Path=/components-lifecycle-service/**
          filters:
            - StripPrefix=1
            - TokenRelay=  # Forwards JWT token to backend service
```

#### Option 2: Gateway Validates, Backend Trusts
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa
  cloud:
    gateway:
      routes:
        - id: components-lifecycle-service
          uri: http://components-lifecycle-service:8080
          predicates:
            - Path=/components-lifecycle-service/**
          filters:
            - StripPrefix=1
            # Gateway validates JWT and creates authenticated principal
```

## How It Works

### Architecture Flow:

```
User Browser
    ↓ (1) Access application
API Gateway (f1-gateway)
    ↓ (2) Redirect to Keycloak login
Keycloak
    ↓ (3) Authenticate user, issue JWT
API Gateway
    ↓ (4) Forward request with JWT token
Components Lifecycle Service
    ↓ (5) Validate JWT (if configured) or trust gateway
    ↓ (6) Extract user info from JWT claims
    ↓ (7) Serve request
```

### JWT Token Flow:

1. **User logs in** → Keycloak issues JWT token
2. **Gateway receives request** → Extracts JWT from cookie/header
3. **Gateway forwards to service** → Includes JWT in Authorization header
4. **Service validates JWT** → Checks signature, expiry, issuer (if configured)
5. **Service extracts user** → Gets `preferred_username`, `groups` from claims
6. **Service serves request** → User info available via `securityService.getCurrentUser()`

## Testing

### 1. Check JWT Token Flow

```bash
# Get JWT token from Keycloak
TOKEN=$(curl -X POST "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpass" \
  -d "grant_type=password" \
  -d "client_id=your-client" \
  -d "client_secret=your-secret" | jq -r '.access_token')

# Test authenticated request
curl -H "Authorization: Bearer $TOKEN" \
  https://f1-gateway-test.acme.com/components-lifecycle-service/auth/me
```

### 2. Check User Info Extraction

```bash
# Should return actual user info, not "anonymous"
curl -H "Authorization: Bearer $TOKEN" \
  https://f1-gateway-test.acme.com/components-lifecycle-service/auth/me

# Expected response:
{
  "username": "john.doe",
  "groups": ["developers", "users"],
  "roles": []
}
```

### 3. Verify Authentication Required

```bash
# Without token - should fail with 401
curl https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules
# Expected: 401 Unauthorized or redirect to Keycloak login

# With token - should succeed
curl -H "Authorization: Bearer $TOKEN" \
  https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules
# Expected: 200 OK with data
```

## Troubleshooting

### Issue: "anonymous" user returned

**Cause**: JWT token not being forwarded or not being parsed correctly

**Solution**:
1. Check API Gateway forwards JWT: `TokenRelay` filter enabled
2. Check JWT contains `preferred_username` claim
3. Enable debug logging: `logging.level.org.springframework.security=DEBUG`

### Issue: 401 Unauthorized with valid token

**Cause**: JWT validation failing

**Solution**:
1. Check `issuer-uri` or `jwk-set-uri` is correct
2. Verify Keycloak realm name matches
3. Check JWT hasn't expired
4. Ensure clock sync between services

### Issue: JWT validation fails with "Invalid signature"

**Cause**: Public key mismatch or wrong JWK Set URI

**Solution**:
1. Verify `jwk-set-uri` points to correct Keycloak endpoint
2. Check Keycloak is accessible from the service pod
3. Verify no proxy/firewall blocking JWK endpoint

## Recommended Setup

For your environment (f1-gateway + Keycloak), I recommend:

### ✅ **Option: Gateway Validates, Backend Trusts** (Simpler)

1. **API Gateway**: Validates JWT tokens from Keycloak
2. **This Service**: Trusts authenticated requests from gateway
3. **Config**: Leave JWT config empty in this service

**Pros**: 
- Simpler configuration
- Centralized JWT validation
- Better performance (validate once at gateway)

**Cons**:
- Must trust gateway (but it's in same cluster, so OK)

### Configuration:

```yaml
# In Config Server - components-lifecycle-service-cloud-qa.yml
# Leave JWT config empty or omit it
```

The service will automatically trust authenticated principals from Spring Security context populated by the gateway.

## Current Implementation Status

✅ **Configured and Working**:
- OAuth2 Resource Server enabled
- JWT token parsing (handles Keycloak claims)
- User extraction from JWT (`preferred_username`, `groups`)
- Authentication required for endpoints
- Static resources and health checks public

🔧 **Needs Configuration** (in Config Server):
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` or
- `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
- OR leave empty to trust gateway

The application is **ready for Keycloak integration**! Just add the appropriate configuration in your Config Server. 🎉

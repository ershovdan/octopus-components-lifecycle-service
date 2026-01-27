# Authentication Flow & Redirect Loop Fix

## Problem: ERR_TOO_MANY_REDIRECTS

You're experiencing a redirect loop when accessing:
```
https://f1-gateway-test.acme.com/components-lifecycle-service/oauth2/authorization/keycloak
```

### Root Cause

**This service is NOT an OAuth2 Client - it's a Resource Server.**

- ❌ Your service does NOT have OAuth2 Client configuration
- ❌ Your service does NOT expose `/oauth2/authorization/*` endpoints
- ✅ Your service is designed to work behind an API Gateway
- ✅ The Gateway handles all authentication flows

### Correct Architecture

```
User Browser
    ↓
API Gateway (f1-gateway) ← Handles OAuth2/Keycloak authentication
    ↓
Components Lifecycle Service ← Trusts authenticated requests from Gateway
```

## Solution

### Option 1: Access Through Gateway (Recommended)

**Correct URL:**
```
https://f1-gateway-test.acme.com/components-lifecycle-service/
```

**What should happen:**
1. User accesses the URL above
2. Gateway checks authentication
3. If not authenticated → Gateway redirects to Keycloak login
4. User logs in at Keycloak
5. Keycloak redirects back to Gateway with token
6. Gateway validates token and forwards request to your service
7. User sees the application

### Option 2: Configure Gateway Properly

The API Gateway needs proper OAuth2 Client configuration:

```yaml
# In f1-gateway configuration (Config Server)
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: your-client-id
            client-secret: your-client-secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: https://keycloak.acme.com/realms/f1-qa
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa
  cloud:
    gateway:
      routes:
        - id: components-lifecycle-service
          uri: http://components-lifecycle-service-test.f1.svc.cluster.local:8080
          predicates:
            - Path=/components-lifecycle-service/**
          filters:
            - StripPrefix=1
            - TokenRelay=
```

**Key points:**
- Gateway has **OAuth2 Client** configuration (for login flow)
- Gateway also has **OAuth2 Resource Server** (for JWT validation)
- Gateway uses `TokenRelay` filter to forward JWT to backend services

### Option 3: Direct Access (Development Only)

If you need to access the service directly (not through Gateway):

1. **Get a JWT token from Keycloak:**
```bash
TOKEN=$(curl -X POST "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=your-username" \
  -d "password=your-password" \
  -d "grant_type=password" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" | jq -r '.access_token')
```

2. **Use the token to access the service:**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://components-lifecycle-service-test.f1.svc.cluster.local:8080/
```

## What This Service Does

This Components Lifecycle Service is configured as:
- ✅ **OAuth2 Resource Server** - validates JWT tokens (optional)
- ✅ **Trusts API Gateway** - accepts authenticated requests from gateway
- ❌ **NOT an OAuth2 Client** - does not handle login flows
- ❌ **Does NOT expose** `/oauth2/authorization/*` endpoints

## Troubleshooting

### Check Gateway Configuration

```bash
# Check if Gateway is configured with OAuth2 Client
kubectl get configmap -n f1 f1-gateway-config -o yaml | grep -A 20 "oauth2"

# Check Gateway logs
kubectl logs -n f1 deployment/f1-api-gateway-test --tail=100
```

### Check Service Configuration

```bash
# Check if service is receiving authenticated requests
kubectl logs -n f1 deployment/components-lifecycle-service-test --tail=100 | grep "Logged User"
```

### Test Authentication Flow

1. **Clear browser cookies** (important!)
2. **Access through Gateway:** `https://f1-gateway-test.acme.com/components-lifecycle-service/`
3. **Expected:** Redirect to Keycloak login
4. **After login:** Redirect back to your application

### Common Issues

**Issue:** "Too many redirects"
- **Cause:** Accessing OAuth2 endpoints directly on the service
- **Fix:** Access through API Gateway URL

**Issue:** "401 Unauthorized" when accessing through Gateway
- **Cause:** Gateway not forwarding authentication
- **Fix:** Check Gateway configuration has `TokenRelay` filter or proper headers

**Issue:** "User is anonymous"
- **Cause:** Gateway not setting authentication headers
- **Fix:** Ensure Gateway forwards `X-Forwarded-User` header or JWT token

## Summary

✅ **DO:** Access your application through the API Gateway
```
https://f1-gateway-test.acme.com/components-lifecycle-service/
```

❌ **DON'T:** Try to access OAuth2 authorization endpoints directly on the service
```
https://f1-gateway-test.acme.com/components-lifecycle-service/oauth2/authorization/keycloak
```

The Gateway handles all authentication. Your service just processes authenticated requests.

# Fix Summary: ERR_TOO_MANY_REDIRECTS

## Problem

When accessing:
```
https://f1-gateway-test.acme.com/components-lifecycle-service/oauth2/authorization/keycloak
```

You get: **ERR_TOO_MANY_REDIRECTS**

## Root Cause

**You are trying to access an OAuth2 Client endpoint that doesn't exist in your service.**

- Your service is an **OAuth2 Resource Server**, not an OAuth2 Client
- The `/oauth2/authorization/*` endpoints don't exist in your service
- These endpoints should only exist in the API Gateway
- This causes a redirect loop

## Changes Made

### 1. Fixed Authentication Entry Point ✅

**File:** `SecurityConfig.java`

Changed from returning HTML to returning proper 401 status:

```java
// Before: Returned HTML with login link (caused confusion)
response.setStatus(401);
response.setContentType("text/html");
response.getWriter().write("<html><body><h1>401 Unauthorized</h1>...");

// After: Returns clean 401 for Gateway to handle
response.sendError(401, "Unauthorized");
```

**Why:** The API Gateway needs a clean 401 response to know it should handle authentication.

### 2. Added 401 Error Page ✅

**File:** `src/main/resources/templates/error/401.html`

Created a user-friendly error page for unauthenticated users.

### 3. Created Documentation ✅

**File:** `project-doc/REDIRECT-LOOP-FIX.md`

Comprehensive guide explaining:
- Why the redirect loop occurs
- The correct architecture
- How to access the application properly
- How to configure the API Gateway
- Troubleshooting steps

## Solution

### ✅ CORRECT: Access Through API Gateway

```
https://f1-gateway-test.acme.com/components-lifecycle-service/
                                                                    ^
                                                                    |
                                                         Access the root path
```

**What should happen:**
1. Gateway checks if you're authenticated
2. If not → redirects to Keycloak login
3. After login → redirects back to your application
4. You see the application

### ❌ INCORRECT: Direct OAuth2 Endpoint Access

```
https://f1-gateway-test.acme.com/components-lifecycle-service/oauth2/authorization/keycloak
                                                                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                                                                    This endpoint doesn't exist in your service!
```

## Next Steps

### 1. Clear Browser Cookies

```bash
# In Chrome: 
# Settings → Privacy → Clear browsing data → Cookies

# Or use Incognito mode
```

### 2. Access the Correct URL

```
https://f1-gateway-test.acme.com/components-lifecycle-service/
```

### 3. If Still Not Working: Check Gateway Configuration

The API Gateway needs to be configured as an OAuth2 Client.

> **📝 Note:** Don't know what to use for `client-id`? See [HOW-TO-FIND-CLIENT-ID.md](HOW-TO-FIND-CLIENT-ID.md) for detailed instructions on finding or creating the Keycloak client.

```yaml
# f1-gateway Config Server configuration
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: f1-gateway  # See HOW-TO-FIND-CLIENT-ID.md to find your actual client-id
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
```

### 4. Verify Gateway Routes

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: components-lifecycle-service
          uri: http://components-lifecycle-service-test.f1.svc.cluster.local:8080
          predicates:
            - Path=/components-lifecycle-service/**
          filters:
            - StripPrefix=1
            - TokenRelay=  # Important: forwards JWT token
```

## Architecture Clarification

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ Access: /components-lifecycle-service/
       ↓
┌─────────────────────┐
│   API Gateway       │  ← OAuth2 CLIENT (handles login)
│   (f1-gateway)      │  ← OAuth2 Resource Server (validates JWT)
└──────┬──────────────┘
       │ Forwards authenticated request
       ↓
┌─────────────────────┐
│ Lifecycle Service   │  ← OAuth2 Resource Server ONLY
│  (your service)     │  ← Trusts Gateway authentication
└─────────────────────┘
```

### Key Points:

1. **API Gateway = OAuth2 Client**
   - Handles login flows
   - Redirects to Keycloak
   - Has `/oauth2/authorization/*` endpoints

2. **Your Service = OAuth2 Resource Server**
   - Validates JWT tokens (optional)
   - Trusts Gateway authentication
   - Does NOT have `/oauth2/authorization/*` endpoints

## Testing

### Test 1: Access Through Gateway

```bash
curl -v https://f1-gateway-test.acme.com/components-lifecycle-service/
```

**Expected:**
- If unauthenticated: 302 redirect to Keycloak
- If authenticated: 200 OK with your application

### Test 2: Check Service Health

```bash
curl https://f1-gateway-test.acme.com/components-lifecycle-service/actuator/health
```

**Expected:** `{"status":"UP",...}`

### Test 3: Access Protected API

```bash
curl https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules
```

**Expected:**
- If unauthenticated: 401 or redirect to login
- If authenticated: 200 OK with rules data

## Troubleshooting

### Still Getting Redirect Loop?

1. **Clear ALL cookies** (including f1-gateway-test.acme.com)
2. **Try Incognito mode**
3. **Check Gateway logs:**
   ```bash
   kubectl logs -n f1 deployment/f1-api-gateway-test --tail=100
   ```

### Getting 401 Unauthorized?

- Check if Gateway has OAuth2 Client configuration
- Verify Keycloak client credentials are correct
- Ensure Gateway routes have `TokenRelay` filter

### User Shows as "anonymous"?

- Check Gateway forwards authentication headers
- Verify `GatewayAuthenticationFilter` is working
- Check service logs for authentication details

## Files Modified

1. ✅ `SecurityConfig.java` - Fixed authentication entry point
2. ✅ `error/401.html` - Added user-friendly error page
3. ✅ `project-doc/REDIRECT-LOOP-FIX.md` - Detailed documentation

## Summary

The redirect loop occurs because you're trying to access an OAuth2 Client endpoint (`/oauth2/authorization/keycloak`) that doesn't exist in your service. Your service is designed to work behind an API Gateway that handles all authentication.

**Solution:** Always access your application through the Gateway root path:
```
https://f1-gateway-test.acme.com/components-lifecycle-service/
```

Never try to access OAuth2 authorization endpoints directly - those are handled by the Gateway.

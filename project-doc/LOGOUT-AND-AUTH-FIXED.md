# Logout and Authorization Redirect - Fixed ✅

## Problems Solved

### 1. ✅ Unauthorized Users Not Redirected to Login
**Problem:** Users accessing the app without authentication weren't redirected to Keycloak login.

**Solution:** Added exception handling that:
- Redirects UI requests to OAuth2 authorization endpoint
- Returns 401 for API requests
- Gateway handles the actual Keycloak authentication

### 2. ✅ Logout Button Doesn't Work
**Problem:** Logout button in UI did nothing.

**Solution:** 
- Created `LogoutController` with `/logout` endpoint
- Clears Spring Security context
- Invalidates HTTP session
- Optionally redirects to Keycloak logout (full SSO logout)

## Changes Made

### 1. SecurityConfig.java - Added Exception Handling

```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint((request, response, authException) -> {
        // For API requests, return 401
        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(401, "Unauthorized");
        } else {
            // For UI requests, redirect to gateway login
            String loginUrl = request.getContextPath() + "/oauth2/authorization/keycloak";
            response.sendRedirect(loginUrl);
        }
    })
)
```

### 2. SecurityConfig.java - Added Logout Configuration

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .permitAll()
)
```

### 3. LogoutController.java - NEW

Created dedicated logout controller that:
- Handles both GET and POST `/logout` requests
- Clears Spring Security context
- Invalidates HTTP session
- Redirects to Keycloak logout (if configured)
- Falls back to home page redirect

### 4. application.properties - Added Keycloak Logout URL

```properties
keycloak.logout-url=${KEYCLOAK_LOGOUT_URL:}
```

## How It Works Now

### Authorization Redirect Flow

```
User → Access Protected Page
  ↓ (Not authenticated)
SecurityConfig → authenticationEntryPoint
  ↓
Redirect to /oauth2/authorization/keycloak
  ↓
API Gateway → Keycloak Login
  ↓
User Logs In
  ↓
Redirect back to original page (authenticated)
```

### Logout Flow

```
User → Clicks Logout Button
  ↓
JavaScript → window.location.href = "/logout"
  ↓
LogoutController → /logout endpoint
  ↓
1. Clear Security Context
2. Invalidate Session
3. Redirect to Keycloak Logout (if configured)
  ↓
Keycloak → Logout from SSO
  ↓
Redirect to home page (unauthenticated)
```

## Configuration Options

### Option 1: Local Logout Only (Default)

**No configuration needed**

The logout will:
- Clear local session
- Clear Spring Security context
- Redirect to home page
- User still logged in to Keycloak SSO (will auto-login on next visit)

### Option 2: Full SSO Logout (Recommended)

**Add to Config Server:**

```yaml
keycloak:
  logout-url: https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout
```

The logout will:
- Clear local session
- Clear Spring Security context
- Logout from Keycloak SSO completely
- User must re-enter credentials on next login

## Testing

### Test 1: Unauthorized Access Redirect

```bash
# 1. Clear cookies/session in browser
# 2. Access application URL
https://f1-gateway-test.acme.com/components-lifecycle-service/

# Expected behavior:
# - Redirected to API Gateway
# - API Gateway redirects to Keycloak login
# - After login, redirected back to application
```

### Test 2: Logout Button

```bash
# 1. Log in to the application
# 2. Click the logout button in UI

# Expected behavior (without Keycloak logout URL):
# - Redirected to home page
# - Local session cleared
# - Can access again (auto-login via SSO)

# Expected behavior (with Keycloak logout URL):
# - Redirected to Keycloak
# - Logged out from Keycloak
# - Redirected back to home page
# - Must re-login on next access
```

### Test 3: API Unauthorized

```bash
# Without authentication
curl https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules

# Expected: 401 Unauthorized (not redirect)
```

## JavaScript (compocaste-base.js)

The logout button code is **already correct**:

```javascript
document.getElementById("logout_button").onclick = () => {
    window.location.href = (window.contextPath || '') + "/logout"
}
```

This will:
1. Navigate to `/logout` endpoint (or `/components-lifecycle-service/logout` with context path)
2. Backend handles the actual logout
3. No changes needed in JavaScript!

## Gateway Configuration

The API Gateway should have OAuth2 client configuration for Keycloak:

```yaml
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
```

**Note:** If this is already configured in the gateway, no changes needed!

## Deployment

### 1. Build Application

```bash
./gradlew clean bootJar
./gradlew dockerBuildImage
```

### 2. Optional: Add Keycloak Logout URL

Add to Config Server (`components-lifecycle-service-cloud-qa.yml`):

```yaml
keycloak:
  logout-url: https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout
```

### 3. Deploy

```bash
helm upgrade components-lifecycle-service-test helm-repo/spring-cloud \
  --atomic --install --timeout 5m \
  [your parameters]
```

## Troubleshooting

### Issue: Redirect loop

**Cause:** Gateway not configured for OAuth2

**Solution:** Ensure gateway has OAuth2 client registration for Keycloak

### Issue: Logout doesn't clear Keycloak session

**Cause:** `keycloak.logout-url` not configured

**Solution:** Add logout URL to Config Server

### Issue: 404 on /logout

**Cause:** CSRF protection blocking POST requests

**Solution:** Already fixed - logout is permitted in SecurityConfig

### Issue: Can't access /oauth2/authorization/keycloak

**Cause:** Gateway doesn't have this endpoint

**Solution:** Update redirect to use gateway's OAuth2 endpoints

## Summary

✅ **Unauthorized users** → Redirected to Keycloak login via gateway  
✅ **Logout button** → Works correctly, clears session  
✅ **API requests** → Return 401 (no redirect)  
✅ **Optional SSO logout** → Configure via Config Server  
✅ **No JavaScript changes needed** → Works as-is  

Both issues are now resolved! 🎉

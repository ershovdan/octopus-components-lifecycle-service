# Logout Fix: Redirect to Gateway

## Problem

Based on HAR file analysis, logout was:
1. Requesting: `https://f1-gateway-test.acme.com/components-lifecycle-service/logout`
2. Getting 302 redirect back to: `https://f1-gateway-test.acme.com/components-lifecycle-service/`
3. **NOT actually logging out from Keycloak**

Should redirect to:
```
https://f1-gateway-test.acme.com/logout
```

## Root Cause

The service was handling logout internally instead of delegating to the Gateway. When behind an API Gateway, **logout must happen at the Gateway level** to:
- Clear Gateway session
- Redirect to Keycloak logout
- Properly end the SSO session

## Solution Implemented

### 1. Removed LogoutController ✅

**Deleted:** `src/main/java/org/octopusden/octopus/lifecycle/authorization/LogoutController.java`

**Why:** The service should NOT handle logout at all. The Gateway handles all authentication flows.

### 2. Updated JavaScript to Use Absolute URL ✅

**File:** `src/main/resources/static/js/compocaste-base.js`

**Changed:**
```javascript
// OLD (was using context path)
window.location.href = (window.contextPath || '') + "/logout"

// NEW (uses absolute URL to Gateway root)
window.location.href = window.location.origin + "/logout"
```

**Result:**
- Goes to: `https://f1-gateway-test.acme.com/logout`
- NOT to: `https://f1-gateway-test.acme.com/components-lifecycle-service/logout`

### 3. Removed Logout from SecurityConfig ✅

**File:** `SecurityConfig.java`

- Removed `.logout()` configuration
- Removed `/logout` from `permitAll()` list
- Service no longer has any logout endpoints

## Architecture

```
User clicks Logout
        ↓
JavaScript: window.location.href = window.location.origin + "/logout"
        ↓
Browser navigates to: https://gateway/logout
        ↓
┌─────────────────────────────────┐
│   API Gateway                   │
│   Handles /logout endpoint      │
│   - Clears session              │
│   - Redirects to Keycloak       │
└─────────────────────────────────┘
        ↓
┌─────────────────────────────────┐
│   Keycloak                      │
│   Logs out user                 │
│   Redirects back                │
└─────────────────────────────────┘
        ↓
User is logged out ✅
```

## Gateway Configuration Required

The API Gateway MUST have logout configuration:

```yaml
# f1-gateway configuration (Config Server)
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: f1-gateway  # Or your client-id
            client-secret: ${KEYCLOAK_SECRET}
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

### Gateway Logout Handler

The Gateway needs Spring Security logout configuration:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            # ... above config ...
            
# Optional: Custom logout redirect
# If not set, defaults to /login?logout
server:
  servlet:
    session:
      cookie:
        name: GATEWAY_SESSION
```

**Or configure via Java:**

```java
// In f1-gateway SecurityConfig
http
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/")
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID", "GATEWAY_SESSION")
        .addLogoutHandler((request, response, authentication) -> {
            // Clear Keycloak session
            String keycloakLogoutUrl = "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout";
            String redirectUri = request.getScheme() + "://" + request.getServerName() + "/";
            response.sendRedirect(keycloakLogoutUrl + "?redirect_uri=" + redirectUri);
        })
    );
```

## Deployment Steps

### 1. Rebuild the Application

```bash
./gradlew clean build
```

### 2. Build Docker Image

```bash
./gradlew dockerBuildImage
```

### 3. Deploy to Kubernetes

```bash
kubectl rollout restart deployment/components-lifecycle-service-test -n f1
```

### 4. Verify JavaScript File is Updated

After deployment, check the JavaScript file:

```bash
# Check the deployed JS file
curl https://f1-gateway-test.acme.com/components-lifecycle-service/js/compocaste-base.js | grep -A 3 "logout_button"

# Should show:
# window.location.href = window.location.origin + "/logout"
```

### 5. Clear Browser Cache

**Important:** Clear browser cache or hard refresh to get the new JavaScript file!

- Chrome: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows/Linux)
- Or: DevTools → Network tab → Disable cache

## Testing

### Test 1: Check JavaScript File

1. Open DevTools → Sources
2. Navigate to: `js/compocaste-base.js`
3. Find the logout button handler
4. **Should see:** `window.location.href = window.location.origin + "/logout"`
5. **Should NOT see:** `contextPath` or `/components-lifecycle-service/logout`

### Test 2: Network Tab Verification

1. Open DevTools → Network tab
2. Click Logout button
3. **First request should be:** `https://f1-gateway-test.acme.com/logout`
4. **Should NOT be:** `.../components-lifecycle-service/logout`

### Test 3: Full Logout Flow

1. Login to application
2. Click Logout
3. **Expected flow:**
   ```
   1. Browser → https://gateway/logout
   2. Gateway → Redirects to Keycloak logout
   3. Keycloak → Logs out user
   4. Keycloak → Redirects back
   5. Next access → Requires login again ✅
   ```

### Test 4: HAR File Verification

Capture a new HAR file after deployment:
1. Open DevTools → Network tab
2. Click Logout
3. Right-click → Save all as HAR
4. Check the first request URL
5. **Should be:** `.../logout` (without /components-lifecycle-service/)

## Troubleshooting

### Issue: Still Goes to /components-lifecycle-service/logout

**Cause:** Browser cached the old JavaScript file

**Fix:**
1. Hard refresh: Cmd+Shift+R or Ctrl+Shift+R
2. Clear browser cache completely
3. Try incognito/private browsing mode
4. Check DevTools → Network → Disable cache checkbox

### Issue: 404 on /logout

**Cause:** Gateway doesn't have logout endpoint configured

**Fix:** Add Spring Security logout configuration to f1-gateway

### Issue: Logout but Still Authenticated

**Cause:** Gateway session cleared but Keycloak session still active

**Fix:** Gateway must redirect to Keycloak logout URL with proper redirect_uri

### Issue: Redirect Loop After Logout

**Cause:** Keycloak redirect_uri pointing back to authenticated area

**Fix:** Set redirect_uri to public landing page (usually `/` or `/login`)

## Files Modified

1. ✅ **REMOVED:** `LogoutController.java` 
2. ✅ **Modified:** `compocaste-base.js` - Uses `window.location.origin + "/logout"`
3. ✅ **Modified:** `SecurityConfig.java` - Removed logout configuration

## What Changed in HAR

### Before (Old Behavior - From Your HAR):
```json
Request: {
  "url": "https://f1-gateway-test.acme.com/components-lifecycle-service/logout"
}
Response: {
  "status": 302,
  "location": "https://f1-gateway-test.acme.com/components-lifecycle-service/"
}
```

### After (New Behavior - Expected):
```json
Request: {
  "url": "https://f1-gateway-test.acme.com/logout"
}
Response: {
  "status": 302,
  "location": "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout?redirect_uri=..."
}
```

## Summary

✅ **LogoutController removed** - Service no longer handles logout
✅ **JavaScript updated** - Uses absolute URL to Gateway root
✅ **SecurityConfig updated** - No logout configuration
✅ **Gateway must handle logout** - Add logout configuration to f1-gateway

**Key Point:** After deployment and cache clear, logout will go directly to the Gateway's `/logout` endpoint, which then handles Keycloak logout.

**Next Steps:**
1. Deploy the updated code
2. Clear browser cache
3. Test logout flow
4. If Gateway returns 404, add logout configuration to Gateway

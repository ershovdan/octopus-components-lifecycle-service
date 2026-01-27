# Logout Fix - Analysis from HAR File

## HAR File Analysis Summary

### What The HAR File Revealed

**Request:**
```json
{
  "method": "GET",
  "url": "https://f1-gateway-test.acme.com/components-lifecycle-service/logout",
  "initiator": {
    "file": "compocaste-base.js",
    "line": 63
  }
}
```

**Response:**
```json
{
  "status": 302,
  "location": "https://f1-gateway-test.acme.com/components-lifecycle-service/"
}
```

### The Problems Identified

1. ❌ **Wrong URL:** Going to `/components-lifecycle-service/logout` instead of `/logout`
2. ❌ **Service handling logout:** The service was redirecting back to itself
3. ❌ **No Keycloak logout:** Not logging out from SSO session
4. ❌ **Old JavaScript:** Line 63 showing old code with `contextPath`

## Root Causes

### 1. Service Had LogoutController

The service was trying to handle logout itself instead of delegating to the Gateway.

### 2. JavaScript Used Context Path

The logout button was appending the service's context path:
```javascript
// OLD CODE (line 63 in HAR)
window.location.href = (window.contextPath || '') + "/logout"
```

### 3. No Gateway Logout Flow

The logout was staying within the service context instead of going to the Gateway root.

## Complete Fix Applied

### Change 1: Removed LogoutController

**Action:** Deleted entire file
```
DELETED: src/main/java/org/octopusden/octopus/lifecycle/authorization/LogoutController.java
```

**Reason:** Service should NOT handle logout. The Gateway handles all authentication flows.

### Change 2: Updated JavaScript

**File:** `src/main/resources/static/js/compocaste-base.js`

**Old Code (causing the issue):**
```javascript
document.getElementById("logout_button").onclick = () => {
    window.location.href = (window.contextPath || '') + "/logout"
}
```

**New Code:**
```javascript
document.getElementById("logout_button").onclick = () => {
    // Redirect to Gateway's logout endpoint using absolute URL
    // This ensures we hit https://gateway-host/logout, not /components-lifecycle-service/logout
    window.location.href = window.location.origin + "/logout"
}
```

**Result:** Will now navigate to `https://f1-gateway-test.acme.com/logout`

### Change 3: Updated SecurityConfig

**File:** `SecurityConfig.java`

**Removed:**
- `.logout()` configuration block
- `/logout` from `permitAll()` list

**Reason:** No logout endpoints in the service anymore.

## Expected Behavior After Fix

### New HAR File Should Show:

**Request:**
```json
{
  "method": "GET",
  "url": "https://f1-gateway-test.acme.com/logout",  // ← No /components-lifecycle-service/
  "initiator": {
    "file": "compocaste-base.js",
    "line": 64  // ← Updated line
  }
}
```

**Response (from Gateway):**
```json
{
  "status": 302,
  "location": "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout?redirect_uri=..."
}
```

## Comparison: Before vs After

| Aspect | Before (HAR) | After (Fixed) |
|--------|--------------|---------------|
| **Logout URL** | `/components-lifecycle-service/logout` | `/logout` |
| **Handled By** | Service (LogoutController) | Gateway |
| **Redirects To** | Service home page | Keycloak logout |
| **Logs Out of Keycloak** | ❌ No | ✅ Yes |
| **Clears Gateway Session** | ❌ No | ✅ Yes |
| **JavaScript Line** | 63 (old code) | 64 (new code) |

## Deployment Required

⚠️ **IMPORTANT:** The HAR file shows the OLD behavior because the changes haven't been deployed yet.

### To Apply the Fix:

1. **Rebuild:**
   ```bash
   ./gradlew clean build
   ```

2. **Redeploy:**
   ```bash
   kubectl rollout restart deployment/components-lifecycle-service-test -n f1
   ```

3. **Clear Browser Cache:**
   - Hard refresh: `Cmd+Shift+R` or `Ctrl+Shift+R`
   - Or use Incognito mode

4. **Test:**
   - Click logout
   - Capture new HAR file
   - Verify URL is now `/logout` not `/components-lifecycle-service/logout`

## Verification Steps

### Step 1: Check Deployed JavaScript

After deployment:
```bash
curl -s https://f1-gateway-test.acme.com/components-lifecycle-service/js/compocaste-base.js \
  | grep -A 3 "logout_button"
```

**Should output:**
```javascript
document.getElementById("logout_button").onclick = () => {
    window.location.href = window.location.origin + "/logout"
}
```

### Step 2: Test in Browser

1. Open DevTools → Network tab
2. Clear cache
3. Reload application
4. Click Logout
5. **First request should be:** `https://f1-gateway-test.acme.com/logout`

### Step 3: Capture New HAR

1. Open DevTools → Network tab
2. Click Logout
3. Right-click on requests → "Save all as HAR with content"
4. Check the new HAR file
5. First entry should show `/logout` not `/components-lifecycle-service/logout`

## Gateway Configuration Check

If after deployment you get **404 on /logout**, the Gateway needs configuration.

### Required Gateway Configuration:

```yaml
# f1-gateway config (Config Server or application.yml)
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: f1-gateway
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

## Summary

✅ **Problem identified:** HAR shows logout going to wrong URL
✅ **Root cause found:** Service handling logout + old JavaScript
✅ **Fix applied:** Removed LogoutController, updated JavaScript
✅ **Next step:** Deploy and test

After deployment with cleared cache, logout will work correctly by going through the Gateway's `/logout` endpoint, which then handles Keycloak SSO logout.

## Documentation

- **Full Details:** [LOGOUT-FIX.md](LOGOUT-FIX.md)
- **Deployment Guide:** [LOGOUT-FIX-DEPLOY.md](LOGOUT-FIX-DEPLOY.md)

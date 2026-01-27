# How to Find or Create Keycloak Client ID

## Quick Answer

The `client-id` should be the **Keycloak client name** configured for your API Gateway (f1-gateway).

**Common client ID names:**
- `f1-gateway`
- `f1-api-gateway`
- `api-gateway`
- `gateway-client`
- Or whatever name your team uses

---

## Option 1: Find Existing Client ID (Recommended)

### Check Existing Gateway Configuration

The f1-gateway should already have a Keycloak client configured. You need to find it.

#### Step 1: Check Gateway Config Server

```bash
# SSH to your config server or check the Git repository for:
# service-config/f1-api-gateway-test.yml
# or
# service-config/f1-gateway-cloud-qa.yml

# Look for this section:
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: <THIS_IS_WHAT_YOU_NEED>
            client-secret: <AND_THIS_SECRET>
```

#### Step 2: Check Kubernetes Secrets

```bash
# Check if the client ID is in Kubernetes secrets
kubectl get secret -n f1 application-cloud-qa -o yaml | grep client

# Or check the gateway specific secret
kubectl get secret -n f1 f1-api-gateway-cloud-qa -o yaml | grep client
```

#### Step 3: Check Gateway Deployment Logs

```bash
# Gateway might log the client ID on startup
kubectl logs -n f1 deployment/f1-api-gateway-test --tail=200 | grep -i "client"
```

#### Step 4: Ask Your Team

The DevOps team or whoever configured the API Gateway should know the client ID.

---

## Option 2: Check Keycloak Admin Console

### Step 1: Log into Keycloak

1. Go to: `https://keycloak.acme.com/admin/`
2. Select Realm: `f1-qa` (or your realm name)

### Step 2: Find Gateway Client

1. Navigate to: **Clients** (left sidebar)
2. Look for clients with names like:
   - `f1-gateway`
   - `f1-api-gateway`
   - `api-gateway`
   - `gateway-client`

### Step 3: Get Client Details

Click on the client name, then note:
- **Client ID** (on the Settings tab)
- **Client Secret** (on the Credentials tab)

---

## Option 3: Create New Keycloak Client (If None Exists)

If the Gateway doesn't have a Keycloak client yet, you need to create one.

### Step 1: Create Client in Keycloak

1. **Log into Keycloak Admin Console**
   - URL: `https://keycloak.acme.com/admin/`
   - Select Realm: `f1-qa`

2. **Create New Client**
   - Click **Clients** → **Create client**
   
3. **Client Settings:**
   ```yaml
   Client ID: f1-gateway
   Name: F1 API Gateway
   Client Protocol: openid-connect
   Client authentication: ON (for confidential client)
   ```

4. **Access Settings:**
   ```yaml
   Standard Flow Enabled: ON (for authorization_code grant)
   Direct Access Grants Enabled: ON (for password grant, if needed)
   Valid Redirect URIs:
     - https://f1-gateway-test.acme.com/login/oauth2/code/keycloak
     - https://f1-gateway-test.acme.com/login/oauth2/code/*
   Web Origins:
     - https://f1-gateway-test.acme.com
   ```

5. **Get Client Secret**
   - Go to **Credentials** tab
   - Copy the **Client Secret**

### Step 2: Configure Gateway

Add to your Gateway Config Server configuration:

```yaml
# f1-api-gateway-test.yml or f1-gateway-cloud-qa.yml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: f1-gateway
            client-secret: <SECRET_FROM_KEYCLOAK>
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

---

## Important Notes

### ⚠️ This Configuration is for the API Gateway, NOT Your Service

**Remember:**
- The `client-id` and `client-secret` go in the **f1-gateway** configuration
- Your `components-lifecycle-service` does NOT need OAuth2 Client configuration
- Your service is a Resource Server, not an OAuth2 Client

### Architecture Reminder

```
┌────────────────────────────┐
│  API Gateway (f1-gateway)  │
│                            │
│  Needs:                    │
│  - client-id: f1-gateway   │ ← Configure THIS
│  - client-secret: xxxxx    │
│  - OAuth2 Client config    │
└────────────────────────────┘
              ↓
┌────────────────────────────┐
│  Your Service              │
│  (lifecycle-service)       │
│                            │
│  Needs:                    │
│  - NOTHING! (or just JWT   │ ← NO client-id needed!
│    validation config)      │
└────────────────────────────┘
```

---

## Example Real Configuration

Here's what a typical setup looks like:

### Gateway Configuration (f1-api-gateway-test.yml)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: f1-gateway             # ← This is what you're looking for
            client-secret: ${KEYCLOAK_SECRET} # ← Stored in Kubernetes secret
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

### Your Service Configuration (components-lifecycle-service-cloud-qa.yml)

```yaml
# Option 1: Validate JWT (if you want double validation)
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa

# Option 2: Trust Gateway (simpler)
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ""  # Empty = trust gateway
```

---

## TL;DR - What You Should Do

1. **Check the f1-gateway configuration** in your Config Server
2. **Find the `client-id`** that's already configured there
3. **Use that same client-id** in the Gateway configuration

**You do NOT need to configure client-id in your lifecycle service!**

---

## Still Need Help?

### Check with DevOps Team

Ask them:
1. "What is the Keycloak client ID for f1-gateway?"
2. "Where is the Gateway OAuth2 configuration stored?"
3. "Can I see the f1-api-gateway-test.yml config file?"

### Check Running Gateway Configuration

```bash
# Get the Gateway pod
kubectl get pods -n f1 | grep gateway

# Check environment variables
kubectl exec -n f1 <gateway-pod-name> -- env | grep -i client

# Check the Spring properties
kubectl exec -n f1 <gateway-pod-name> -- cat /app/application.yml
```

---

## Summary

✅ **DO:**
- Find the existing client-id from f1-gateway configuration
- Use that client-id in the Gateway OAuth2 client config
- Ask your DevOps team if you can't find it

❌ **DON'T:**
- Add client-id to your lifecycle service (it doesn't need it)
- Create a new Keycloak client unless the Gateway doesn't have one
- Guess the client-id (it must match what's registered in Keycloak)

The client-id is typically something like: `f1-gateway`, `f1-api-gateway`, or `api-gateway`

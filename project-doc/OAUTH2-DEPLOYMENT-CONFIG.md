# Deployment Configuration for OAuth2 Login

## Environment Variables Required

Add these environment variables to your deployment configuration (Kubernetes, Helm, etc.):

### Production Environment

```yaml
env:
  - name: KEYCLOAK_ISSUER_URI
    value: "https://keycloak.acme.com/realms/f1-qa"
  
  - name: KEYCLOAK_CLIENT_ID
    value: "components-lifecycle-service"
  
  - name: KEYCLOAK_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: keycloak-clients
        key: components-lifecycle-service-secret
  
  - name: KEYCLOAK_LOGOUT_URL
    value: "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout"
```

### Test Environment

```yaml
env:
  - name: KEYCLOAK_ISSUER_URI
    value: "https://keycloak-test.acme.com/realms/f1-qa"
  
  - name: KEYCLOAK_CLIENT_ID
    value: "components-lifecycle-service"
  
  - name: KEYCLOAK_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: keycloak-clients-test
        key: components-lifecycle-service-secret
  
  - name: KEYCLOAK_LOGOUT_URL
    value: "https://keycloak-test.acme.com/realms/f1-qa/protocol/openid-connect/logout"
```

## Kubernetes Secret Creation

Create a Kubernetes secret for the Keycloak client secret:

```bash
# For test environment
kubectl create secret generic keycloak-clients-test \
  --from-literal=components-lifecycle-service-secret='YOUR_CLIENT_SECRET_HERE' \
  -n f1

# For production environment
kubectl create secret generic keycloak-clients \
  --from-literal=components-lifecycle-service-secret='YOUR_CLIENT_SECRET_HERE' \
  -n f1-prod
```

## Keycloak Client Configuration

### Create Client in Keycloak

1. Login to Keycloak Admin Console
2. Select realm: `f1-qa`
3. Go to Clients → Create
4. Configure:

```
Client ID: components-lifecycle-service
Client Protocol: openid-connect
Access Type: confidential
Standard Flow Enabled: ON
Direct Access Grants Enabled: ON
Service Accounts Enabled: ON (optional, for service-to-service)

Valid Redirect URIs:
  https://f1-gateway-test.acme.com/components-lifecycle-service/login/oauth2/code/keycloak
  https://f1-gateway.acme.com/components-lifecycle-service/login/oauth2/code/keycloak
  http://localhost:8080/login/oauth2/code/keycloak

Web Origins:
  https://f1-gateway-test.acme.com
  https://f1-gateway.acme.com
  http://localhost:8080

Base URL: /components-lifecycle-service/
```

5. Go to Credentials tab
6. Copy the Secret
7. Use this secret in the Kubernetes secret above

## Helm Chart Configuration

If using Helm, add to your `values.yaml`:

```yaml
env:
  keycloak:
    issuerUri: "https://keycloak.acme.com/realms/f1-qa"
    clientId: "components-lifecycle-service"
    clientSecretName: "keycloak-clients"
    clientSecretKey: "components-lifecycle-service-secret"
    logoutUrl: "https://keycloak.acme.com/realms/f1-qa/protocol/openid-connect/logout"
```

And in your deployment template:

```yaml
env:
  - name: KEYCLOAK_ISSUER_URI
    value: {{ .Values.env.keycloak.issuerUri | quote }}
  - name: KEYCLOAK_CLIENT_ID
    value: {{ .Values.env.keycloak.clientId | quote }}
  - name: KEYCLOAK_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ .Values.env.keycloak.clientSecretName }}
        key: {{ .Values.env.keycloak.clientSecretKey }}
  - name: KEYCLOAK_LOGOUT_URL
    value: {{ .Values.env.keycloak.logoutUrl | quote }}
```

## Local Development

For local development, create `application-local.properties`:

```properties
keycloak.issuer-uri=https://keycloak-test.acme.com/realms/f1-qa
spring.security.oauth2.resourceserver.jwt.issuer-uri=${keycloak.issuer-uri}
spring.security.oauth2.client.provider.keycloak.issuer-uri=${keycloak.issuer-uri}

spring.security.oauth2.client.registration.keycloak.client-id=components-lifecycle-service
spring.security.oauth2.client.registration.keycloak.client-secret=YOUR_LOCAL_SECRET

keycloak.logout-url=https://keycloak-test.acme.com/realms/f1-qa/protocol/openid-connect/logout
```

Run with:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Verification

After deployment:

### Test Redirect to Login
```bash
curl -I https://f1-gateway-test.acme.com/components-lifecycle-service/

# Expected: HTTP 302 redirect
# Location: https://keycloak-test.acme.com/realms/f1-qa/protocol/openid-connect/auth?...
```

### Test with Browser
1. Open: https://f1-gateway-test.acme.com/components-lifecycle-service/
2. Should redirect to Keycloak login
3. After login, should return to service

### Test API with JWT
```bash
# Get token
TOKEN=$(curl -s -X POST \
  "https://keycloak-test.acme.com/realms/f1-qa/protocol/openid-connect/token" \
  -d "client_id=components-lifecycle-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r .access_token)

# Test API
curl -H "Authorization: Bearer $TOKEN" \
  https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules

# Expected: HTTP 200 with data
```

## Troubleshooting

### 401 Still Appears
- Check environment variables are set correctly
- Verify Keycloak client secret matches
- Check Valid Redirect URIs in Keycloak include your service URL

### Redirect Loop
- Check Base URL in Keycloak client settings
- Verify X-Forwarded-* headers are being sent by gateway
- Check `server.forward-headers-strategy=FRAMEWORK` is set

### Token Validation Fails
- Verify `KEYCLOAK_ISSUER_URI` matches Keycloak realm URL exactly
- Check service can reach Keycloak (network/firewall)
- Verify JWT token audience includes your client-id

## Security Notes

- Never commit client secrets to Git
- Use Kubernetes secrets for production
- Rotate client secrets periodically
- Monitor authentication failures
- Enable audit logging in Keycloak

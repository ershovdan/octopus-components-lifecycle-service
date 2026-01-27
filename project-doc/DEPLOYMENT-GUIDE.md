# Quick Deployment Guide

## Build and Deploy

### 1. Build the Application
```bash
cd /Users/ersh/wrk/github/octopus-components-lifecycle-service
./gradlew clean bootJar
```

### 2. Build Docker Image
```bash
./gradlew dockerBuildImage
```

### 3. Deploy with Helm
```bash
helm upgrade components-lifecycle-service-test helm-repo/spring-cloud \
  --atomic --install --timeout 5m \
  --set image.tag=<your-version> \
  --set componentName=components-lifecycle-service \
  --set configLabel=master \
  --set dockerRegistry=docker.artifactory.acme.com \
  --set route.clusterDomain=apps.ocpm.eq.acme.com \
  --set additionalProfile=hotel \
  --set image.name=octopusden/components-lifecycle-service \
  --set resources.limits.cpu=600m \
  -f okd/deployments/test/default.yml \
  -f okd/deployments/test/components-lifecycle-service.yml \
  -n f1
```

## Configuration Options

### Default (No Changes Needed) - Trust Gateway
The application will start with **no JWT configuration** and trust the API Gateway.

### Optional - Enable JWT Validation
Add to Config Server (`components-lifecycle-service-cloud-qa.yml`):
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.acme.com/realms/f1-qa
```

## Verification

### Check Logs
```bash
kubectl logs -n f1 deployment/components-lifecycle-service-test --tail=100
```

### Test Endpoints
```bash
# Health check (should work)
curl https://f1-gateway-test.acme.com/components-lifecycle-service/actuator/health

# User info (requires authentication)
curl https://f1-gateway-test.acme.com/components-lifecycle-service/auth/me

# API endpoint (requires authentication)
curl https://f1-gateway-test.acme.com/components-lifecycle-service/api/rules
```

## Troubleshooting

### Application won't start
- Check logs: `kubectl logs -n f1 deployment/components-lifecycle-service-test`
- Verify Config Server is accessible
- Check database connection

### "anonymous" user returned
- Verify API Gateway forwards JWT tokens
- Check `TokenRelay` filter is enabled in gateway
- Enable debug logging: `logging.level.org.springframework.security=DEBUG`

### 401 Unauthorized with valid token
- Check if issuer-uri is configured correctly
- Verify Keycloak realm name matches
- Ensure JWT hasn't expired

## Current Status

✅ **Code ready**  
✅ **No gateway changes needed**  
✅ **Works in trust-gateway mode by default**  
✅ **Can enable JWT validation via Config Server**  

Deploy and test! 🚀

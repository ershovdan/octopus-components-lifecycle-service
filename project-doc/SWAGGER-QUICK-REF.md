# Quick Reference: Swagger API Documentation

## 🔗 Swagger UI URL

### Production/Test Environment

```
https://f1-gateway-test.acme.com/components-lifecycle-service/swagger-ui.html
```

### Local Development

```
http://localhost:8080/swagger-ui.html
```

---

## 📋 Other Useful URLs

### OpenAPI Specification (JSON)
```
https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs
```

### OpenAPI Specification (YAML)
```
https://f1-gateway-test.acme.com/components-lifecycle-service/v3/api-docs.yaml
```

---

## 🚀 What Was Done

✅ Added Swagger endpoints to SecurityConfig `permitAll()`
✅ Swagger UI now accessible without authentication
✅ SpringDoc version: 2.7.0

---

## 📦 Deploy to Activate

The changes need to be deployed:

```bash
./gradlew clean build
kubectl rollout restart deployment/components-lifecycle-service-test -n f1
```

After deployment, Swagger will be accessible!

---

## 📖 Full Documentation

See [SWAGGER-API-DOCS.md](SWAGGER-API-DOCS.md) for complete details.

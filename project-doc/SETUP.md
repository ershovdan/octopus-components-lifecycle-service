# Quick Setup for Contributors

## Install Pre-Commit Hook (Recommended)

To automatically check for domain name issues before each commit:

```bash
# From project root
cp scripts/pre-commit.sample .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## What It Does

The pre-commit hook will:
- ❌ **Block** commits containing `acme.com`
- ⚠️ **Warn** about potential hardcoded credentials
- ✅ **Allow** commits after you review warnings

## Test It

Try to commit a file with `acme.com` - it should be blocked:

```bash
# This will be blocked by the hook
echo "https://keycloak.acme.com/test" > test.txt
git add test.txt
git commit -m "test"
# Expected: ERROR and commit blocked
```

## Manual Check (Alternative)

If you don't want to use the hook, manually check before committing:

```bash
# Check for forbidden domains
git diff --cached | grep -i "acme"

# If no output = good to commit ✅
```

## Read More

- [PROJECT-GUIDELINES.md](PROJECT-GUIDELINES.md) - Full guidelines
- [CONTRIBUTING.md](CONTRIBUTING.md) - Detailed contribution guide

---

**Remember:** Always use `acme.com` for examples in documentation! 🎯

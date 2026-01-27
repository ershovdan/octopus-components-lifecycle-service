# Contributing Guidelines

## Documentation Standards

### ⚠️ CRITICAL: Never Use Real Domain Names

When creating examples or documentation:

**✅ Allowed:**
- `acme.com` - Primary example domain
- `example.com` - Generic examples
- `localhost` or `127.0.0.1` - Local development
- Placeholder values like `your-domain.com`

**❌ Forbidden:**
- Real company domain names
- Actual internal hostnames
- Real infrastructure URLs

### Why This Matters

1. **Security**: Exposing real infrastructure details is a security risk
2. **Privacy**: Company domains should not appear in public repos
3. **Portability**: Examples should work for anyone

## Before Committing

Run this checklist:

```bash
# 1. Search for forbidden domains
git grep -i "acme"

# 2. Check for hardcoded credentials
git grep -iE "(password|secret|token|api[_-]?key)\s*=\s*['\"].+['\"]"

# 3. Review your changes
git diff --cached

# 4. Run tests (if available)
./gradlew test
```

## Git Pre-Commit Hook (Recommended)

Install the provided pre-commit hook to catch issues automatically:

```bash
cp scripts/pre-commit.sample .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

This hook will:
- Block commits containing real domain names
- Warn about potential hardcoded credentials
- Help maintain documentation standards

## Code Style

- Follow standard Java conventions
- Use meaningful variable names
- Add JavaDoc for public APIs
- Keep methods small and focused

## Commit Messages

Use clear, descriptive commit messages:

```
✅ Good:
feat: Add JWT authentication support
fix: Resolve null pointer in BuildParser
docs: Update deployment guide with JWT config

❌ Bad:
fixed stuff
wip
update
```

## Testing

- Write unit tests for business logic
- Test edge cases and error conditions
- Ensure tests are deterministic

## Questions?

If you're unsure about anything:
1. Check [PROJECT-GUIDELINES.md](PROJECT-GUIDELINES.md)
2. Review existing code for patterns
3. Ask the team before committing

Thank you for contributing! 🚀

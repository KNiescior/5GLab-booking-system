# Production Deployment Guide

This guide covers deploying the 5GLab Booking System to production.

## Pre-Deployment Checklist

### 1. Remove Development Components

Before deploying to production, **you MUST remove** the following:

| File | Why Remove |
|------|------------|
| `src/main/java/.../config/DataInitializer.java` | Creates test users with known passwords |

```bash
# Delete the DataInitializer
rm src/main/java/com/_glab/booking_system/config/DataInitializer.java
```

### 2. Security Configuration Review

Update `SecurityConfig.java`:

```java
// TODO items to address:
// 1. Configure CORS for your frontend domain
// 2. Consider re-enabling CSRF for browser clients
// 3. Review public endpoints - restrict /buildings/** and /labs/** if needed
// 4. Enable secure cookies (set secure=true) when using HTTPS
```

### 3. Environment Variables

Create production `.env` file with secure values:

```env
# Database
POSTGRES_DB=booking_prod
POSTGRES_USER=booking_app
POSTGRES_PASSWORD=<generate-strong-password>

# JWT
JWT_ACCESS_TOKEN_EXPIRY=15m
JWT_REFRESH_TOKEN_EXPIRY=7d
JWT_ISSUER=5glab-booking

# Email (Production SMTP)
MAIL_HOST=<your-smtp-host>
MAIL_PORT=587
MAIL_USERNAME=<your-smtp-username>
MAIL_PASSWORD=<your-smtp-password>
MAIL_FROM=noreply@yourdomain.com

# Frontend URL (for email links)
APP_FRONTEND_URL=https://your-frontend-domain.com
```

### 4. Generate Production JWT Keys

```bash
# Generate new keys for production (do NOT reuse development keys)
openssl genrsa -out private.pem 4096  # Use 4096-bit for production
openssl rsa -in private.pem -pubout -out public.pem

# Store securely - these should be in a secrets manager, not in the repo
```

---

## Initial Admin Setup (Bootstrap)

When deploying to a fresh database, you need to create the first admin account. The system provides a secure bootstrap flow.

### Option 1: Bootstrap API (Recommended)

The `/api/v1/bootstrap` endpoint allows creating the first admin **only when no admin exists**.

#### Step 1: Check Bootstrap Availability

```bash
curl https://your-api.com/api/v1/bootstrap/status
```

Response if no admin exists:
```json
{
  "bootstrapAvailable": true,
  "message": "No admin found - bootstrap available"
}
```

Response if admin already exists:
```json
{
  "bootstrapAvailable": false,
  "message": "Admin exists - bootstrap disabled"
}
```

#### Step 2: Create First Admin

```bash
curl -X POST https://your-api.com/api/v1/bootstrap/admin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@yourcompany.com",
    "firstName": "System",
    "lastName": "Administrator"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Admin account created. Password setup email sent to admin@yourcompany.com",
  "note": "The admin will need to set their password and configure MFA on their own device."
}
```

#### Step 3: Admin Completes Setup

The admin receives an email with a password setup link:

1. **Click the link** → Opens frontend password setup page
2. **Set password** → Account is enabled
3. **Login** → Returns `mfaSetupRequired: true` with `mfaToken`
4. **Setup MFA** → Scan QR code with their authenticator app
5. **Verify MFA** → Receives JWT and backup codes
6. **Done!** → Admin is fully set up with MFA on their own device

### Option 2: Database Seed Script

For automated deployments, you can use a SQL script:

```sql
-- Insert roles (run once)
INSERT INTO role (id, name) VALUES 
  (1, 'ADMIN'),
  (2, 'LAB_MANAGER'),
  (3, 'PROFESSOR')
ON CONFLICT DO NOTHING;

-- Insert initial admin (disabled, no password)
INSERT INTO "user" (email, username, first_name, last_name, enabled, role_id, created_at)
VALUES (
  'admin@yourcompany.com',
  'admin',
  'System',
  'Administrator',
  false,
  1,
  NOW()
);

-- Then use the bootstrap API or a separate script to generate 
-- the password setup token and send the email
```

---

## MFA Setup Flow (For Required Roles)

Admin and Lab Manager roles **require MFA**. Here's the complete flow:

```
┌─────────────────────────────────────────────────────────────────┐
│                     MFA REQUIRED LOGIN FLOW                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. POST /auth/login                                            │
│     { email, password }                                         │
│              │                                                   │
│              ▼                                                   │
│     ┌────────────────────┐                                      │
│     │ Password Correct?  │──No──► 401 Invalid credentials       │
│     └────────────────────┘                                      │
│              │ Yes                                               │
│              ▼                                                   │
│     ┌────────────────────┐                                      │
│     │ MFA Enabled?       │──Yes──► Return MfaChallengeResponse  │
│     └────────────────────┘         { mfaToken, mfaRequired }    │
│              │ No                           │                    │
│              ▼                              │                    │
│     ┌────────────────────┐                  │                    │
│     │ MFA Required       │                  │                    │
│     │ for Role?          │──No──► Return LoginResponse          │
│     └────────────────────┘        { accessToken, user }         │
│              │ Yes                                               │
│              ▼                                                   │
│     Return MfaSetupRequiredResponse                             │
│     { mfaToken, mfaSetupRequired }                              │
│              │                                                   │
│              ▼                                                   │
│  2. POST /auth/mfa/setup                                        │
│     { mfaToken }                                                │
│              │                                                   │
│              ▼                                                   │
│     Returns { secret, qrCodeDataUri, manualEntryUri }           │
│              │                                                   │
│              ▼                                                   │
│  3. User scans QR with Google Authenticator                     │
│              │                                                   │
│              ▼                                                   │
│  4. POST /auth/mfa/setup/verify                                 │
│     { mfaToken, secret, code }                                  │
│              │                                                   │
│              ▼                                                   │
│     Returns MfaSetupCompleteResponse                            │
│     { mfaEnabled, backupCodes, accessToken, user }              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Deployment Architecture

### Recommended Production Setup

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │   (HTTPS/TLS)   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
        │  App #1   │  │  App #2   │  │  App #3   │
        │ (Spring)  │  │ (Spring)  │  │ (Spring)  │
        └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    │   (Primary)     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    │   (Replica)     │
                    └─────────────────┘
```

### Docker Compose Production

Use `docker-compose.prod.yml`:

```bash
docker compose -f docker-compose.prod.yml up -d
```

---

## Post-Deployment Verification

### 1. Health Check

```bash
curl https://your-api.com/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Bootstrap Status

```bash
curl https://your-api.com/api/v1/bootstrap/status
```

### 3. Test Login Flow

After admin setup is complete:

```bash
# Login should return MFA challenge (not setup required)
curl -X POST https://your-api.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@yourcompany.com","password":"<password>"}'
```

---

## Security Considerations

### Bootstrap Endpoint

The `/api/v1/bootstrap` endpoint is designed to be secure:

- ✅ **Self-disabling**: Only works when no admin exists
- ✅ **No credentials exposed**: Sends email, doesn't return password
- ✅ **MFA enforced**: Admin must set up MFA on their own device
- ⚠️ **Consider removing**: After initial setup, you may want to remove `BootstrapController.java`

### Secrets Management

**Never commit to version control:**
- `.env` files with real credentials
- JWT private keys
- Database passwords
- SMTP credentials

**Recommended**: Use a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)

### HTTPS

**Always use HTTPS in production.** Update cookie settings:

```java
ResponseCookie.from("refreshToken", token)
    .httpOnly(true)
    .secure(true)  // Enable for HTTPS
    .sameSite("Strict")
    .build();
```

---

## Rollback Procedure

If deployment fails:

1. **Stop new containers**: `docker compose down`
2. **Restore database backup**: `pg_restore -d booking backup.sql`
3. **Deploy previous version**: `docker compose up -d`
4. **Verify health**: `curl /actuator/health`

---

## Monitoring

### Logs

```bash
# View application logs
docker compose logs -f booking_app

# Filter security events
docker compose logs booking_app | grep -E "(SECURITY|login|MFA)"
```

### Key Metrics to Monitor

- Failed login attempts (potential brute force)
- MFA verification failures
- Token refresh patterns
- API response times

---

## Support

For deployment issues:
1. Check logs: `docker compose logs booking_app`
2. Verify environment variables
3. Ensure database connectivity
4. Check JWT key permissions

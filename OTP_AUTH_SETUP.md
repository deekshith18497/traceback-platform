# TraceBack OTP setup

## 1. Configure email OTP

Configure these environment variables on the backend host:

- `OTP_EMAIL_ENABLED=true`
- `MAIL_HOST=smtp.gmail.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=your-sending-email@gmail.com`
- `MAIL_PASSWORD=your-google-app-password`

For Gmail, use a Google App Password rather than your normal account password.
Never put the App Password in frontend code or GitHub.

## 2. Configure SMS OTP

Configure these environment variables on the backend host:

- `OTP_SMS_ENABLED=true`
- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_FROM_NUMBER`

The Twilio account must be configured to send to the destination country.
Never commit the credentials to GitHub.

## 3. OTP policy

Defaults:
- OTP lifetime: 300 seconds
- Minimum resend interval: 60 seconds
- Maximum incorrect attempts: 5
- Password-reset authorization lifetime after OTP verification: 10 minutes

Change these through:
- `OTP_TTL_SECONDS`
- `OTP_RESEND_SECONDS`
- `OTP_MAX_ATTEMPTS`

## 4. Endpoints

### Request registration OTP

`POST /auth/register/request-otp`

Email example:

```json
{
  "name": "Test User",
  "channel": "EMAIL",
  "email": "test@example.com",
  "password": "Example@123"
}
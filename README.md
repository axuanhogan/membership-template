# Membership Template

This is a backend project template for a membership system developed using **Spring Boot 3**, **Spring Security**, **JWT**, and **Two-Factor Authentication (2FA OTP)** email verification mechanism.

The project uses an **H2 In-Memory Database** for data storage during development and integrates **Mailgun** as the service for sending verification and OTP emails.

---

## Prerequisites

- **Java 17** or above
- **Gradle 8.x** (Gradle Wrapper is included in the project)

---

## Quick Start

### 1. Set Up Environment Variables

Before starting the project, you must configure the required environment variables. A template file `.env.example` is provided in the project root directory.

Please copy and create the `.env` file:
```bash
cp .env.example .env
```

Then edit the `.env` file and fill in your settings, especially the API key and domain configuration for the Mailgun email service:

```ini
# JWT Secret Key (Default value provided, make sure to change it in production environment)
JET.SECRET="dGhpcy1pcy1hLXN1cGVyLXNlY3JldC1rZXktd2hpY2gtaXMtbG9uZy1lbm91Z2gtZm9yLWp3dC1zaWduaW5nLTI1Ni1iaXRzCg=="

# Mailgun Email Configuration (Get API Key and Domain from the Mailgun platform)
MAILGUN.API_KEY="your-mailgun-api-key"
MAILGUN.DOMAIN="your-mailgun-domain"
MAILGUN.BASE_URL="https://api.mailgun.net"

# Sender Email and Name
SENDER_EMAIL="sender@your-domain.com"
SENDER_NAME="Membership Support"

# Security Permit All Patterns
SECURITY_PERMIT_ALL_PATTERNS="/v1/auth/**,/h2-console/**,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html"
```

### 2. Run the Application

With the built-in Gradle Wrapper, you can execute the following command to start the local development server:

- **macOS / Linux**:
  ```bash
  ./gradlew bootRun
  ```
- **Windows**:
  ```cmd
  gradlew.bat bootRun
  ```

Once started, the application will run at `http://localhost:8080` by default.

### 3. Run Tests

You can run the unit and integration tests using the Gradle Wrapper:

```bash
./gradlew test
```

---

## Swagger API Documentation

This project integrates `springdoc-openapi` to provide dynamically generated interactive API documentation. Once the application is running, you can click the links below to browse and test the APIs:

- **Swagger UI Link**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Specification JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> [!NOTE]
> Management APIs under `/v1/users/**` require `Bearer {Access_Token}` for requests. You can click the "Authorize" button in the top right corner of Swagger UI, and enter `Bearer ` followed by your Access Token to authorize requests.

---

## H2 Database Console

This project uses an In-Memory Database for default data storage, which is initialized on every application startup.

- **H2 Console Link**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **Login Configuration Info**:
  - **Saved Settings**: `Generic H2 (Embedded)`
  - **Driver Class**: `org.h2.Driver`
  - **JDBC URL**: `jdbc:h2:mem:membershipdb` (Corresponds to settings in `application.yml`)
  - **User Name**: `sa`
  - **Password**: `password`

---

## API Endpoints

### 1. Authentication Module (AuthController)
- `POST /v1/auth/sign-up` - **Sign Up**: Register with Email and Password. Upon registration, the account will be disabled, and an activation email will be sent.
- `GET /v1/auth/sign-up/activate` - **Account Activation**: Receive the token from the activation email link and activate the user account if validation succeeds.
- `POST /v1/auth/sign-in` - **Sign In - Stage 1 (Password Verification)**: Verify account and password. If verification succeeds, send an OTP verification code to the registered email and return a `preAuthToken`.
- `PATCH /v1/auth/sign-in` - **Sign In - Stage 2 (2FA Verification)**: Provide `preAuthToken` and the received `otpCode` to complete 2FA. Return the formal JWT `accessToken` upon success.

### 2. User Management Module (UserController)
- `GET /v1/users/me/last-sign-in` - **Get Last Sign-In Time for Current User**: Requires a Bearer Token. The system resolves the user identity from the token and returns their last sign-in time.

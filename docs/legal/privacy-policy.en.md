# DraftURL Privacy Policy

> Effective date: [TBD]
> Last updated: 2026-03-30

> This is a translation provided for your convenience. In case of any discrepancy, the Korean version shall prevail.

---

DraftURL (hereinafter the "Service") values the personal information of its Users and complies with the Personal Information Protection Act (PIPA) and other applicable laws of the Republic of Korea.

---

## Article 1 (Categories of Personal Information Collected and Collection Methods)

### 1. Upon Member Registration (Required)

| Category | Purpose of Collection |
|----------|----------------------|
| Email address | Account identification, login, delivery of notices |
| Name | Display within the Service |
| Password | Account authentication (stored as BCrypt hash; plaintext is not retained) |

### 2. During Service Use (Automatic)

| Category | Purpose of Collection |
|----------|----------------------|
| Authentication token (cookie) | Maintaining login session |
| Document metadata (title, type, size, creation/modification date) | Service provision, document management |

### 3. Web Analytics (Automatic)

| Category | Purpose of Collection |
|----------|----------------------|
| Page visit records (URL, referrer) | Understanding Service usage patterns |
| Browser type, operating system, screen resolution | Improving Service compatibility |
| Country/language settings | Prioritizing multilingual support |
| In-service events (document creation, login, registration) | Feature usage statistics |

Web analytics are powered by **Umami**. Umami does not use cookies, and all collected data is processed in an anonymized form that cannot identify individual Users. IP addresses are used only for country detection and are not stored. Analytics data is stored on a server operated by the Service (stats.drafturl.com).

---

## Article 2 (Purpose of Collection and Use of Personal Information)

1. **Service provision**: Account management, document hosting, login authentication
2. **Service improvement**: Usage statistics analysis, service quality enhancement
3. **Delivery of notices**: Terms changes, important service announcements (via email)
4. **Security**: Prevention of unauthorized access, detection of fraudulent use
5. **Error diagnosis**: Root cause analysis and stability improvement when service errors occur

---

## Article 3 (Use of Cookies)

The Service uses the following cookies for login authentication:

| Cookie Name | Purpose | Expiration | Attributes |
|-------------|---------|------------|------------|
| `access_token` | Login authentication | 1 hour | httpOnly, Secure, SameSite=Lax |
| `refresh_token` | Authentication token renewal | 7 days | httpOnly, Secure, SameSite=Lax |

These cookies are essential cookies required for the core functionality (login) of the Service. No cookies are created for Non-Members.

---

## Article 4 (Retention and Use Period of Personal Information)

| Category | Retention Period | Basis |
|----------|-----------------|-------|
| Account information (email, name, password hash) | Until account termination | Service agreement |
| Refresh token | 7 days after issuance (automatically deleted upon expiration) | Authentication functionality |
| Member document metadata | 30 days after document deletion | Service operations (retained internally for 30 days after deletion, then permanently deleted) |
| Non-Member documents | Expire 24 hours after creation; permanently deleted within 30 days after expiration | Service policy |
| Web analytics data | 12 months from date of collection | Service improvement |

Upon account termination, personal information is destroyed **within 30 days**. However, if retention is required under applicable laws, the information will be stored separately for the required period.

---

## Article 5 (Provision of Personal Information to Third Parties)

The Service does not provide Users' personal information to third parties. Exceptions are made only when required by specific legal provisions or lawful requests from investigative authorities.

---

## Article 6 (Outsourcing of Personal Information Processing and International Transfers)

The following external services are used for Service operations, and data may be stored outside of Korea accordingly:

| Provider | Purpose | Data Transferred | Storage Location |
|----------|---------|-----------------|-----------------|
| Cloudflare, Inc. | CDN, R2 object storage (document file storage) | Document content files | United States / Global |
| Sentry (Functional Software, Inc.) | Error tracking and service stability monitoring | Request context upon error occurrence (may include user identifiers) | United States |

Changes to outsourcing providers will be disclosed through this policy.

---

## Article 7 (Procedures and Methods for Destruction of Personal Information)

1. Personal information whose retention period has expired or whose processing purpose has been fulfilled is destroyed immediately.
2. Electronic files are permanently deleted using methods that prevent recovery.
3. Files stored in R2 are completely deleted via the object deletion API.

---

## Article 8 (User Rights and How to Exercise Them)

Users may exercise the following rights:

- **Right of access**: Users may view the status of processing of their personal information.
- **Right of rectification**: Users may request correction of inaccurate personal information.
- **Right of erasure**: Users may request deletion of their personal information (account termination).
- **Right to suspend processing**: Users may request suspension of the processing of their personal information.

Rights may be exercised through the in-service settings or by email (privacy@drafturl.com), and requests will be processed **within 10 days**.

If you do not wish to have web analytics data collected, you may block the execution of the Umami script using a **browser ad-blocking extension**.

---

## Article 9 (Measures to Ensure the Security of Personal Information)

1. **Password protection**: Passwords are hashed using the BCrypt algorithm before storage; plaintext is not retained.
2. **Encryption in transit**: All communications are encrypted via TLS (HTTPS).
3. **Authentication security**: JWT tokens are stored in httpOnly/Secure/SameSite=Lax cookies to prevent XSS/CSRF attacks.
4. **Access control**: Access to personal information is minimized.

---

## Article 10 (Personal Information Protection Officer)

| Item | Details |
|------|---------|
| Name | [Name] |
| Email | privacy@drafturl.com |

---

## Article 11 (Remedies for Infringement of Rights)

If you need to report or consult about a personal information infringement, you may contact the following agencies:

- Personal Information Infringement Report Center (KISA): 118, privacy.kisa.or.kr
- Personal Information Dispute Mediation Committee: 1833-6972, kopico.go.kr

---

## Article 12 (Changes to the Privacy Policy)

This policy may be amended in accordance with changes in applicable laws or Service policies. Changes will be announced through the Service website or via email.

---

## Supplementary Provisions

1. This Privacy Policy shall take effect on [effective date].

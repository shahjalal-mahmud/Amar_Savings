# Security Policy

The AmarSavings team takes the security of this project seriously. We
appreciate responsible disclosure of vulnerabilities and will work with
reporters to investigate, fix, and (where appropriate) publicly disclose
issues promptly.

## Supported Versions

AmarSavings is an actively maintained Android application. Security fixes
are applied to the most recent source on the `main` branch and shipped in
the next release on Google Play.

| Version | Supported            |
| ------- | -------------------- |
| `main`  | ✅ Active            |
| older   | ❌ No longer patched |

Released APKs distributed through Google Play will receive the fix once a
new version is published. Self-built APKs from older commits are not
covered.

## Reporting a Vulnerability

**Please use GitHub's Private vulnerability reporting feature.** This
keeps the report confidential while still letting us triage it through the
standard advisory workflow.

👉 **[Report a vulnerability](https://github.com/shahjalal-mahmud/Amar_Savings/security/advisories/new)**

A public email address is **not** provided by design. If you cannot use
GitHub's private reporting (for example, you are not a GitHub user),
open a regular issue **without** sensitive details and a maintainer will
follow up with a private channel.

> **Do not file security issues as public GitHub Issues or Discussions.**
> Public disclosure before a fix is shipped puts every user of the app at
> risk.

## What to Expect

When you submit a report via private vulnerability reporting, you can
expect:

1. **Acknowledgement within 72 hours** of submission.
2. **Triage within 7 days** — we will assess severity, scope, and
   reproducibility, and respond with our initial assessment.
3. **A fix timeline** based on severity:
   - Critical (data loss, remote code execution, credential exposure):
     patch as fast as possible, typically within days.
   - High: patch within the next minor release.
   - Medium / Low: scheduled for an upcoming release.
4. **Credit**, if you wish, in the advisory's "Credits" section when the
   fix is published.

If we determine the report is not a vulnerability, or is out of scope, we
will explain why.

## Disclosure Policy

We follow **coordinated disclosure**:

- Reporters agree not to publicly disclose the vulnerability until a fix
  has been released, or until 90 days have passed — whichever comes first.
- Maintainers will not publicly disclose the issue until a fix is
  available, and will keep the reporter informed throughout the process.
- Once a fix is released, we will publish a GitHub Security Advisory
  describing the issue, impact, and resolution.

## Out of Scope

The following are typically **not** considered security vulnerabilities
and may be closed without further action:

- Theoretical issues without a concrete impact or proof of concept
- Reports about dependency vulnerabilities **without** a working
  reproduction in the context of AmarSavings (please report those to the
  upstream maintainers instead)
- Social-engineering attacks against users or maintainers
- Physical-access attacks
- Lack of a security header or rate limiting on third-party endpoints
  that AmarSavings does not control
- "The app is unsigned" or similar — release builds are signed; the
  debug keystore is used for debug builds by Android convention

## Scope Notes for This Project

AmarSavings is **offline-first** and stores all data locally in a Room
database. There is no remote backend. The only network traffic is the
**opt-in** Google Drive backup feature, which uses the official Google
Sign-In and Google Drive APIs.

Areas of particular interest to security reviewers:

- Backup encryption-at-rest and integrity checks
- Local database encryption (currently the Room DB is unencrypted; users
  with sensitive data should enable device-level encryption)
- Google Sign-In and Drive scope handling
- `AndroidManifest.xml` exported components and `intent-filter`
  configurations

---

Thank you for helping keep AmarSavings safe. 🙏

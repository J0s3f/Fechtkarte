# Security Policy

## Reporting a vulnerability

If you find a security issue in Fechtkarte, please report it privately rather than opening a
public GitHub issue — use GitHub's private vulnerability reporting feature on this repository
(Security tab → "Report a vulnerability").

Given the app's actual attack surface — no network access, no accounts, no server component,
no third-party SDKs (see [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md)) — most classes of remote
vulnerability don't apply here. Reports about the local Room database, file export/share paths
(`FileProvider` scoping in particular), or dependency vulnerabilities are all still very much
welcome.

## Supported versions

Only the latest published release is supported. There is no long-term-support branch.

## Response

Reports are reviewed as they come in. There's no formal SLA — this is a small open-source
project — but security reports are prioritised over ordinary feature requests.

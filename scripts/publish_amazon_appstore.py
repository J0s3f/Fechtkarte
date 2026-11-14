#!/usr/bin/env python3
"""Upload a signed APK and the fastlane store listing to Amazon Appstore, then submit for review.

Unlike Google Play, there is no first-party fastlane action for Amazon Appstore, and the one
community plugin (fastlane-plugin-amazon_app_store) would pull in a full Ruby/Fastfile toolchain
for what is, underneath, a handful of JSON HTTP calls against Amazon's documented Appstore
Developer API (https://developer.amazon.com/docs/app-submission-api/overview.html). This talks
to it directly, with nothing beyond the Python standard library.

Everything here runs as ONE edit, not split across separate upload/listing jobs the way the
Google Play workflow splits `deploy`/`deploy-listing`: Amazon allows only one open edit per app,
so a binary upload and a listing update racing in two concurrently-running jobs would either
collide or silently clobber each other. Google's API has no such shared resource to race over,
which is why that split is safe there and isn't attempted here.

Usage: publish_amazon_appstore.py --app-id ID --client-id ID --client-secret SECRET
           --apk PATH --metadata-dir fastlane/metadata/android --version-code N
"""
from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request

TOKEN_URL = "https://api.amazon.com/auth/O2/token"
API_BASE = "https://developer.amazon.com/api/appstore/v1"

# Must match the locale directories already maintained under fastlane/metadata/android for
# Google Play -- one listing source, two upload paths, same reasoning as that workflow's own
# deploy-listing job.
LOCALES = ("en-US", "de-DE", "fr-FR")

# Enforced by Amazon's own listing form; per-locale changelogs are already kept under this limit
# for Google Play and F-Droid (see CLAUDE.md), so this is a safety net, not the real constraint.
MAX_RECENT_CHANGES_LEN = 500


def _request(method: str, url: str, token: str, data=None, extra_headers=None, raw_body=False):
    headers = {"Accept": "application/json", "Authorization": f"Bearer {token}"}
    if extra_headers:
        headers.update(extra_headers)
    body = None
    if data is not None:
        if raw_body:
            body = data
        else:
            body = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read()
            return (json.loads(payload) if payload else {}), response.headers.get("ETag")
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} failed: {error.code} {detail}") from error


def get_access_token(client_id: str, client_secret: str) -> str:
    body = (
        f"grant_type=client_credentials&client_id={client_id}"
        f"&client_secret={client_secret}&scope=appstore::apps:readwrite"
    ).encode("utf-8")
    request = urllib.request.Request(
        TOKEN_URL,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read())["access_token"]


def create_edit(app_id: str, token: str) -> str:
    edit, _ = _request("POST", f"{API_BASE}/applications/{app_id}/edits", token, data={})
    return edit["id"]


def upload_apk(app_id: str, edit_id: str, token: str, apk_path: str) -> None:
    apk_slot, etag = _request(
        "POST", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks", token, data={}
    )
    with open(apk_path, "rb") as apk_file:
        apk_bytes = apk_file.read()
    _request(
        "PUT",
        f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks/{apk_slot['id']}/replace",
        token,
        data=apk_bytes,
        raw_body=True,
        extra_headers={"Content-Type": "application/octet-stream", "If-Match": etag},
    )


def push_listing(app_id: str, edit_id: str, token: str, metadata_dir: str, version_code: str) -> None:
    for locale in LOCALES:
        locale_dir = f"{metadata_dir}/{locale}"
        listing, etag = _request(
            "GET", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/listings/{locale}", token
        )
        listing["title"] = _read(f"{locale_dir}/title.txt")
        listing["shortDescription"] = _read(f"{locale_dir}/short_description.txt")
        listing["description"] = _read(f"{locale_dir}/full_description.txt")
        listing["recentChanges"] = _read(f"{locale_dir}/changelogs/{version_code}.txt")[
            :MAX_RECENT_CHANGES_LEN
        ]
        _request(
            "PUT",
            f"{API_BASE}/applications/{app_id}/edits/{edit_id}/listings/{locale}",
            token,
            data=listing,
            extra_headers={"If-Match": etag},
        )


def _read(path: str) -> str:
    with open(path, "r", encoding="utf-8") as text_file:
        return text_file.read().strip()


def submit_edit(app_id: str, edit_id: str, token: str) -> None:
    _request("POST", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/validate", token, data={})
    _request("POST", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/commit", token, data={})


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-id", required=True)
    parser.add_argument("--client-id", required=True)
    parser.add_argument("--client-secret", required=True)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--metadata-dir", required=True)
    parser.add_argument("--version-code", required=True)
    args = parser.parse_args()

    token = get_access_token(args.client_id, args.client_secret)
    edit_id = create_edit(args.app_id, token)
    upload_apk(args.app_id, edit_id, token, args.apk)
    push_listing(args.app_id, edit_id, token, args.metadata_dir, args.version_code)
    submit_edit(args.app_id, edit_id, token)
    print(f"Amazon Appstore edit {edit_id} submitted for review.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

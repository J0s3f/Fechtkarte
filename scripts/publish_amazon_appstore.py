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

Usage: AMAZON_APP_ID=... AMAZON_CLIENT_ID=... AMAZON_CLIENT_SECRET=... \
           publish_amazon_appstore.py --apk PATH --metadata-dir fastlane/metadata/android \
           --version-code N
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
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


def _request(
    method: str, url: str, token: str, data=None, extra_headers=None, raw_body=False, not_found_ok=False
):
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
        if not_found_ok and error.code == 404:
            return None, None
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} failed: {error.code} {detail}") from error


def get_access_token(client_id: str, client_secret: str) -> str:
    # urlencode, not an f-string: a client secret containing '+', '=' or '&' (common in these
    # credentials) corrupts a hand-built form body silently -- Amazon just sees a different,
    # wrong secret and returns an opaque 401 with no indication the body was ever malformed.
    body = urllib.parse.urlencode(
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            "scope": "appstore::apps:readwrite",
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        TOKEN_URL,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read())["access_token"]
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"POST {TOKEN_URL} failed: {error.code} {detail}") from error


def get_or_create_edit(app_id: str, token: str) -> str:
    # An app can have only one open edit at a time; createEdit fails outright if one already
    # exists (confirmed live: error_new_version_creation_not_allowed), so a previous run that
    # got interrupted after creating its edit but before committing it leaves that edit open
    # for every later run to continue, not just the one that created it.
    active_edit, _ = _request(
        "GET", f"{API_BASE}/applications/{app_id}/edits", token, not_found_ok=True
    )
    if active_edit:
        return active_edit["id"]
    edit, _ = _request("POST", f"{API_BASE}/applications/{app_id}/edits", token, data={})
    return edit["id"]


def upload_apk(app_id: str, edit_id: str, token: str, apk_path: str) -> None:
    with open(apk_path, "rb") as apk_file:
        apk_bytes = apk_file.read()
    file_name = apk_path.rsplit("/", 1)[-1]

    # A fresh edit carries over the app's existing APK(s): replaceApk (PUT .../replace) is the
    # right call for an update, since it preserves that APK's device-targeting configuration --
    # uploadApk (POST .../apks/upload) adds a SEPARATE new APK alongside it instead, which is
    # for multi-APK device targeting, not for shipping a new version of the one APK this app
    # ships. Only fall back to uploadApk when the edit has no APK yet at all (first-ever
    # publish, carried over from nothing).
    existing_apks, _ = _request(
        "GET", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks", token
    )
    if existing_apks:
        apk_id = existing_apks[0]["id"]
        _, etag = _request(
            "GET", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks/{apk_id}", token
        )
        _request(
            "PUT",
            f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks/{apk_id}/replace",
            token,
            data=apk_bytes,
            raw_body=True,
            extra_headers={
                "Content-Type": "application/octet-stream",
                "If-Match": etag,
                "fileName": file_name,
            },
        )
    else:
        _request(
            "POST",
            f"{API_BASE}/applications/{app_id}/edits/{edit_id}/apks/upload",
            token,
            data=apk_bytes,
            raw_body=True,
            extra_headers={"Content-Type": "application/octet-stream", "fileName": file_name},
        )


def push_listing(app_id: str, edit_id: str, token: str, metadata_dir: str, version_code: str) -> None:
    for locale in LOCALES:
        locale_dir = f"{metadata_dir}/{locale}"
        listing, etag = _request(
            "GET", f"{API_BASE}/applications/{app_id}/edits/{edit_id}/listings/{locale}", token
        )
        listing["title"] = _read(f"{locale_dir}/title.txt")
        listing["shortDescription"] = _read(f"{locale_dir}/short_description.txt")
        listing["fullDescription"] = _read(f"{locale_dir}/full_description.txt")
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
    # commit requires the edit resource's own current ETag as If-Match -- fetched fresh here
    # rather than reused from get_or_create_edit(), since the apk/listing writes above are sub-resource
    # mutations that may have advanced the parent edit's ETag in between.
    _, etag = _request("GET", f"{API_BASE}/applications/{app_id}/edits/{edit_id}", token)
    # data=None, not {}: commitEdit defines no body parameter at all (confirmed against the real
    # API -- sending an empty JSON body with Content-Type: application/json, which createEdit and
    # validateEdit both silently tolerate, gets this specific endpoint rejected with a bare 403
    # Forbidden and no validation detail, easy to mistake for a permissions problem instead).
    _request(
        "POST",
        f"{API_BASE}/applications/{app_id}/edits/{edit_id}/commit",
        token,
        data=None,
        extra_headers={"If-Match": etag},
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--metadata-dir", required=True)
    parser.add_argument("--version-code", required=True)
    args = parser.parse_args()

    # Credentials come from the environment, not argv: a shell `run:` step still expands `$`
    # and backticks inside double-quoted "$VAR" interpolation, so a secret containing either
    # would arrive here silently corrupted. Reading os.environ directly skips the shell
    # entirely for these three values.
    try:
        app_id = os.environ["AMAZON_APP_ID"]
        client_id = os.environ["AMAZON_CLIENT_ID"]
        client_secret = os.environ["AMAZON_CLIENT_SECRET"]
    except KeyError as missing:
        raise SystemExit(f"Required environment variable {missing} is not set.") from missing

    token = get_access_token(client_id, client_secret)
    edit_id = get_or_create_edit(app_id, token)
    upload_apk(app_id, edit_id, token, args.apk)
    push_listing(app_id, edit_id, token, args.metadata_dir, args.version_code)
    submit_edit(app_id, edit_id, token)
    print(f"Amazon Appstore edit {edit_id} submitted for review.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

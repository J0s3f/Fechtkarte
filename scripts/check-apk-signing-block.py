#!/usr/bin/env python3
"""Fail if a release APK carries a signing block entry F-Droid would reject.

F-Droid rebuilds this app's releases from source and publishes the result only when it matches
the APK on GitHub Releases byte for byte (see DESIGN_CHOICES.md). An APK Signing Block entry
that AGP derives from the build environment rather than from the source tree -- the "Dependency
metadata" block above all -- breaks that silently: the build still succeeds, the release still
installs, and only F-Droid's verification notices, long after the tag is public.

So this asserts what the APK actually contains, rather than trusting `dependenciesInfo` to have
stayed switched off in app/build.gradle.kts.

Usage: check-apk-signing-block.py <apk> [<apk> ...]
"""
import struct
import sys

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"

# The only entries a release APK of this project may carry. Anything else is either an AGP
# feature that needs to be switched off for reproducibility, or something worth understanding
# before it ships.
ALLOWED_BLOCK_IDS = {
    0x7109871A: "APK Signature Scheme v2",
    0xF05368C0: "APK Signature Scheme v3",
    0x1B93AD61: "APK Signature Scheme v3.1",
    0x42726577: "verity padding",
}

REJECTED_BLOCK_IDS = {
    0x504B4453: "Dependency metadata (set dependenciesInfo.includeInApk = false)",
}


def read_signing_block(apk_bytes):
    end_of_central_directory = apk_bytes.rfind(b"PK\x05\x06")
    if end_of_central_directory < 0:
        raise ValueError("not a zip file")
    central_directory_offset = struct.unpack_from("<I", apk_bytes, end_of_central_directory + 16)[0]
    magic_start = central_directory_offset - len(APK_SIG_BLOCK_MAGIC)
    if apk_bytes[magic_start:central_directory_offset] != APK_SIG_BLOCK_MAGIC:
        raise ValueError("no APK Signing Block -- unsigned, or v1-signed only")
    block_size = struct.unpack_from("<Q", apk_bytes, magic_start - 8)[0]
    return apk_bytes[central_directory_offset - block_size:magic_start - 8]


def block_ids(signing_block):
    offset = 0
    while offset < len(signing_block):
        pair_length = struct.unpack_from("<Q", signing_block, offset)[0]
        yield struct.unpack_from("<I", signing_block, offset + 8)[0]
        offset += 8 + pair_length


def check(path):
    with open(path, "rb") as apk:
        signing_block = read_signing_block(apk.read())

    problems = []
    for block_id in block_ids(signing_block):
        if block_id in ALLOWED_BLOCK_IDS:
            print(f"    ok       0x{block_id:08x}  {ALLOWED_BLOCK_IDS[block_id]}")
        elif block_id in REJECTED_BLOCK_IDS:
            problems.append(f"0x{block_id:08x}  {REJECTED_BLOCK_IDS[block_id]}")
        else:
            problems.append(f"0x{block_id:08x}  unrecognised signing block")

    for problem in problems:
        print(f"    REJECT   {problem}")
    return not problems


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    failed = False
    for apk_path in sys.argv[1:]:
        print(apk_path)
        if not check(apk_path):
            failed = True
    if failed:
        sys.exit("APK signing block check FAILED -- this release would not reproduce on F-Droid.")
    print("APK signing block check passed.")

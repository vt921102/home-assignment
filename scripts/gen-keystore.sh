#!/usr/bin/env bash
# ============================================================
# Generate PKCS12 keystore for JWT RS256 signing.
# Run once before first docker compose up.
#
# Usage:
#   chmod +x scripts/gen-keystore.sh
#   ./scripts/gen-keystore.sh
#
# Override defaults via environment:
#   JWT_KEYSTORE_PASSWORD=secret ./scripts/gen-keystore.sh
# ============================================================

set -euo pipefail

KEYSTORE_DIR="keys"
KEYSTORE_FILE="${KEYSTORE_DIR}/jwt.p12"
KEY_ALIAS="${JWT_KEY_ALIAS:-jwt-signing}"
KEYSTORE_PASS="${JWT_KEYSTORE_PASSWORD:-changeit}"
VALIDITY_DAYS="${VALIDITY_DAYS:-3650}"
DNAME="CN=flash-sale-backend,OU=auth,O=example,L=Hanoi,C=VN"

# Create keys directory if it does not exist
mkdir -p "${KEYSTORE_DIR}"

# Skip if keystore already exists
if [ -f "${KEYSTORE_FILE}" ]; then
    echo "✓ Keystore already exists at ${KEYSTORE_FILE}"
    echo "  Delete it and re-run this script to regenerate."
    exit 0
fi

echo "→ Generating RSA-2048 keypair..."

keytool -genkeypair \
    -alias       "${KEY_ALIAS}" \
    -keyalg      RSA \
    -keysize     2048 \
    -validity    "${VALIDITY_DAYS}" \
    -storetype   PKCS12 \
    -keystore    "${KEYSTORE_FILE}" \
    -storepass   "${KEYSTORE_PASS}" \
    -keypass     "${KEYSTORE_PASS}" \
    -dname       "${DNAME}" \
    -v

echo ""
echo "✓ Keystore created: ${KEYSTORE_FILE}"
echo ""

# Print certificate details for verification
keytool -list \
    -keystore  "${KEYSTORE_FILE}" \
    -storepass "${KEYSTORE_PASS}" \
    -v 2>/dev/null | grep -E "(Alias|Valid|Type)"

echo ""
echo "Next steps:"
echo "  1. Copy .env.example to .env"
echo "  2. Set JWT_KEYSTORE_PASSWORD=${KEYSTORE_PASS} in .env"
echo "  3. Set OTP_HASH_PEPPER=\$(openssl rand -hex 32) in .env"
echo "  4. Run: docker compose up -d"
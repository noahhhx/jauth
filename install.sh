#!/usr/bin/env bash
set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-$HOME/.local/bin}"
BINARY="$INSTALL_DIR/jauth"
REPO="noahhhx/jauth"

case "$(uname -s)" in
  Linux)  : ;;
  *)      echo "ERROR: jauth only supports Linux" >&2; exit 1 ;;
esac

case "$(uname -m)" in
  x86_64)  ARCH="amd64" ;;
  aarch64) ARCH="arm64" ;;
  *)       echo "ERROR: unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

RELEASE_URL="https://api.github.com/repos/$REPO/releases/latest"

DOWNLOAD_URL=$(curl -fsSL "$RELEASE_URL" \
  | grep -o '"browser_download_url": *"[^"]*jauth"' \
  | head -1 \
  | sed 's/.*"browser_download_url": *"\([^"]*\)"/\1/')

if [[ -z "$DOWNLOAD_URL" ]]; then
  echo "ERROR: could not find jauth binary in latest release" >&2
  exit 1
fi

echo "Downloading jauth from $DOWNLOAD_URL ..."
mkdir -p "$INSTALL_DIR"
curl -fsSL "$DOWNLOAD_URL" -o "$BINARY"
chmod +x "$BINARY"

if ! command -v openconnect &>/dev/null; then
  echo "WARNING: openconnect is not installed. jauth requires it to connect to VPN."
  echo "         Install it with your package manager (e.g. pacman -S openconnect)."
fi

if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
  echo "NOTE: $INSTALL_DIR is not in your PATH. Add this to your shell config:"
  echo "      export PATH=\"\$HOME/.local/bin:\$PATH\""
fi

chmod +x $BINARY

echo "Installed jauth to $BINARY"
echo "Run: jauth help"


#!/bin/bash
set -euo pipefail

echo "==> Deleting k3d cluster 'mesoql'..."
k3d cluster delete mesoql
echo "Done."

#!/usr/bin/env bash
# =============================================================================
# Remoção dos objetos de teste no Cloudflare R2 (bucket primato-os-photos)
#
# O bucket continha exatamente 3 objetos, todos de Checklists Diários de teste.
#
# EXECUTADO em 2026-08-20, junto com cleanup-test-data.sql. O bucket ficou
# vazio. Mantido no repositório como registro do que foi removido.
#
# Uso único: as chaves abaixo já não existem mais.
#
# Requer as credenciais do backend/.env:
#   R2_ACCOUNT_ID, R2_ACCESS_KEY, R2_SECRET_KEY, R2_BUCKET_NAME
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/backend"
set -a && source .env && set +a

ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

OBJECTS=(
  "daily-reports/274/8fd26e95-85ab-4fc3-82cf-2f7dff6411ee.jpg"  # 327 KB — report 274 (Smoketest02)
  "daily-reports/275/c0d71faf-480f-40b1-bf26-3afdcf836883.jpg"  # 388 KB — report 275 (Smoketest02)
  "daily-reports/307/cfda55b1-c36c-43d4-bc07-2bb12db313be.jpg"  # 160 B  — report 307 (verificação)
)

echo "== Antes =="
aws s3api list-objects-v2 --bucket "$R2_BUCKET_NAME" --endpoint-url "$ENDPOINT" \
  --query 'Contents[].Key' --output text

for key in "${OBJECTS[@]}"; do
  echo "Removendo $key"
  aws s3api delete-object --bucket "$R2_BUCKET_NAME" --endpoint-url "$ENDPOINT" --key "$key"
done

echo "== Depois (deve estar vazio) =="
aws s3api list-objects-v2 --bucket "$R2_BUCKET_NAME" --endpoint-url "$ENDPOINT" \
  --query 'Contents[].Key' --output text

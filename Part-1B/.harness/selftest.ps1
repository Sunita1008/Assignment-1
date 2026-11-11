# Proves the harness works using fake agents (no model, no key, no Java needed).
Set-Location (Join-Path $PSScriptRoot "..")
uv run .harness/selftest.py
exit $LASTEXITCODE

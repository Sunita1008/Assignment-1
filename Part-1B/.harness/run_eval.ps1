# Runs the Part-1B evaluation against the real agent.
# Usage (from Part-1B):  powershell -ExecutionPolicy Bypass -File .harness\run_eval.ps1 <label> [delaySeconds]
# Needs: uv, Java 17+, a built agent jar, and MODEL_API_KEY / MODEL_PROVIDER / MODEL_NAME set.
param([string]$Label = "run", [double]$Delay = 6)
Set-Location (Join-Path $PSScriptRoot "..")
foreach ($v in "MODEL_API_KEY", "MODEL_PROVIDER", "MODEL_NAME") {
    if (-not [Environment]::GetEnvironmentVariable($v)) { Write-Host "Missing environment variable $v"; exit 2 }
}
if (-not (Test-Path "target\reminder-agent.jar")) { Write-Host "Build the agent first: mvn -B package"; exit 2 }
uv run .harness/run_eval.py --agent "java -jar target/reminder-agent.jar run" --label $Label --delay $Delay
exit $LASTEXITCODE

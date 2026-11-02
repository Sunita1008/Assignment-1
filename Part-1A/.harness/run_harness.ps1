param([string]$Label = "run")
Set-Location (Join-Path $PSScriptRoot "..")

$ts  = Get-Date -Format "yyyyMMdd-HHmmss"
$out = "evidence/harness-results/$ts-$Label"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$script:overall = 0

function Invoke-Gate([string]$Name, [string[]]$MvnArgs) {
    Write-Host "=== GATE: $Name ==="
    & mvn -B @MvnArgs 2>&1 | Out-File -FilePath "$out/$Name.log" -Encoding utf8
    $code = $LASTEXITCODE
    Set-Content -Path "$out/$Name.exitcode" -Value $code
    if ($code -eq 0) {
        $line = "${Name}: PASS"
    } else {
        $line = "${Name}: FAIL (exit $code) - see $out/$Name.log"
        $script:overall = 1
    }
    Write-Host $line
    Add-Content -Path "$out/summary.txt" -Value $line
}

Invoke-Gate "lint"     @("checkstyle:check")
Invoke-Gate "security" @("compile", "spotbugs:check")
Invoke-Gate "tests"    @("test")

$result = if ($script:overall -eq 0) { "HARNESS RESULT: PASS" } else { "HARNESS RESULT: FAIL" }
Write-Host $result
Add-Content -Path "$out/summary.txt" -Value $result
exit $script:overall
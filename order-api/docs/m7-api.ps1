# M7 request helper. Load once per window:  . .\order-api\docs\m7-api.ps1
# Usage:  Api GET /orders/$LastId
#         Api POST /orders '{"customer":"Asha","amountPaise":500}' -Headers 'Idempotency-Key: k1'
# Bodies and headers go through files, so PowerShell 5.1 never strips the quotes.
# After each call: $LastId = id from the Location header, $LastETag = the ETag header (quotes included).
function Api {
    param([string]$Method, [string]$Path, [string]$Json = '', [string[]]$Headers = @())
    $a = @('-s', '-i', '-X', $Method, "http://localhost:18080$Path")
    if ($Headers.Count -gt 0) {
        $hfile = Join-Path $env:TEMP 'm7-headers.txt'
        Set-Content -Encoding Ascii $hfile $Headers
        $a += @('-H', "@$hfile")
    }
    if ($Json) {
        $bfile = Join-Path $env:TEMP 'm7-body.json'
        Set-Content -Encoding Ascii $bfile $Json
        $a += @('-H', 'Content-Type: application/json', '--data-binary', "@$bfile")
    }
    $out = & curl.exe @a
    $out
    ''
    $loc = $out | Where-Object { $_ -match '^Location: ' } | Select-Object -First 1
    if ($loc) { $global:LastId = ($loc -replace '^Location: .*/', '').Trim() }
    $tag = $out | Where-Object { $_ -match '^ETag: ' } | Select-Object -First 1
    if ($tag) { $global:LastETag = ($tag -replace '^ETag: ', '').Trim() }
}

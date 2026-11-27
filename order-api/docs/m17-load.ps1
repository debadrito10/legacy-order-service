param([string]$Id, [int]$N = 100, [string]$Tag = "run")
$rows = foreach ($i in 1..$N) {
    $out = curl.exe -s -o NUL -w "%{http_code} %{time_total}" -H "X-Correlation-Id: $Tag-$i" "http://localhost:18080/orders/$Id/gst"
    $parts = $out -split ' '
    [pscustomobject]@{ Code = [int]$parts[0]; Ms = [double]$parts[1] * 1000 }
}
$ms = @($rows.Ms | Sort-Object)
$errors = @($rows | Where-Object { $_.Code -ge 500 }).Count
function P([double]$p) { $ms[[math]::Max(0, [math]::Ceiling($p * $ms.Count) - 1)] }
"requests=$N errors=$errors errorRate=$([math]::Round(100.0 * $errors / $N, 1))%"
"p50={0:N0} ms  p95={1:N0} ms  p99={2:N0} ms  max={3:N0} ms" -f (P 0.50), (P 0.95), (P 0.99), $ms[-1]
$rows | Group-Object Code | ForEach-Object { "status $($_.Name): $($_.Count)" }
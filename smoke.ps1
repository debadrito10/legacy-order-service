# Smoke test: the packaged JAR starts and runs to "Done." on Windows and Linux.
$jar = Get-ChildItem -Path target -Filter 'legacy-order-service-*.jar' | Select-Object -First 1
if (-not $jar) { 'SMOKE FAIL: no JAR in target'; exit 1 }
$out = & java -jar $jar.FullName 2>&1 | ForEach-Object { "$_" }
if ($LASTEXITCODE -eq 0 -and ($out -contains 'Done.')) { 'SMOKE PASS'; exit 0 } else { 'SMOKE FAIL'; $out; exit 1 }

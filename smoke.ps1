$out = cmd /c "java -jar target\legacy-order-service-1.0.0.jar 2>&1"
if ($LASTEXITCODE -eq 0 -and ($out -contains 'Done.')) { 'SMOKE PASS'; exit 0 } else { 'SMOKE FAIL'; $out; exit 1 }

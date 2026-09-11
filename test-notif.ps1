$body = @{account='admin';password='admin123'} | ConvertTo-Json -Compress
$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
$token = ($r.Content | ConvertFrom-Json).data.token

try {
    $r2 = Invoke-WebRequest -Uri 'http://localhost:8080/api/notifications/unread-count' -Method GET -Headers @{Authorization="Bearer $token"} -UseBasicParsing -TimeoutSec 5
    Write-Output "Status: $($r2.StatusCode) Body: $($r2.Content)"
} catch {
    $sr = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    Write-Output "Error: $($_.Exception.Response.StatusCode.value__) $($sr.ReadToEnd())"
}

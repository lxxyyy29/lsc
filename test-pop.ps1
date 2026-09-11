$body = @{account='admin';password='admin123'} | ConvertTo-Json -Compress
$r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 10
$token = ($r.Content | ConvertFrom-Json).data.token

# Test with gridId=1 (parent grid)
try {
    $r2 = Invoke-WebRequest -Uri 'http://localhost:8080/api/community/population?gridId=1' -Method GET -Headers @{Authorization="Bearer $token"} -UseBasicParsing -TimeoutSec 10
    $json = $r2.Content | ConvertFrom-Json
    Write-Output "gridId=1: success=$($json.success), count=$(@($json.data).count)"
} catch {
    $sr = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    Write-Output "gridId=1 Error: $($_.Exception.Response.StatusCode.value__) $($sr.ReadToEnd())"
}

# Test with gridId=10 (child grid)
try {
    $r3 = Invoke-WebRequest -Uri 'http://localhost:8080/api/community/population?gridId=10' -Method GET -Headers @{Authorization="Bearer $token"} -UseBasicParsing -TimeoutSec 10
    $json3 = $r3.Content | ConvertFrom-Json
    Write-Output "gridId=10: success=$($json3.success), count=$(@($json3.data).count)"
} catch {
    $sr3 = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    Write-Output "gridId=10 Error: $($_.Exception.Response.StatusCode.value__) $($sr3.ReadToEnd())"
}

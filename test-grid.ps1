# Test grid tree API
$session = Get-Content "$env:USERPROFILE\.grid-session" -ErrorAction SilentlyContinue
if ($session) {
    $token = $session
} else {
    # Try login first
    $body = @{account='admin';password='admin123'} | ConvertTo-Json -Compress
    $utf8 = [System.Text.Encoding]::UTF8.GetBytes($body)
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json;charset=UTF-8' -Body $utf8 -UseBasicParsing -TimeoutSec 10
        $json = $r.Content | ConvertFrom-Json
        $token = $json.data.token
        Write-Output "Logged in, token: $($token.Substring(0,20))..."
    } catch {
        Write-Output "Login failed: $($_.Exception.Message)"
        exit
    }
}

# Test grid tree
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/community/grids/tree' -Method GET -Headers @{Authorization="Bearer $token"} -UseBasicParsing -TimeoutSec 10
    Write-Output "Status: $($r.StatusCode)"
    Write-Output $r.Content.Substring(0, [Math]::Min(500, $r.Content.Length))
} catch {
    $sr = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    Write-Output "Error: $($_.Exception.Response.StatusCode.value__) $($sr.ReadToEnd())"
}

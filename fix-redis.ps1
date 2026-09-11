$path = 'C:\Users\19795\Desktop\redis\Redis-backup\redis.windows.conf'
$content = Get-Content $path -Raw
$content + "`nrequirepass 123456`n" | Set-Content $path -NoNewline
Write-Content "Done. Checking..."
Select-String -Path $path -Pattern '^requirepass'

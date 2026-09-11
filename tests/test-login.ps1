# ============================================================
# test-login.ps1 — 登录接口测试（合并原 test-login / test-login2 / test-login3）
#
# 覆盖：后端直连登录、前端代理登录、错误账号登录
# 公共逻辑见 TestCommon.psm1
# ============================================================
[CmdletBinding()]
param()

Import-Module "$PSScriptRoot\TestCommon.psm1" -Force
$cfg = Get-ApiConfig

Write-Output '========== 登录接口测试 =========='

# 1. 后端直连登录（admin / admin123）
$body = @{ account = $cfg.Account; password = $cfg.Password } | ConvertTo-Json -Compress
$r1 = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/auth/login" -Method POST -Body $body
Show-TestResult -Resp $r1 -Title '登录：后端直连 (admin/admin123)'

# 2. 前端代理登录（同一账号，验证 5175 代理转发）
$r2 = Invoke-ApiRequest -Uri "$($cfg.ProxyUrl)/auth/login" -Method POST -Body $body
Show-TestResult -Resp $r2 -Title '登录：前端代理 (admin/admin123)'

# 3. 错误密码登录（应返回 401 类错误）
$badBody = @{ account = $cfg.Account; password = 'wrongpass' } | ConvertTo-Json -Compress
$r3 = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/auth/login" -Method POST -Body $badBody
Show-TestResult -Resp $r3 -Title '登录：错误密码 (应失败)'

Write-Output '========== 完成 =========='

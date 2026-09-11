# ============================================================
# test-api.ps1 — 通用 API 冒烟测试（登录后探测核心只读接口连通性）
# ============================================================
[CmdletBinding()]
param()

Import-Module "$PSScriptRoot\TestCommon.psm1" -Force
$cfg = Get-ApiConfig

Write-Output '========== 通用 API 冒烟测试 =========='

try {
    $token = Get-AuthToken -BaseUrl $cfg.BaseUrl
    Write-Output '  [OK]   获取 token 成功'

    # 当前用户信息
    $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/auth/me" -Token $token
    Show-TestResult -Resp $resp -Title 'GET /auth/me'

    # 看板总览
    $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/community/dashboard/overview" -Token $token
    Show-TestResult -Resp $resp -Title 'GET /community/dashboard/overview'

    # 系统用户列表
    $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/system/users" -Token $token
    Show-TestResult -Resp $resp -Title 'GET /system/users'
} catch {
    Write-Output "  [FAIL] 测试异常: $($_.Exception.Message)"
    exit 1
}

Write-Output '========== 完成 =========='

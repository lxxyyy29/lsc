# ============================================================
# test-menu.ps1 — 菜单树接口测试（修复原脚本缺少 try-catch 的问题）
# ============================================================
[CmdletBinding()]
param()

Import-Module "$PSScriptRoot\TestCommon.psm1" -Force
$cfg = Get-ApiConfig

Write-Output '========== 菜单树接口测试 =========='

try {
    $token = Get-AuthToken -BaseUrl $cfg.BaseUrl
    Write-Output '  [OK]   获取 token 成功'

    $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/auth/menu-tree" -Token $token
    Show-TestResult -Resp $resp -Title 'GET /auth/menu-tree'
} catch {
    Write-Output "  [FAIL] 测试异常: $($_.Exception.Message)"
    exit 1
}

Write-Output '========== 完成 =========='

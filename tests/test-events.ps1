# ============================================================
# test-events.ps1 — 事件列表接口测试
# （合并原 test-api / test-events 中重复的事件列表查询逻辑）
# ============================================================
[CmdletBinding()]
param()

Import-Module "$PSScriptRoot\TestCommon.psm1" -Force
$cfg = Get-ApiConfig

Write-Output '========== 事件列表接口测试 =========='

try {
    $token = Get-AuthToken -BaseUrl $cfg.BaseUrl
    Write-Output '  [OK]   获取 token 成功'

    # 分页查询事件列表（含 excludeHidden）
    $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/events?page=1&size=20&excludeHidden=true" -Token $token
    Show-TestResult -Resp $resp -Title 'GET /events?page=1&size=20&excludeHidden=true'

    # 按状态筛选（待审核）
    $resp2 = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/events?page=1&size=20&status=PENDING_AUDIT" -Token $token
    Show-TestResult -Resp $resp2 -Title 'GET /events?status=PENDING_AUDIT'
} catch {
    Write-Output "  [FAIL] 测试异常: $($_.Exception.Message)"
    exit 1
}

Write-Output '========== 完成 =========='

# ============================================================
# TestCommon.psm1 — 接口测试公共封装模块
#
# 统一解决原 6 个 test-*.ps1 脚本中重复的登录逻辑、不一致的
# 错误处理与输出格式。所有测试脚本通过 Import-Module 引入本模块。
#
# 用法示例：
#   Import-Module "$PSScriptRoot\TestCommon.psm1" -Force
#   $cfg  = Get-ApiConfig
#   $tok  = Get-AuthToken                      # 统一登录
#   $resp = Invoke-ApiRequest -Uri "$($cfg.BaseUrl)/events?page=1&size=20" -Token $tok
#   Show-TestResult -Resp $resp                # 统一输出
# ============================================================

# ---------------- 全局配置 ----------------
$script:ApiConfig = [ordered]@{
    BaseUrl  = 'http://localhost:8080/api'   # 直连后端
    ProxyUrl = 'http://localhost:5175/api'   # 经前端代理
    Account  = 'admin'
    Password = 'admin123'
    Timeout  = 15                            # 秒
    MaxChars = 2000                          # 输出内容截断长度
}

function Get-ApiConfig {
    <#
    .SYNOPSIS
    返回公共配置（BaseUrl / ProxyUrl / 账号密码等）。
    #>
    return $script:ApiConfig
}

function Get-AuthToken {
    <#
    .SYNOPSIS
    统一登录并返回 JWT token。
    .PARAMETER BaseUrl
    目标服务地址（默认直连后端）。
    .PARAMETER Account
    登录账号（默认 admin）。
    .PARAMETER Password
    登录密码（默认 admin123）。
    #>
    param(
        [string]$BaseUrl = $script:ApiConfig.BaseUrl,
        [string]$Account = $script:ApiConfig.Account,
        [string]$Password = $script:ApiConfig.Password
    )
    $body = @{ account = $Account; password = $Password } | ConvertTo-Json -Compress
    $resp = Invoke-ApiRequest -Uri "$BaseUrl/auth/login" -Method POST -Body $body
    if ($resp.Code -ne 200 -or -not $resp.Json.data.token) {
        throw "登录失败: HTTP $($resp.Code) $($resp.Raw)"
    }
    return $resp.Json.data.token
}

function Invoke-ApiRequest {
    <#
    .SYNOPSIS
    统一 HTTP 请求封装：UTF-8 编码、Bearer Token、统一错误处理。
    .PARAMETER Uri
    完整请求地址（含 BaseUrl 前缀）。
    .PARAMETER Method
    HTTP 方法，默认 GET。
    .PARAMETER Body
    请求体对象（字典 / 字符串），自动 UTF-8 编码。
    .PARAMETER Token
    Bearer Token（可为空，未登录接口无需传）。
    .PARAMETER Timeout
    超时秒数，默认取公共配置。
    .PARAMETER AsJson
    是否解析响应 JSON 到 $resp.Json，默认 $true。
    .OUTPUTS
    返回 [PSCustomObject]：Code / Raw / Json（解析失败时 Json=$null）。
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [string]$Method = 'GET',
        $Body = $null,
        [string]$Token = '',
        [int]$Timeout = $script:ApiConfig.Timeout,
        [bool]$AsJson = $true
    )

    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $psArgs = @{
        Uri             = $Uri
        Method          = $Method
        Headers         = $headers
        UseBasicParsing = $true
        TimeoutSec      = $Timeout
    }
    if ($null -ne $Body) {
        $psArgs['ContentType'] = 'application/json;charset=UTF-8'
        # 调用方可能传入已序列化的 JSON 字符串，也可能传字典（hashtable/PSCustomObject）。
        # 仅对非字符串做一次 ConvertTo-Json，否则会二次编码成 "\"{...}\""，
        # 后端会返回 REQUEST_BODY_INVALID（请求数据格式错误或缺少请求体）。
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress }
        $psArgs['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
    }

    try {
        $r = Invoke-WebRequest @psArgs
        $raw = [string]$r.Content
        return [PSCustomObject]@{
            Code = [int]$r.StatusCode
            Raw  = $raw
            Json = if ($AsJson) { try { $raw | ConvertFrom-Json } catch { $null } } else { $null }
        }
    } catch {
        $code = 0
        $errBody = ''
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            try {
                $sr = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $errBody = $sr.ReadToEnd()
            } catch { $errBody = $_.Exception.Message }
        } else {
            $errBody = $_.Exception.Message
        }
        return [PSCustomObject]@{
            Code = $code
            Raw  = $errBody
            Json = $null
        }
    }
}

function Show-TestResult {
    <#
    .SYNOPSIS
    统一输出测试结果：状态码 + 截断内容（成功/失败统一格式）。
    .PARAMETER Resp
    Invoke-ApiRequest 返回对象。
    .PARAMETER Title
    用例标题（可选）。
    #>
    param(
        [Parameter(Mandatory = $true)]$Resp,
        [string]$Title = ''
    )
    $max = $script:ApiConfig.MaxChars
    if ($Title) { Write-Output "[$Title]" }
    if ($Resp.Code -ge 200 -and $Resp.Code -lt 300) {
        Write-Output "  [OK]   HTTP $($Resp.Code)"
    } else {
        $tag = if ($Resp.Code -eq 0) { '连接失败' } else { "HTTP $($Resp.Code)" }
        Write-Output "  [FAIL] $tag"
    }
    $content = if ($Resp.Raw) { $Resp.Raw.Substring(0, [Math]::Min($max, $Resp.Raw.Length)) } else { '(空响应)' }
    Write-Output "  $content"
    Write-Output ""
}

Export-ModuleMember -Function Get-ApiConfig, Get-AuthToken, Invoke-ApiRequest, Show-TestResult

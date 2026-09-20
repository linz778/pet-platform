param(
    [string]$BaseUrl = 'http://127.0.0.1:5173/api',
    [string]$UserPassword = '123456',
    [string]$SitterPassword = '123456',
    [string]$AdminPassword = 'admin123'
)

$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param([string]$Method, [string]$Path, [string]$Token, $Body)
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; TimeoutSec = 20 }
    if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = $Body | ConvertTo-Json -Depth 8
    }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 200) { throw "$Method $Path failed: $($response.code) $($response.message)" }
    return $response.data
}

function Login([string]$Username, [string]$Password) {
    return (Invoke-Api POST '/auth/login' '' @{ username = $Username; password = $Password }).token
}

$userToken = Login 'user' $UserPassword
$sitterToken = Login 'sitter' $SitterPassword
$adminToken = Login 'admin' $AdminPassword

$null = Invoke-Api POST '/wallet/recharge' $userToken @{ amount = 100 }
$userWalletBefore = Invoke-Api GET '/wallet/me' $userToken $null
$sitterWalletBefore = Invoke-Api GET '/wallet/me' $sitterToken $null
$pet = @(Invoke-Api GET '/pet/my' $userToken $null)[0]
if (-not $pet) { throw 'Seed user has no pet profile' }

$start = (Get-Date).AddHours(2).ToString('yyyy-MM-dd HH:mm:ss')
$created = Invoke-Api POST '/order/bounty' $userToken @{
    petId = $pet.id
    title = "全栈联调悬赏 $(Get-Date -Format 'MMdd-HHmmss')"
    description = '完成一次真实三角色订单闭环验证'
    amount = 12.34
    serviceAddress = '上海市黄浦区人民广场联调点'
    addressLat = 31.2304000
    addressLng = 121.4737000
    serviceStart = $start
}

$orderId = $created.id
$hall = Invoke-Api GET '/sitter/hall/page?page=1&size=50&lng=121.4737000&lat=31.2304000&radiusKm=5' $sitterToken $null
if (-not @($hall.records | Where-Object id -eq $orderId).Count) { throw "Order $orderId is missing from sitter hall" }

$null = Invoke-Api POST "/sitter/hall/$orderId/grab" $sitterToken $null
$null = Invoke-Api POST "/sitter/order/$orderId/checkin" $sitterToken @{ lat = 31.2304000; lng = 121.4737000 }

$workDir = 'D:\workfile'
New-Item -ItemType Directory -Path $workDir -Force | Out-Null
$proof = Join-Path $workDir "petplatform-smoke-$orderId.png"
try {
    [IO.File]::WriteAllBytes($proof, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='))
    $upload = Invoke-RestMethod -Method Post -Uri "$BaseUrl/file/upload?bizType=evidence" -Headers @{ Authorization = "Bearer $sitterToken" } -Form @{ file = Get-Item $proof } -TimeoutSec 20
    if ($upload.code -ne 200) { throw "Image upload failed: $($upload.code) $($upload.message)" }
    $null = Invoke-Api POST "/sitter/order/$orderId/task-proof" $sitterToken @{ imageUrl = $upload.data.url; remark = '全栈联调证明' }
} finally {
    Remove-Item -LiteralPath $proof -ErrorAction SilentlyContinue
}

$null = Invoke-Api POST "/sitter/order/$orderId/finish" $sitterToken $null
$null = Invoke-Api POST "/admin/dispatch/bounty/$orderId/review" $adminToken @{ approved = $true; result = '联调审核通过' }

$userOrder = Invoke-Api GET "/order/$orderId" $userToken $null
$sitterOrder = Invoke-Api GET "/order/$orderId" $sitterToken $null
$adminOrder = Invoke-Api GET "/order/$orderId" $adminToken $null
$userWallet = Invoke-Api GET '/wallet/me' $userToken $null
$sitterWallet = Invoke-Api GET '/wallet/me' $sitterToken $null
$userInbox = Invoke-Api GET '/notifications?limit=20' $userToken $null
$sitterInbox = Invoke-Api GET '/notifications?limit=20' $sitterToken $null

if ($userOrder.status -ne 5 -or $sitterOrder.status -ne 5 -or $adminOrder.status -ne 5) {
    throw "Order $orderId did not reach COMPLETED"
}
if ([decimal]$userWallet.frozen -ne [decimal]$userWalletBefore.frozen) { throw 'Escrow was not released after settlement' }
if ([decimal]$userWallet.balance -ne ([decimal]$userWalletBefore.balance - [decimal]$userOrder.amount)) { throw 'Owner balance delta is incorrect' }
if ([decimal]$sitterWallet.balance -ne ([decimal]$sitterWalletBefore.balance + [decimal]$sitterOrder.sitterIncome)) { throw 'Sitter income was not settled' }
if (-not @($userInbox.items | Where-Object businessId -eq $orderId).Count) { throw 'User notification missing' }
if (-not @($sitterInbox.items | Where-Object businessId -eq $orderId).Count) { throw 'Sitter notification missing' }

[PSCustomObject]@{
    OrderId = $orderId
    OrderNo = $userOrder.orderNo
    Status = $userOrder.statusText
    EscrowReleased = $true
    SitterIncome = $sitterOrder.sitterIncome
    UserNotified = $true
    SitterNotified = $true
}

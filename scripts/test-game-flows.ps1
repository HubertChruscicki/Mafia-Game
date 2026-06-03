# Mafia Game API flow tests
$ErrorActionPreference = "Stop"
$AuthBase = "http://localhost:8081"
$GameBase = "http://localhost:8082"

function Register-User($username) {
    $body = @{ username = $username; email = "$username@test.com"; password = "Test1234!" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$AuthBase/api/auth/register" -Method POST -ContentType "application/json" -Body $body
    return $r
}

function Login-User($email) {
    $body = @{ email = $email; password = "Test1234!" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$AuthBase/api/auth/login" -Method POST -ContentType "application/json" -Body $body
    return $r.token
}

function Api-Game($token, $method, $path, $body = $null) {
    $headers = @{ Authorization = "Bearer $token" }
    $params = @{ Uri = "$GameBase$path"; Method = $method; Headers = $headers }
    if ($null -ne $body) { $params.ContentType = "application/json"; $params.Body = ($body | ConvertTo-Json) }
    return Invoke-RestMethod @params
}

function Cast-Vote($token, $gameId, $sessionId, $targetUserId) {
    $resp = Api-Game $token POST "/api/games/$gameId/voting/vote" @{
        votingSessionId = $sessionId; targetUserId = $targetUserId
    }
    if (-not $resp.success) {
        throw "Vote failed: $($resp.message)"
    }
}

function Wait-ForPhase($token, $gameId, $expectedPhases, $timeoutSec = 60) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $state = Api-Game $token GET "/api/games/$gameId"
            $phase = $state.phase
            Write-Host "  phase=$phase"
            if ($expectedPhases -contains $phase) { return $state }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Timeout waiting for phases: $($expectedPhases -join ', ')"
}

Write-Host "`n=== TEST 1: Voting timeout (15s, no votes) ===" -ForegroundColor Cyan
$suffix = Get-Random -Maximum 99999
$p1 = "t1_$suffix"
Register-User $p1 | Out-Null
$t1 = Login-User "$p1@test.com"

$room = Api-Game $t1 POST "/api/game_rooms/create" @{
    name = "TimeoutTest$suffix"; maxPlayers = 5
}
$code = $room.roomCode
Write-Host "Room: $code"

for ($i = 2; $i -le 3; $i++) {
    $u = "t${i}_$suffix"
    Write-Host "Joining user $u..."
    Register-User $u | Out-Null
    $tk = Login-User "$u@test.com"
    Api-Game $tk POST "/api/game_rooms/join/$code" | Out-Null
    Write-Host "  joined OK"
}

$start = Api-Game $t1 POST "/api/games/start" @{
    roomCode = $code; mafiaCount = 1; discussionTimeSeconds = 30
}
$gameId = $start.gameId
Write-Host "Game started: $gameId, phase=$($start.phase)"

Write-Host "Waiting for phase advance after timeout (~30s, expect DAY_VOTE)..."
$state1 = Wait-ForPhase $t1 $gameId @("DAY_VOTE") 45
Write-Host "OK: timeout advanced game to $($state1.phase)" -ForegroundColor Green

Write-Host "`n=== TEST 2: Citizens win (Day vote eliminates MAFIA) ===" -ForegroundColor Cyan
$suffix2 = Get-Random -Maximum 99999
$users = @()
$tokens = @()
for ($i = 1; $i -le 4; $i++) {
    $u = "c${i}_$suffix2"
    Register-User $u | Out-Null
    $users += $u
    $tokens += (Login-User "$u@test.com")
}

$room2 = Api-Game $tokens[0] POST "/api/game_rooms/create" @{
    name = "CitizensWin$suffix2"; maxPlayers = 6; mafiaCount = 1; discussionTimeSeconds = 120
}
$code2 = $room2.roomCode
Write-Host "Room: $code2"

for ($i = 1; $i -le 3; $i++) {
    Api-Game $tokens[$i] POST "/api/game_rooms/join/$code2" | Out-Null
}

$start2 = Api-Game $tokens[0] POST "/api/games/start" @{
    roomCode = $code2; mafiaCount = 1; discussionTimeSeconds = 120
}
$gameId2 = $start2.gameId

# Find roles
$roles = @()
for ($i = 0; $i -lt 4; $i++) {
    $role = Api-Game $tokens[$i] GET "/api/games/rooms/$code2/me/role"
    $roles += $role
    Write-Host "  $($users[$i]): role=$($role.role)"
}

$mafiaIdx = ($roles | ForEach-Object { $_.role }).IndexOf("MAFIA")
$mafiaUserId = $roles[$mafiaIdx].userId
Write-Host "Mafia is player index $mafiaIdx (userId=$mafiaUserId)"

# Night vote: mafia kills a citizen
$session = Api-Game $tokens[$mafiaIdx] GET "/api/games/$gameId2/voting/current"
$citizenTarget = ($roles | Where-Object { $_.role -eq "CITIZEN" } | Select-Object -First 1).userId
Write-Host "Night vote: mafia votes for citizen $citizenTarget"
Cast-Vote $tokens[$mafiaIdx] $gameId2 $session.sessionId $citizenTarget

Write-Host "Waiting for DAY_VOTE..."
Wait-ForPhase $tokens[0] $gameId2 @("DAY_VOTE") 20 | Out-Null

$daySession = Api-Game $tokens[0] GET "/api/games/$gameId2/voting/current"
Write-Host "Day vote session: $($daySession.sessionId)"

# All alive players vote — citizens vote mafia, mafia votes a citizen
for ($i = 0; $i -lt 4; $i++) {
    $roleDto = Api-Game $tokens[$i] GET "/api/games/rooms/$code2/me/role"
    if ($roleDto.isAlive -eq $false) { continue }
    $target = if ($i -eq $mafiaIdx) { ($roles | Where-Object { $_.role -eq "CITIZEN" -and $_.userId -ne $citizenTarget } | Select-Object -First 1).userId } else { $mafiaUserId }
    if (-not $target) { $target = $citizenTarget }
    Write-Host "  $($users[$i]) votes for $target"
    Cast-Vote $tokens[$i] $gameId2 $daySession.sessionId $target
}

Write-Host "Waiting for GAME_OVER with CITIZENS win..."
$deadline = (Get-Date).AddSeconds(15)
$final = $null
while ((Get-Date) -lt $deadline) {
    $final = Api-Game $tokens[0] GET "/api/games/$gameId2"
    Write-Host "  phase=$($final.phase) status=$($final.status)"
    if ($final.phase -eq "GAME_OVER") { break }
    Start-Sleep -Seconds 1
}

$activeGame = Api-Game $tokens[0] GET "/api/games/rooms/$code2/active-game"
Write-Host "active-game: phase=$($activeGame.phase) winner=$($activeGame.winnerTeam) status=$($activeGame.status)"

if ($activeGame.winnerTeam -eq "CITIZENS" -and $activeGame.phase -eq "GAME_OVER") {
    Write-Host "OK: Citizens win path works!" -ForegroundColor Green
} else {
    Write-Host "FAIL: expected CITIZENS/GAME_OVER, got $($activeGame.winnerTeam)/$($activeGame.phase)" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== TEST 3: Multi-round (2 full cycles) ===" -ForegroundColor Cyan
$suffix3 = Get-Random -Maximum 99999
$tokens3 = @()
for ($i = 1; $i -le 4; $i++) {
    $u = "m${i}_$suffix3"
    Register-User $u | Out-Null
    $tokens3 += (Login-User "$u@test.com")
}

$room3 = Api-Game $tokens3[0] POST "/api/game_rooms/create" @{
    name = "MultiRound$suffix3"; maxPlayers = 6
}
$code3 = $room3.roomCode
for ($i = 1; $i -le 3; $i++) {
    Api-Game $tokens3[$i] POST "/api/game_rooms/join/$code3" | Out-Null
}

$start3 = Api-Game $tokens3[0] POST "/api/games/start" @{
    roomCode = $code3; mafiaCount = 1; discussionTimeSeconds = 30
}
$gameId3 = $start3.gameId

# Let 2 night+day cycles timeout (no votes) = ~40s minimum
Write-Host "Waiting through 2 timeout cycles (night+day x2, ~80s)..."
$seenPhases = @()
$deadline3 = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline3) {
    $s = Api-Game $tokens3[0] GET "/api/games/$gameId3"
    if ($s.status -eq "FINISHED") {
        Write-Host "Game ended early at phase $($s.phase)" -ForegroundColor Yellow
        break
    }
    if ($seenPhases.Count -eq 0 -or $seenPhases[-1] -ne $s.phase) {
        Write-Host "  phase=$($s.phase) day=$($s.dayNumber)"
        $seenPhases += $s.phase
    }
    if ($s.dayNumber -ge 2 -and $s.phase -eq "DAY_VOTE") {
        Write-Host "OK: Multi-round reached day 2 DAY_VOTE without crash!" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 3
}

if ($seenPhases -contains "DAY_VOTE" -and ($seenPhases | Where-Object { $_ -eq "NIGHT_VOTE" }).Count -ge 2) {
    Write-Host "OK: Multiple rounds completed" -ForegroundColor Green
} else {
    Write-Host "Phases seen: $($seenPhases -join ' -> ')" -ForegroundColor Yellow
}

Write-Host "`nAll tests completed." -ForegroundColor Cyan

# Test Health
Write-Host "=== Health Check ===" -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri 'http://localhost:8080/api/health'
$health | ConvertTo-Json -Depth 5

# Test SQL Generation
Write-Host "`n=== SQL Generation ===" -ForegroundColor Cyan
$body = '{"prompt":"查询formmain_1001中本月所有请假单"}'
$bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$sql = Invoke-RestMethod -Uri 'http://localhost:8080/api/sql/generate' -Method Post -ContentType 'application/json; charset=utf-8' -Body $bytes
$sql | ConvertTo-Json -Depth 5

# Test SQL History
Write-Host "`n=== SQL History ===" -ForegroundColor Cyan
$history = Invoke-RestMethod -Uri 'http://localhost:8080/api/sql/history?limit=5'
$history | ConvertTo-Json -Depth 5

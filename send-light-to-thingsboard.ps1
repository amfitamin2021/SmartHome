# Скрипт для отправки данных с люстры в ThingsBoard

# Токен устройства из ThingsBoard
$thingsboardToken = "light-living-room-token"

# Базовый URL API
$smarthomeUrl = "http://localhost:8086/api"
$thingsboardUrl = "http://localhost:9090/api/v1"

# ID люстры из вашей системы
$lightId = "4d4f800c-6242-429a-a1e9-55233685446b"

# 1. Получаем данные с люстры из вашего API
Write-Host "Getting light data from Smarthome API..."
try {
    $response = Invoke-RestMethod -Method Get -Uri "$smarthomeUrl/devices/$lightId"
    Write-Host "Data received successfully"
    
    # Выводим текущие свойства люстры
    Write-Host "Current light properties:"
    Write-Host "Power: $($response.properties.power)"
    Write-Host "Brightness: $($response.properties.brightness)"
    Write-Host "Color: $($response.properties.color)"
    
} catch {
    Write-Host "Error getting data: $_"
    exit
}

# 2. Подготавливаем данные для отправки в ThingsBoard
$telemetry = @{
    power = $response.properties.power
    brightness = [int]$response.properties.brightness
    color = $response.properties.color
    status = $response.status
}

# 3. Отправляем данные в ThingsBoard
$body = $telemetry | ConvertTo-Json
$url = "$thingsboardUrl/$thingsboardToken/telemetry"

Write-Host "Sending data to ThingsBoard..."
try {
    Invoke-RestMethod -Method Post -Uri $url -ContentType "application/json" -Body $body
    Write-Host "Data sent successfully to ThingsBoard"
} catch {
    Write-Host "Error sending data to ThingsBoard: $_"
}

# Выводим справочную информацию
Write-Host ""
Write-Host "Helpful information:"
Write-Host "1. To see updates on your ThingsBoard dashboard, make sure you've created widgets for 'power', 'brightness', and 'color'"
Write-Host "2. If data is not displayed, check that property names in ThingsBoard match those used in the script"
Write-Host "3. You can run this script after changing the light state through Smarthome API" 
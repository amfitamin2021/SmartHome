# Скрипт "Вечерний режим" - устанавливает комфортные настройки для вечера

# Базовый URL API
$baseUrl = "http://localhost:8086/api"

# IDs устройств
$livingRoomLightId = "4d4f800c-6242-429a-a1e9-55233685446b"  # Люстра в гостиной
$bedroomLightId = "81e03a6b-e9f8-44df-8c01-4a61f57becee"     # Ночник в спальне
$livingRoomThermostatId = "2d75ed09-5bee-46a7-9549-d58066fdd4a0"  # Термостат в гостиной
$bedroomThermostatId = "9818f47f-cb45-4a34-b699-df6dd289afc8"  # Термостат в спальне
$tvSocketId = "b9931898-082e-4342-8f61-908c91f61b59"  # Умная розетка ТВ

# 1. Приглушаем свет в гостиной
Write-Host "Настраиваем освещение в гостиной..."
$lightParams = @{
    command = "setState"
    parameters = @{
        power = "on"
        brightness = "30"
        color = "FF9900"  # Теплый желтый свет
    }
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/devices/$livingRoomLightId/command" -ContentType "application/json" -Body $lightParams

# 2. Включаем ночник в спальне
Write-Host "Включаем ночник в спальне..."
$nightLightParams = @{
    command = "setState"
    parameters = @{
        power = "on"
        brightness = "20"
        color = "FF6A00"  # Очень теплый оранжевый свет
    }
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/devices/$bedroomLightId/command" -ContentType "application/json" -Body $nightLightParams

# 3. Устанавливаем комфортную температуру в гостиной
Write-Host "Настраиваем термостат в гостиной..."
$livingRoomThermostatParams = @{
    command = "setState"
    parameters = @{
        power = "on"
        mode = "heat"
        target_temperature = "22.0"
    }
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/devices/$livingRoomThermostatId/command" -ContentType "application/json" -Body $livingRoomThermostatParams

# 4. Устанавливаем комфортную температуру в спальне для сна
Write-Host "Настраиваем термостат в спальне..."
$bedroomThermostatParams = @{
    command = "setState"
    parameters = @{
        power = "on"
        mode = "heat"
        target_temperature = "20.0"  # Прохладнее для комфортного сна
    }
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/devices/$bedroomThermostatId/command" -ContentType "application/json" -Body $bedroomThermostatParams

# 5. Включаем ТВ
Write-Host "Включаем умную розетку ТВ..."
$tvSocketParams = @{
    command = "setState"
    parameters = @{
        state = "on"
        power_consumption = "120"
    }
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/devices/$tvSocketId/command" -ContentType "application/json" -Body $tvSocketParams

Write-Host "Вечерний режим активирован!"

# Отправляем обновленные данные в ThingsBoard
Write-Host "Запускаем синхронизацию с ThingsBoard..."
& .\send-to-thingsboard.ps1

Write-Host "Готово! Приятного вечера!" 
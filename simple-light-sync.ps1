# Simple script to sync light data to ThingsBoard

# Device token from ThingsBoard
$token = "light-living-room-token"

# API URLs
$api_url = "http://localhost:8086/api/devices/4d4f800c-6242-429a-a1e9-55233685446b"
$tb_url = "http://localhost:9090/api/v1/light-living-room-token/telemetry"

# Get device data
Write-Host "Getting device data..."
$device_data = Invoke-RestMethod -Method Get -Uri $api_url

# Extract properties
$power = $device_data.properties.power
$brightness = $device_data.properties.brightness
$color = $device_data.properties.color
$status = $device_data.status

Write-Host "Power: $power"
Write-Host "Brightness: $brightness"
Write-Host "Color: $color"
Write-Host "Status: $status"

# Prepare telemetry data
$telemetry = @{
    power = $power
    brightness = [int]$brightness
    color = $color
    status = $status
}

# Convert to JSON
$json_data = $telemetry | ConvertTo-Json

# Send to ThingsBoard
Write-Host "Sending to ThingsBoard..."
Invoke-RestMethod -Method Post -Uri $tb_url -ContentType "application/json" -Body $json_data

Write-Host "Done!" 
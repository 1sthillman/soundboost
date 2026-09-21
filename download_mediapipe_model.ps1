# Download MediaPipe Hand Landmarker Model
$url = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
$output = "app/src/main/assets/hand_landmarker.task"

Write-Host "Downloading MediaPipe Hand Landmarker model..." -ForegroundColor Green
Invoke-WebRequest -Uri $url -OutFile $output
Write-Host "Download complete!" -ForegroundColor Green
Write-Host "Model saved to: $output" -ForegroundColor Cyan

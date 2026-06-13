$env:ANDROID_HOME = "C:/Users/vichu/AppData/Local/Android/Sdk"
$env:ANDROID_SDK_ROOT = "C:/Users/vichu/AppData/Local/Android/Sdk"
$env:PATH = "$env:ANDROID_HOME/platform-tools;$env:ANDROID_HOME/emulator;$env:PATH"
$env:TS_NODE_SKIP_PROJECT = "true"

Write-Host "Starting Appium Server..." -ForegroundColor Yellow
$appiumProcess = Start-Process -FilePath "appium.cmd" -ArgumentList "--address 127.0.0.1 --port 4723" -PassThru -NoNewWindow
Start-Sleep -Seconds 8

Write-Host "Starting PayBuddy Audit & Smoke Test Suite..." -ForegroundColor Cyan
npx wdio run ./wdio.conf.js --spec ./tests/PayBuddySmokeSuite.test.js

Write-Host "Cleaning up Appium Server..." -ForegroundColor Yellow
if ($appiumProcess) { Stop-Process -Id $appiumProcess.Id -Force }

param([Parameter(Mandatory = $true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskApk = Join-Path $taskRoot 'dist\RuleApp-0.9_beta-debug.apk'
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (!(Test-Path -LiteralPath $taskApk)) { throw 'Falta el APK en dist. Compila o usa app/build/outputs/apk/debug/app-debug.apk.' }
if (!(Test-Path -LiteralPath $taskAdb)) { throw 'No se encontrÃ³ ADB en el SDK del usuario.' }
& $taskAdb -s $Serial get-state
if ($LASTEXITCODE -ne 0) { throw 'El dispositivo debe estar conectado y autorizado en la pantalla del telÃ©fono.' }
& $taskAdb -s $Serial install -r $taskApk
if ($LASTEXITCODE -ne 0) { throw 'FallÃ³ la instalaciÃ³n. Si la firma anterior es distinta, no desinstales sin respaldar tus datos.' }
& $taskAdb -s $Serial shell am start -n com.leninasto.ruleapp/com.leninasto.ruleapp.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'El APK se instalÃ³, pero no se pudo abrir la app.' }

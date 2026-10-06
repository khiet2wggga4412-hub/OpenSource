param(
    [switch] $WhatIf,
    [switch] $Rerun,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs = @(':app:assembleDebug')
)

$ErrorActionPreference = 'Stop'

$workspace = $PSScriptRoot
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
    elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
    else { 'D:\AndroidSDK' }
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'E:\Dev\jdk-21' }
$unixSocketTemp = Join-Path $env:TEMP 'gradle-tmp-clean'
$localGradle = 'E:\Dev\gradle-dists\gradle-9.7.1\bin\gradle.bat'
$gradleBat = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { Join-Path $workspace 'gradlew.bat' }

if (-not (Test-Path -LiteralPath $gradleBat)) { throw "Gradle launcher not found: $gradleBat" }
if (-not (Test-Path -LiteralPath $javaHome)) { throw "Java home not found: $javaHome" }
if (-not (Test-Path -LiteralPath $androidHome)) { throw "Android SDK not found: $androidHome" }

$sdkDirValue = 'sdk.dir=' + ($androidHome -replace '\\', '\\').Replace(':', '\:')
Set-Content -LiteralPath (Join-Path $workspace 'local.properties') -Value $sdkDirValue -Encoding ascii

if ($WhatIf) {
    "GRADLE_USER_HOME = $gradleUserHome"
    "JAVA_HOME        = $javaHome"
    "ANDROID_HOME     = $androidHome"
    "gradle launcher  = $gradleBat"
    "local.properties = $sdkDirValue"
    "args             = $($GradleArgs -join ' ')"
    exit 0
}

New-Item -ItemType Directory -Force -Path $unixSocketTemp | Out-Null

$env:GRADLE_USER_HOME = $gradleUserHome
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$unixSocketTemp"
$env:Path = "$javaHome\bin;$env:Path"

if ($Rerun) { $GradleArgs = $GradleArgs + @('--rerun-tasks') }

& $gradleBat -p $workspace @GradleArgs --console=plain
exit $LASTEXITCODE

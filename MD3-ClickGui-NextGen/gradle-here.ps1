# Portable build entry point for this machine.
# gradle-local.ps1 above pins tool paths from another environment (E:\DevEnv, .trae-cn JDK17),
# which no longer exist here. This script resolves what is actually installed:
#   JDK 21   = E:\Dev\jdk-21 (or $env:JAVA_HOME)
#   SDK      = D:\AndroidSDK (or $env:ANDROID_HOME) and keeps local.properties in sync
#   Gradle   = the extracted gradle-9.7.1 under E:\Dev\gradle-dists when present, else the wrapper
#
# Note: on this machine Gradle's incremental up-to-date checks can miss freshly edited sources
# (a file written at 11:05 was still reported UP-TO-DATE at 11:05:19). Pass -Rerun to force a
# real compile before trusting a "BUILD SUCCESSFUL".
#
# Usage:  ./gradle-here.ps1                              # :app:assembleDebug
#         ./gradle-here.ps1 -Rerun                       # force recompile, then assembleDebug
#         ./gradle-here.ps1 :app:assembleRelease
#         ./gradle-here.ps1 -WhatIf                      # print resolved paths only
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
# The wrapper cannot download its distribution here (services.gradle.org is unreachable from the
# JVM; the mirrors are reachable, so the distribution was fetched from one into this folder).
$localGradle = 'E:\Dev\gradle-dists\gradle-9.7.1\bin\gradle.bat'
$gradleBat = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { Join-Path $workspace 'gradlew.bat' }

if (-not (Test-Path -LiteralPath $gradleBat)) { throw "Gradle launcher not found: $gradleBat" }
if (-not (Test-Path -LiteralPath $javaHome)) { throw "Java home not found: $javaHome" }
if (-not (Test-Path -LiteralPath $androidHome)) { throw "Android SDK not found: $androidHome" }

# Keep local.properties pointing at the SDK that actually exists on this machine.
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

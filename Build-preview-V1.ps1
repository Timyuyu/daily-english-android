$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$taskTemp = Join-Path $PSScriptRoot 'work\java-temp'
New-Item -ItemType Directory -Force -Path $taskTemp | Out-Null
$taskFso = New-Object -ComObject Scripting.FileSystemObject
$taskShortTemp = $taskFso.GetFolder($taskTemp).ShortPath
$taskPreviousOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = "$taskPreviousOptions -Djdk.net.unixdomain.tmpdir=$taskShortTemp -Djava.io.tmpdir=$taskShortTemp".Trim()
    & '.\gradlew.bat' --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Build or verification failed. See the output above.' }
} finally {
    $env:JAVA_TOOL_OPTIONS = $taskPreviousOptions
}

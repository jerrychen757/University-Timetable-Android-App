# Run from any directory. Requires Windows PowerShell 5.1+ and a JDK.
[CmdletBinding()]
param(
    [string]$JavaHome,
    [switch]$Device
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$exitCode = 1
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    elseif (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
        $studioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
        if (Test-Path (Join-Path $studioJdk 'bin\java.exe')) { $env:JAVA_HOME = $studioJdk }
        else { throw 'JDK unavailable. Supply -JavaHome or configure JAVA_HOME/PATH.' }
    }
    if ($env:JAVA_HOME -and -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        throw 'JAVA_HOME does not contain bin\java.exe.'
    }
    # Confirmed against this project with gradlew tasks --all.
    $tasks = @(':app:assembleDebug', ':app:compileDebugKotlin', ':app:compileDebugJavaWithJavac', ':app:lintDebug', ':app:testDebugUnitTest')
    if ($Device) { $tasks += ':app:connectedDebugAndroidTest' }
    Write-Host ('Required checks: ' + ($tasks -join ', '))
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'gradlew.bat') @tasks --continue --console=plain
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) { Write-Host ('Verification FAILED. Gradle exit code: ' + $exitCode) -ForegroundColor Red }
        else {
            Write-Host 'Build, compilation, lint and unit-test gate passed.' -ForegroundColor Green
            if ($Device) { Write-Host 'Connected instrumentation task passed; inspect test coverage before claiming UI behavior verified.' }
            else { Write-Host 'Runtime and UI behavior were not checked by this command.' }
        }
    } finally { Pop-Location }
} catch {
    Write-Host ('Verification FAILED: ' + $_.Exception.Message) -ForegroundColor Red
    $exitCode = 1
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
exit $exitCode

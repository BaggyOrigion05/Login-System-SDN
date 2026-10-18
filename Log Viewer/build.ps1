param([switch]$Test, [switch]$Run)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$jdk = $env:JAVA_HOME
if (-not $jdk) {
    $compiler = Get-Command javac -ErrorAction SilentlyContinue
    if ($compiler) { $jdk = Split-Path (Split-Path $compiler.Source) }
}
if (-not $jdk -and (Test-Path 'C:/Program Files/Java/jdk1.8.0_102/bin/javac.exe')) {
    $jdk = 'C:/Program Files/Java/jdk1.8.0_102'
}
if (-not $jdk) { throw 'Install JDK 8 or later and set JAVA_HOME to its folder.' }
$javac = Join-Path $jdk 'bin/javac.exe'
$java = Join-Path $jdk 'bin/java.exe'
New-Item -ItemType Directory -Force -Path build/classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 -Xlint:all -d build/classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
Write-Host 'Compilation successful.'
if ($Test) {
    $tests = @(Get-ChildItem -LiteralPath test -Recurse -Filter *.java | ForEach-Object { $_.FullName })
    & $javac -encoding UTF-8 -cp build/classes -d build/classes @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & $java '-Djava.awt.headless=true' -cp build/classes logviewer.LogViewerTest
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
}
if ($Run) { & $java -cp build/classes logviewer.ui.LogViewerApp }

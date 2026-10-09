$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Path $toolsRoot -Force | Out-Null

function Install-ApacheZip {
    param(
        [string]$Name,
        [string]$DownloadUrl,
        [string]$ChecksumUrl,
        [string]$InstallPath,
        [string]$ArchiveRoot
    )

    $requiredFile = if ($Name -like '*maven*') { Join-Path $InstallPath 'bin\mvn.cmd' } else { Join-Path $InstallPath 'bin\startup.bat' }
    if (Test-Path $requiredFile) {
        Write-Host "$Name is already set up."
        return
    }

    $archivePath = Join-Path $toolsRoot "$Name.zip"
    $extractPath = Join-Path $toolsRoot "$Name-extracted"
    Write-Host "Downloading $Name from the Apache release mirror..."
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $archivePath -UseBasicParsing
    $checksumText = (Invoke-WebRequest -Uri $ChecksumUrl -UseBasicParsing).Content
    $expected = [regex]::Match($checksumText, '(?i)\b[0-9a-f]{128}\b').Value
    if (-not $expected) { throw "Could not read the official SHA-512 checksum for $Name." }
    $actual = (Get-FileHash -Path $archivePath -Algorithm SHA512).Hash
    if ($actual -ne $expected.ToUpperInvariant()) {
        Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
        throw "The $Name download failed its SHA-512 check; it was not installed."
    }

    Remove-Item -LiteralPath $extractPath -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive -LiteralPath $archivePath -DestinationPath $extractPath -Force
    $sourcePath = Join-Path $extractPath $ArchiveRoot
    if (-not (Test-Path $sourcePath)) { throw "The downloaded $Name archive did not contain the expected folder." }
    Remove-Item -LiteralPath $InstallPath -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item -LiteralPath $sourcePath -Destination $InstallPath
    Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $extractPath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "$Name is ready."
}

function Install-TemurinJdk {
    $installPath = Join-Path $toolsRoot 'jdk-21'
    if (Test-Path (Join-Path $installPath 'bin\javac.exe')) {
        Write-Host 'Java 21 is already set up.'
        return
    }

    $metadataUrl = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    Write-Host 'Finding the current Eclipse Temurin Java 21 release...'
    $assets = Invoke-RestMethod -Uri $metadataUrl
    $package = $assets[0].binary.package
    if (-not $package.link -or -not $package.checksum) { throw 'Could not find the Java 21 download and checksum.' }

    $archivePath = Join-Path $toolsRoot 'temurin-jdk-21.zip'
    $extractPath = Join-Path $toolsRoot 'temurin-jdk-21-extracted'
    Write-Host 'Downloading Eclipse Temurin Java 21...'
    Invoke-WebRequest -Uri $package.link -OutFile $archivePath -UseBasicParsing
    $actual = (Get-FileHash -Path $archivePath -Algorithm SHA256).Hash
    if ($actual -ne $package.checksum.ToUpperInvariant()) {
        Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
        throw 'The Java download failed its SHA-256 check; it was not installed.'
    }

    Remove-Item -LiteralPath $extractPath -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive -LiteralPath $archivePath -DestinationPath $extractPath -Force
    $jdkFolder = Get-ChildItem -Path $extractPath -Directory -Recurse |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } |
        Select-Object -First 1
    if (-not $jdkFolder) { throw 'The downloaded Java archive did not contain a JDK.' }
    Remove-Item -LiteralPath $installPath -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item -LiteralPath $jdkFolder.FullName -Destination $installPath
    Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $extractPath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host 'Java 21 is ready.'
}

try {
    Install-TemurinJdk
    Install-ApacheZip `
        -Name 'apache-maven-3.10.0' `
        -DownloadUrl 'https://dlcdn.apache.org/maven/maven-3/3.10.0/binaries/apache-maven-3.10.0-bin.zip' `
        -ChecksumUrl 'https://dlcdn.apache.org/maven/maven-3/3.10.0/binaries/apache-maven-3.10.0-bin.zip.sha512' `
        -InstallPath (Join-Path $toolsRoot 'apache-maven-3.10.0') `
        -ArchiveRoot 'apache-maven-3.10.0'
    Install-ApacheZip `
        -Name 'apache-tomcat-10.1.60' `
        -DownloadUrl 'https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60-windows-x64.zip' `
        -ChecksumUrl 'https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60-windows-x64.zip.sha512' `
        -InstallPath (Join-Path $toolsRoot 'apache-tomcat-10.1.60') `
        -ArchiveRoot 'apache-tomcat-10.1.60'
} catch {
    Write-Error "DataHive setup failed: $($_.Exception.Message)"
    exit 1
}

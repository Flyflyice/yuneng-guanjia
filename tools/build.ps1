# 驭能管家：无 Gradle 手工构建脚本（Windows PowerShell）
# 依赖：JDK 21、Android SDK build-tools 34.0.0、Python 3（用于 dex 写入与源码打包）
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$sdk = 'D:\Android\Sdk'
$buildTools = Join-Path $sdk 'build-tools\34.0.0'
$androidJar = Join-Path $sdk 'platforms\android-34\android.jar'
$jdk = 'C:\Program Files\Java\jdk-21'
$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
$keystore = Join-Path $root 'tools\debug.keystore'
$versionName = '1.2.0'
$versionCode = 13

foreach ($p in @($buildTools, $androidJar, $jdk, $keystore)) {
    if (-not (Test-Path $p)) { throw "缺少构建依赖：$p" }
}

Write-Host '==> 清理 build 目录' -ForegroundColor Cyan
foreach ($dir in @('classes', 'gen', 'dex', 'test-classes')) {
    $target = Join-Path $build $dir
    if (Test-Path $target) { Remove-Item $target -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path $build, $dist | Out-Null

Write-Host '==> 编译资源（aapt2 compile）' -ForegroundColor Cyan
& (Join-Path $buildTools 'aapt2.exe') compile --dir (Join-Path $root 'app\src\main\res') -o (Join-Path $build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile 失败' }

Write-Host '==> 链接资源与清单（aapt2 link）' -ForegroundColor Cyan
& (Join-Path $buildTools 'aapt2.exe') link `
    -o (Join-Path $build 'unsigned.apk') `
    -I $androidJar `
    --manifest (Join-Path $root 'app\src\main\AndroidManifest.xml') `
    --java (Join-Path $build 'gen') `
    --min-sdk-version 24 --target-sdk-version 34 `
    --version-code $versionCode --version-name $versionName `
    (Join-Path $build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link 失败' }

Write-Host '==> 编译 Java（javac）' -ForegroundColor Cyan
$sources = @(Get-ChildItem (Join-Path $root 'app\src\main\java') -Recurse -Filter *.java | ForEach-Object FullName)
$sources += @(Get-ChildItem (Join-Path $build 'gen') -Recurse -Filter *.java | ForEach-Object FullName)
New-Item -ItemType Directory -Force -Path (Join-Path $build 'classes') | Out-Null
& (Join-Path $jdk 'bin\javac.exe') -encoding UTF-8 -source 8 -target 8 -parameters `
    -classpath $androidJar -d (Join-Path $build 'classes') $sources
if ($LASTEXITCODE -ne 0) { throw 'javac 失败' }

Write-Host '==> 打包 class 并转为 dex（jar + d8）' -ForegroundColor Cyan
$jarFile = Join-Path $build ('app-' + (Get-Date -Format 'HHmmssfff') + '.jar')
& (Join-Path $jdk 'bin\jar.exe') cf $jarFile -C (Join-Path $build 'classes') .
if ($LASTEXITCODE -ne 0) { throw 'jar 失败' }
New-Item -ItemType Directory -Force -Path (Join-Path $build 'dex') | Out-Null
& (Join-Path $buildTools 'd8.bat') --release --lib $androidJar --min-api 24 `
    --output (Join-Path $build 'dex') $jarFile
if ($LASTEXITCODE -ne 0) { throw 'd8 失败' }

Write-Host '==> 将 classes.dex 写入 APK' -ForegroundColor Cyan
$withDex = Join-Path $build 'withdex.apk'
Copy-Item (Join-Path $build 'unsigned.apk') $withDex -Force
python (Join-Path $PSScriptRoot 'add_dex.py') (Join-Path $build 'dex\classes.dex') $withDex
if ($LASTEXITCODE -ne 0) { throw '写入 classes.dex 失败' }

Write-Host '==> 对齐与签名（zipalign + apksigner）' -ForegroundColor Cyan
$aligned = Join-Path $build 'aligned.apk'
& (Join-Path $buildTools 'zipalign.exe') -f 4 $withDex $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign 失败' }
$final = Join-Path $dist "驭能管家-$versionName.apk"
& (Join-Path $buildTools 'apksigner.bat') sign `
    --ks $keystore --ks-key-alias androiddebugkey `
    --ks-pass pass:android --key-pass pass:android `
    --out $final $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner 失败' }

Write-Host '==> 运行 JVM 统计测试' -ForegroundColor Cyan
$testClasses = Join-Path $build 'test-classes'
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
$testSources = @(
    'app\src\main\java\com\yuneng\guanjia\model\Vehicle.java',
    'app\src\main\java\com\yuneng\guanjia\model\EnergyRecord.java',
    'app\src\main\java\com\yuneng\guanjia\model\FuelTypes.java',
    'app\src\main\java\com\yuneng\guanjia\model\Stats.java',
    'app\src\main\java\com\yuneng\guanjia\model\StatsCalculator.java',
    'app\src\main\java\com\yuneng\guanjia\model\TimeRange.java',
    'app\src\main\java\com\yuneng\guanjia\model\TripSlice.java',
    'app\src\main\java\com\yuneng\guanjia\data\DemoData.java',
    'tools\TestStats.java',
    'tools\TestTrip.java'
) | ForEach-Object { Join-Path $root $_ }
& (Join-Path $jdk 'bin\javac.exe') -encoding UTF-8 -d $testClasses $testSources
if ($LASTEXITCODE -ne 0) { throw '测试编译失败' }
& (Join-Path $jdk 'bin\java.exe') '-Dfile.encoding=UTF-8' -cp $testClasses TestStats
if ($LASTEXITCODE -ne 0) { throw '统计测试未通过' }
& (Join-Path $jdk 'bin\java.exe') '-Dfile.encoding=UTF-8' -cp $testClasses TestTrip
if ($LASTEXITCODE -ne 0) { throw '行程片段测试未通过' }

Write-Host '==> 打包源码 zip' -ForegroundColor Cyan
python (Join-Path $PSScriptRoot 'make_source_zip.py')
if ($LASTEXITCODE -ne 0) { throw '源码打包失败' }

$size = (Get-Item $final).Length
Write-Host "==> 构建完成：$final（$size 字节）" -ForegroundColor Green
exit 0

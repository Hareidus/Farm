Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead('C:\Users\pc\.gradle\caches\modules-2\files-2.1\io.izzel.taboolib\bukkit-hook\6.2.4-1645904\9ff13b36b2a5e4ca29b40dffc553d57dd307ea89\bukkit-hook-6.2.4-1645904.jar')
$entries = @('taboolib/platform/compat/VaultService.class', 'taboolib/platform/compat/VaultKt.class', 'taboolib/platform/compat/EconomyResponse.class')
foreach ($name in $entries) {
    $entry = $jar.GetEntry($name)
    if ($entry) {
        $stream = $entry.Open()
        $ms = New-Object System.IO.MemoryStream
        $stream.CopyTo($ms)
        $bytes = $ms.ToArray()
        $text = [System.Text.Encoding]::UTF8.GetString($bytes)
        Write-Output "=== $name ==="
        # Extract string constants which reveal method names
        $strings = [System.Text.RegularExpressions.Regex]::Matches($text, '[\x20-\x7E]{4,}')
        foreach ($m in $strings) { Write-Output $m.Value }
        Write-Output ""
        $stream.Dispose()
        $ms.Dispose()
    }
}
$jar.Dispose()

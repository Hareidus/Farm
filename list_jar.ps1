Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead('C:\Users\pc\.gradle\caches\modules-2\files-2.1\io.izzel.taboolib\bukkit-hook\6.2.4-1645904\9ff13b36b2a5e4ca29b40dffc553d57dd307ea89\bukkit-hook-6.2.4-1645904.jar')
foreach ($entry in $jar.Entries) {
    if ($entry.FullName -like '*.class') {
        Write-Output $entry.FullName
    }
}
$jar.Dispose()

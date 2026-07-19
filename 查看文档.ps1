$ErrorActionPreference = "Stop"

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║           📚 RAG 智能问答系统 - 文档查看器                    ║" -ForegroundColor White
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

$projectPath = "D:\develop\code\JAVA_CODE\RAG_Q&A_sys"
$mdFiles = Get-ChildItem $projectPath -Recurse -Include "*.md" | Sort-Object FullName

if ($mdFiles.Count -eq 0) {
    Write-Host "`n未找到 Markdown 文件！" -ForegroundColor Red
    exit 1
}

Write-Host "`n找到 $($mdFiles.Count) 个 Markdown 文档：`n" -ForegroundColor Green

$index = 1
$fileMap = @{}

foreach ($file in $mdFiles) {
    $name = $file.Name
    $size = [math]::Round($file.Length / 1KB, 2)
    $relativePath = $file.FullName.Replace($projectPath, "")

    Write-Host "  [$index] $name ($size KB)" -ForegroundColor White
    Write-Host "       → $relativePath" -ForegroundColor DarkGray

    $fileMap[$index] = $file.FullName
    $index++
}

Write-Host "`n╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor DarkGray

$choice = Read-Host "选择要查看的文档编号 (1-$($mdFiles.Count))"

if (-not $fileMap.ContainsKey([int]$choice)) {
    Write-Host "`n❌ 无效的选项！" -ForegroundColor Red
    exit 1
}

$selectedFile = $fileMap[[int]$choice]
$content = Get-Content $selectedFile -Raw -Encoding UTF8
$name = Split-Path $selectedFile -Leaf

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor DarkCyan
Write-Host "  📄 $name" -ForegroundColor White
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor DarkCyan

# 简单的 Markdown 渲染
$lines = $content -split "`n"

foreach ($line in $lines) {
    if ($line -match "^# (.+)$") {
        Write-Host "`n╔═══════ $($Matches[1]) ═══════╗" -ForegroundColor Yellow
    }
    elseif ($line -match "^## (.+)$") {
        Write-Host "`n═══ $($Matches[1]) ════" -ForegroundColor Cyan
    }
    elseif ($line -match "^### (.+)$") {
        Write-Host "`n▸ $($Matches[1])" -ForegroundColor Green
    }
    elseif ($line -match "^- (.+)$") {
        Write-Host "  • $($Matches[1])" -ForegroundColor White
    }
    elseif ($line -match "^\d+\. (.+)$") {
        Write-Host "  $($Matches[0])" -ForegroundColor White
    }
    elseif ($line -match "^> (.+)$") {
        Write-Host "  › $($Matches[1])" -ForegroundColor Gray
    }
    elseif ($line -match "^\`\`\`(.*)$") {
        Write-Host "  [代码块]" -ForegroundColor DarkGray
    }
    elseif ($line -match "^\`\`$") {
        Write-Host "  [代码块结束]" -ForegroundColor DarkGray
    }
    elseif ($line -match "^\|(.+)\|$") {
        Write-Host "  $($Matches[0])" -ForegroundColor DarkGray
    }
    elseif ($line.Trim() -eq "") {
        Write-Host ""
    }
    elseif ($line -match "^\-{3,}$") {
        Write-Host "───────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    }
    else {
        Write-Host "  $line" -ForegroundColor White
    }
}

Write-Host "`n╚════════════════════════════════════════════════════════════════╝" -ForegroundColor DarkGray
Write-Host "`n按任意键返回菜单..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# 递归调用显示菜单
& $MyInvocation.MyCommand.Path
$content = Get-Content app\src\main\java\com\example\novapass\MainActivity.kt

$imports = $content[2..100] -join "`r`n"
$baseImports = $imports + "`r`nimport com.example.novapass.models.*`r`nimport com.example.novapass.ui.components.*`r`nimport com.example.novapass.ui.screens.*`r`nimport com.example.novapass.navigation.*`r`nimport com.example.novapass.TicketViewModel`r`nimport com.example.novapass.R`r`nimport com.example.novapass.NovaPassApp`r`n"

# Create directories
New-Item -ItemType Directory -Force -Path app\src\main\java\com\example\novapass\models
New-Item -ItemType Directory -Force -Path app\src\main\java\com\example\novapass\ui\components
New-Item -ItemType Directory -Force -Path app\src\main\java\com\example\novapass\ui\screens
New-Item -ItemType Directory -Force -Path app\src\main\java\com\example\novapass\navigation

# 1. models/ExtractedTicketData.kt
$mContent = "package com.example.novapass.models`r`n" + ($content[100..110] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\models\ExtractedTicketData.kt -Value $mContent

# 2. navigation/NovaPassApp.kt
$nContent = "package com.example.novapass.navigation`r`n" + $baseImports + "`r`n" + ($content[132..165] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\navigation\NovaPassApp.kt -Value $nContent

# 3. ui/screens/TicketListScreen.kt
$tContent = "package com.example.novapass.ui.screens`r`n" + $baseImports + "`r`n" + ($content[167..1030] -join "`r`n") + "`r`n" + ($content[1498..1631] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\ui\screens\TicketListScreen.kt -Value $tContent

# 4. ui/screens/PdfViewerScreen.kt
$pContent = "package com.example.novapass.ui.screens`r`n" + $baseImports + "`r`n" + ($content[1357..1496] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\ui\screens\PdfViewerScreen.kt -Value $pContent

# 5. ui/components/TicketItem.kt (includes GlassCard and EmptyStateView)
$tiContent = "package com.example.novapass.ui.components`r`n" + $baseImports + "`r`n" + ($content[1032..1054] -join "`r`n") + "`r`n" + ($content[1119..1356] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\ui\components\TicketItem.kt -Value $tiContent

# 6. ui/components/CustomInputField.kt
$cContent = "package com.example.novapass.ui.components`r`n" + $baseImports + "`r`n" + ($content[1056..1118] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\ui\components\CustomInputField.kt -Value $cContent

# 7. MainActivity.kt (clean)
$maContent = "package com.example.novapass`r`n" + $baseImports + "`r`n" + ($content[112..130] -join "`r`n")
Set-Content -Path app\src\main\java\com\example\novapass\MainActivity.kt -Value $maContent

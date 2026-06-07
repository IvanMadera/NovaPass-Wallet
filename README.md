# NovaPass Wallet

NovaPass Wallet es una app nativa para Android que organiza boletos digitales en PDF dentro de una experiencia visual sobria, premium y enfocada en uso real: encontrar el boleto rapido, abrirlo con brillo alto, escanear el QR y seguir con el evento.

Version actual: `2026.06.07`

## Caracteristicas

- Importacion de boletos PDF mediante Storage Access Framework.
- Deteccion basica de archivos validos por contenido del PDF.
- Extraccion de datos del boleto: nombre del evento, fecha, hora, seccion, fila, asiento y pagina.
- Soporte para PDFs con multiples boletos, con edicion individual antes de guardar.
- Lista principal con busqueda, orden por fecha y tarjetas de boleto con estilo fisico.
- Archivo de boletos: archivar, desarchivar y alternar entre wallet y archivo.
- Visor PDF seguro con brillo maximo temporal y soporte para pinch-to-zoom.
- Persistencia local con Room y flujos reactivos con StateFlow.
- Prevencion de duplicados por datos del boleto o por URI/pagina.
- Easter egg con informacion del desarrollador.

## Diseno

La interfaz usa un sistema visual oscuro en verde profundo con acentos dorados. Los tickets, inputs, barra de busqueda, modales y acciones principales comparten:

- fondos opacos en verde oscuro,
- bordes dorados sutiles,
- elevacion ligera,
- esquinas consistentes,
- jerarquia visual clara entre contenedores e inputs.

La intencion es que la app se sienta premium sin depender de efectos glass pesados ni decoracion innecesaria.

## Arquitectura

NovaPass sigue una estructura MVVM con separacion por capas:

- `feature/tickets/ui`: pantallas, bottom sheets, dialogs y componentes Compose.
- `feature/tickets/presentation`: `TicketViewModel` y estado de UI.
- `domain`: modelos, repositorios y casos de uso.
- `data`: Room, mappers, repositorios e integracion con PDFBox.
- `core/design`: tema, colores, spacing y componentes reutilizables.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room
- Coroutines + StateFlow
- Android PdfRenderer
- PdfBox Android
- Gradle Version Catalog
- R8 / resource shrinking en release

## Seguridad y robustez

- Backups del sistema desactivados para evitar respaldar datos locales de tickets.
- Reglas de backup/data extraction excluyen database, shared preferences y files.
- Permisos de URI persistentes para acceder a PDFs seleccionados por el usuario.
- Manejo controlado de errores al importar y guardar boletos.
- Recursos PDF cerrados de forma segura.
- Build release minificado y con recursos reducidos.
- Artefactos generados como APKs, logs y ZIPs locales quedan fuera de Git.

## Requisitos

- Min SDK: 31
- Target SDK: 35
- Compile SDK: 36
- Android Studio reciente con soporte para Compose
- Dispositivo fisico recomendado para validar brillo, PDF y escaneo de QR

## Desarrollo

Compilar debug:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Generar APK release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Para instalar una version release en un dispositivo, genera un APK firmado desde Android Studio:

`Build > Generate Signed App Bundle / APK > APK`

El keystore local no debe subirse al repositorio.

## Notas de Git

No se deben versionar:

- APKs o bundles generados,
- logs,
- ZIPs temporales,
- keystores,
- `local.properties`,
- carpetas de build.

El APK release puede conservarse localmente para pruebas o compartirse por fuera del repositorio.

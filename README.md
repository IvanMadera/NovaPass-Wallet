# 🎫 NovaPass: Quantum Sapphire Edition

**NovaPass** es una billetera digital (Wallet) nativa para Android, desarrollada con un enfoque en **diseño premium y alto rendimiento**. Permite organizar tus **boletos, pases y tickets en formato PDF** bajo un sistema de diseño de vanguardia.

El objetivo de la app es eliminar la fricción al asistir a eventos, ofreciendo un acceso inmediato, estético y seguro a tus pases, centralizados en un entorno visualmente impactante.

## ✨ Características Principales

- **💎 Diseño Quantum Sapphire (Glassmorphism)**: Una interfaz moderna basada en profundidad, transparencias y resplandores radiales ambientales que crean una experiencia inmersiva.
- **🫧 Componentes de Cristal**: Los tickets y buscadores utilizan efectos de "vidrio" con bordes de precisión e insets de 0.5dp para evitar artefactos de renderizado.
- **🎟️ Sistema de Perforación Real**: Los tickets cuentan con muescas laterales logradas mediante técnicas de composición offscreen (`BlendMode.Clear`), permitiendo que el fondo sea visible a través del boleto.
- **🔆 Sombra de Resplandor Manual (Anti-Artifact)**: Solución personalizada de dibujo en `Canvas` para el botón de acción, evitando los errores de hardware (octágonos) y garantizando una sombra circular perfecta con aura dorada.
- **📄 Visor PDF de Alta Fidelidad**: Renderizado nativo usando `android.graphics.pdf.PdfRenderer` con soporte para **Pinch-to-zoom**, ideal para escanear QRs y leer detalles pequeños.
- **💾 Persistencia con Room**: Almacenamiento local seguro de metadatos y enlaces a archivos, con reactividad inmediata mediante `StateFlow`.
- **📂 Gestión Inteligente de Archivos**: Uso de *Storage Access Framework* para importar PDFs sin duplicar archivos innecesariamente en el sistema.

## 🛠️ Tecnologías y Arquitectura

NovaPass está construida sobre una arquitectura robusta **MVVM (Model-View-ViewModel)**.

- **Frontend**: Jetpack Compose (Material 3), Navigation Compose, UI Graphics Avanzado.
- **Backend Local**: Room Persistence Library.
- **Lógica de Dibujo**: `DrawScope`, Composición Offscreen, `PathEffect` para líneas punteadas dinámicas.
- **Lenguaje**: Kotlin + Coroutines para procesamiento asíncrono y seguro de PDFs.
- **Tokens de Diseño**: Sistema centralizado de colores en `Color.kt` (Deep Navy, Premium Gold, Emerald Tint).

## 🚀 Requisitos Técnicos

- **Min SDK**: 31 (Android 12+)
- **Target SDK**: 35 (Android 15)
- **Soporte**: Optimizado para pantallas OLED con negros profundos y altos contrastes.

## 📦 Instalación y Uso

1.  Clona el repositorio.
2.  Abre el proyecto en **Android Studio (Ladybug o superior)**.
3.  Sincroniza `Gradle` para obtener las dependencias oficiales de AndroidX.
4.  Ejecuta la aplicación en un dispositivo físico para apreciar los efectos de profundidad y resplandor.

---
*NovaPass es un ejemplo de cómo la potencia de Jetpack Compose puede elevar una herramienta funcional a una pieza de diseño de software premium.*

# 🎫 NovaPass

**NovaPass** es una aplicación nativa para Android desarrollada completamente en **Kotlin** y **Jetpack Compose** que funciona como una "billetera" (Wallet) personal enfocada en organizar tus **boletos, tickets y pases en formato PDF**.

El objetivo de la app es que no tengas que escarbar entre tus descargas o correos electrónicos al momento de asistir a un evento. Puedes simplemente importar el boleto a la aplicación y quedará guardado y asegurado para cuando más lo necesites.

## ✨ Características Principales

- **📱 Diseño 100% Jetpack Compose**: Toda la interfaz está construida con el entorno declarativo más moderno de Android (Material 3).
- **📂 Importación Rápida de Archivos**: Agrega tus PDFs directamente desde el almacenamiento central de tu teléfono de manera sencilla. Utiliza Storage Access Framework (`OpenDocument`) para persistir tus accesos a los archivos.
- **📄 Visor PDF Ultra-Rápido (Nativo)**: Renderizado de páginas PDF usando directamente `android.graphics.pdf.PdfRenderer` en conjunto con el ciclo de vida de corrutinas en Android; eliminando dependencias infladas, librerías *alpha* inestables o *memory leaks*.
- **🔍 Zoom Nativo (Pinch-to-zoom)**: Visualiza las letras chiquitas de tu boleto o acerca los Códigos QR usando dos dedos para hacer Zoom sin perder calidad.
- **💾 Almacenamiento Local (Room Database)**: La aplicación utiliza el ORM oficial básico de Android (`Room`) para almacenar el historial de manera persistente, enlazando las URI de los boletos. Todos tus boletos vivirán en un solo lugar y responderán a Flow `StateFlow` para reactividad inmediata en la UI.
- **🗑️ Eliminación Fácil**: Borra cualquier pase expirado con tan solo un botón.

## 🛠️ Tecnologías y Arquitectura

El proyecto está diseñado bajo un patrón arquitectónico **MVVM (Model-View-ViewModel)**.

- **Frontend**: Jetpack Compose (Material 3), Navigation Compose
- **Lenguaje**: Kotlin
- **Base de Datos Local**: Room (AndroidX)
- **Procesamiento de Archivos**: ParcelFileDescriptor, UI Graphics Bitmap y persistencia de URIs
- **Lógica asíncrona**: Kotlin Coroutines + `Mutex` (para concurrencia y seguridad de entrada-salida sobre IO / `PdfRenderer`).

## 🚀 Requisitos

- **Min SDK**: 31 (Android 12+)
- **Target SDK**: 35 (Android 15)

## 📦 Instalación

1. Clona el repositorio a través de tu entorno local.
2. Abre el proyecto en **Android Studio**.
3. Deja que `Gradle` sincronice el proyecto y descargue las dependencias (Principalmente `androidx.compose` y `androidx.room`).
4. Haz clic en **Run** (`Shift + F10`) con un Emulador corriendo Android 12 o superior, o conéctalo a tu dispositivo físico.

---
*Si deseas implementar más extensiones, considera agregar una vista en miniatura de la primera página del pdf para el listado Room, ¡la lógica ya está en progreso dentro del `TicketEntity`!*

# Plan de Implementación: Compatibilidad 100% con Android TV y TV Box

Hacer que la aplicación `CFilm` sea totalmente compatible nativamente con Android TV y TV Boxes, permitiendo su aparición en la pantalla de inicio (Leanback Launcher) y asegurando navegación fluida con control remoto (D-Pad).

## ¿Será un APK aparte o el mismo APK?

> [!NOTE]
> **APK Único (Universal):**
> **Será el mismo APK para todo.** Gracias a declarar las características de hardware como opcionales (`required="false"`), Google Play Store y el sistema Android permiten que **un solo archivo APK** funcione perfectamente tanto en teléfonos móviles como en tablets, Android TV y TV Boxes. No necesitas mantener dos proyectos ni APKs separados.

---

## User Review Required

> [!IMPORTANT]
> **Banner de Android TV:**
> Se creará un recurso gráfico vectorial o de drawable para el banner de la televisión (`tv_banner.xml`) requerido por Google TV / Android TV.

---

## Proposed Changes

### 1. Configuración del Manifiesto y Recursos

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/AndroidManifest.xml)
- Declarar características de hardware opcionales (`android.hardware.touchscreen` y `android.software.leanback` requeridas como `false` para permitir instalación en TV y móviles).
- Añadir el atributo `android:banner` en la etiqueta `<application>`.
- Añadir el `intent-filter` con la categoría `android.intent.category.LEANBACK_LAUNCHER` en `MainActivity`.

#### [NEW] [tv_banner.xml](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/res/drawable/tv_banner.xml)
- Crear un banner vectorial corporativo para la pantalla de inicio de Android TV (proporción 320x180).

---

### 2. Adaptación de Navegación por Foco (D-Pad / Control Remoto)

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/CFilm1/app/src/main/java/com/example/peliselprimazo/ui/screens/home/HomeScreen.kt) y Componentes de Películas
- Asegurar que los elementos interactivos clave (tarjetas de películas, pestañas de navegación, botones) soporten enfoque de teclado/D-Pad mediante `Modifier.focusable()` o selección activa por foco para que el control remoto de la TV pueda navegar sin problemas.

---

## Verification Plan

### Automated Tests
- Compilación del proyecto con `./gradlew app:assembleDebug`.

### Manual Verification
- Desplegar en un emulador de Android TV o TV Box y comprobar:
  1. Que la aplicación aparece en la pantalla principal (Launcher) de Android TV.
  2. Que se puede navegar por los menús usando la cruceta (D-Pad) del control remoto.

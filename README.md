# RuleApp · cliente Android nativo

Galería Compose de fotos, GIF y videos para Shimmie2, Paheal, Rule34.xxx y Danbooru. Cliente no oficial para adultos, distribuido como APK personal. Sin anuncios, backend ni analítica.

## Uso

1. Confirma mayoría de edad y elige clasificaciones visibles. Shimmie usa **Sin clasificar**; ninguna etiqueta garantiza la clasificación real.

2. En **Explorar**, elige fuente, busca tags y abre una publicación. Las miniaturas son estáticas; GIF y videos se reproducen en el visor. Los videos comienzan pausados.

3. Guarda en favoritos y añade a una o varias colecciones. Borrar una colección conserva favoritos; quitar un favorito elimina sus pertenencias.

4. Rule34 necesita `user_id` y API key personales en **Ajustes → Configurar**. Danbooru admite login/API key opcionales. Usa **Probar conexión** para verificar tu acceso.

5. Los originales autorizados se descargan en `Downloads/RuleApp`; su estado aparece en Ajustes. Los metadatos guardados funcionan offline; el archivo requiere caché o descarga.

Una página totalmente oculta por filtros no dispara una búsqueda automática ilimitada. Se puede avanzar explícitamente con «Buscar en la página siguiente».

## Compilar

Se requiere Android SDK 36 y JDK 21. El bytecode se genera para Java 17; minSdk 26 y applicationId original `com.leninasto.ruleapp`.

```powershell

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'

$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

.\gradlew.bat :app:assembleRelease

```

APK personal: `app/build/outputs/apk/debug/app-debug.apk`. Instalación: `adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk`. Selecciona explícitamente el dispositivo.

Release se compila **sin firma**. Para distribución necesitas tu propio keystore y una configuración de firma externa al repositorio. No publiques el APK debug como release. Conserva la misma clave de firma para actualizar instalaciones existentes.

## Arquitectura y privacidad

Un módulo de producto con contenedor manual, ViewModels/StateFlow/SavedStateHandle, Paging 3, Room y DataStore. `API → parser por motor → BooruPost → Paging → Compose`. No hay WebView ni scraping HTML.

Identidad: `siteId:postId`. Room persiste favoritos, colecciones, relaciones, historial y registros de DownloadManager. Sus esquemas se versionan en `app/schemas`; futuros cambios necesitan migración explícita.

Páginas desde 1 en Shimmie; `pid=page-1` en Rule34; cursor anterior al último ID para Danbooru en orden reciente. La paginación usa el tamaño recibido del servidor antes de deduplicar/filtrar. Sesiones en memoria por fuente conservan resultados y scroll; nuevas consultas, incluso iguales, generan una nueva sesión y cancelan la anterior.

Claves AES-GCM con Android Keystore, aisladas por fuente y excluidas de backup/transferencia. Si se pierde la clave del dispositivo, vuelve a introducir las credenciales. No se registran URLs autenticadas. API sin caché HTTP compartida, sin redirects y sin interceptores que adjunten credenciales a CDN. Imágenes usan un cliente independiente y caché de 256 MiB.

401/403 se muestran sin reintentos. Errores transitorios tienen dos reintentos máximos. 429 respeta `Retry-After`; esperas largas bloquean nuevas llamadas de esa fuente durante el plazo en la sesión actual. El usuario puede quitar credenciales, caché, historial y biblioteca por separado.

Las APIs reciben la IP y las búsquedas. Las credenciales personales de Rule34 no se incluyen en el APK ni en este repositorio. No se usan múltiples claves para evadir límites. Consulta [sus términos de API](https://api.rule34.xxx/).

## Pruebas

```powershell

.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest

adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk

adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

adb -s SERIAL shell am instrument -w -e notClass com.leninasto.ruleapp.LiveSourcesTest,com.leninasto.ruleapp.DownloadTest com.leninasto.ruleapp.test/androidx.test.runner.AndroidJUnitRunner

```

Tests JVM: XML/JSON, ratings por motor, URLs relativas/nulas, estructuras inesperadas, error HTTP 200, paginación/cursor, cancelación, codificación de tags, reintentos y aislamiento de claves. Los fixtures Rule34 son **sintéticos**, no una respuesta autenticada validada.

Tests instrumentados: Room, cifrado, navegación nativa, favoritos/colecciones, cambio de fuente y repetición de consulta. `LiveSourcesTest` realiza consultas públicas pequeñas; registra el estado de Danbooru sin evadir restricciones. Una prueba que registra denegación no implica que la conexión de esa fuente esté validada.

La matriz final requiere Android 26, 28 y 36, teléfono/ventana amplia, rotación, fuente ampliada, TalkBack, formatos GIF/MP4/WebM y descargas reales. Ver [VALIDATION.md](VALIDATION.md) para resultados y pendientes.

## Rendimiento

El módulo `macrobenchmark` mide cinco iteraciones de scroll con `FrameTimingMetric`. Usa una variante **benchmark** independiente (`com.leninasto.ruleapp.benchmark`), no depurable, firmada para pruebas y con paisajes sintéticos generados localmente. Esas clases no se empaquetan en debug/release del producto. No depende de cuentas ni servicios remotos.

```powershell

.\gradlew.bat :app:assembleBenchmark :macrobenchmark:assembleBenchmark

.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest

```

Ejecutar el benchmark de aceptación en un teléfono físico declarado, con frecuencia y condiciones constantes. Para Android 31+, `frameOverrunMs` p95 ≤ 0 corresponde al objetivo de al menos 95 % de frames dentro del presupuesto. Un resultado en emulador es exploratorio, no certifica rendimiento en teléfono. Informes/trazas: `macrobenchmark/build/outputs/connected_android_test_additional_output`.

## Referencias

- [Material 3 en Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

- [Cuadrículas de alturas variables](https://developer.android.com/develop/ui/compose/lists)

- [Shimmie Danbooru Client API](https://github.com/shish/shimmie2/blob/main/ext/danbooru_api/main.php)

- [Rule34 API](https://api.rule34.xxx/) y [Danbooru API](https://safebooru.donmai.us/wiki_pages/help%3Aapi)

- [Media3 y ciclo de vida](https://developer.android.com/media/media3/exoplayer/hello-world)

- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)

## Bloqueo, supervisión y acentos

En Ajustes se puede activar un PIN de 6–12 dígitos o una contraseña de 8–128 caracteres. Es independiente del bloqueo del teléfono y no usa biometría. Se almacena un hash PBKDF2-HMAC-SHA256 con sal aleatoria de 32 bytes y 210 000 iteraciones, excluido de backup/transferencia. Se bloquea al salir de primer plano, oculta capturas/vista de recientes cuando está activo y aplica espera progresiva desde cinco fallos. Cambiar o quitar la clave requiere la actual. No existe recuperación: restablecer los datos de Android elimina la biblioteca local. No equivale a control parental resistente a desinstalación o borrado de datos.

No se encontró una API pública documentada de Family Link para leer el estado de supervisión desde una app normal. Instalar Family Link, tener un perfil de trabajo o un bloqueo del sistema no permite inferir ese estado. El supervisor puede [bloquear RuleApp desde Family Link](https://support.google.com/families/answer/7103028). La app admite además la restricción administrada Android `app_disabled=true` (deshabilita UI y cancela consultas). Esta integración con [configuraciones administradas](https://developer.android.com/work/managed-configurations) está destinada a administradores Android; no se presenta como una integración automática con Family Link.

Acentos de fallback: Shimmie violeta, Paheal ámbar, Rule34 verde y Danbooru azul. En Android 12+ el color dinámico del sistema está activo por defecto y tiene prioridad. Al desactivarlo, aparece el acento de la fuente seleccionada.

## Diagnóstico de Rule34.xxx

El 10/10/2026, una petición HTTPS normal desde este PC recibió HTTP 403, `server: cloudflare` y `cf-mitigated: challenge`, tanto en la portada como en la lista web. Esto identifica un desafío contra clientes automatizados en esa ruta de acceso; no demuestra censura geográfica ni caída del sitio. El lector web externo sí pudo recuperar la portada, lo que confirma que el resultado depende del cliente/ruta/IP.

La petición de lectura a la API, sin claves, recibió HTTP 200 con una cadena JSON que indica ausencia de autenticación. Son restricciones diferentes: navegador/desafío para la web, credenciales personales para la API. El parser convierte ese error HTTP 200 en un estado de credenciales; no lo trata como una lista vacía. La app usa la API nativa y abre páginas auxiliares en el navegador con Custom Tabs. No evita desafíos, denegaciones ni límites. La respuesta autenticada y el esquema Rule34 siguen pendientes hasta introducir las claves dentro de la app. [Documentación de Rule34](https://api.rule34.xxx/).

## Bienvenida e Información (0.9_beta)

La bienvenida aparece una vez por apertura y conserva su estado al rotar o volver de Ajustes. El historial incluye 0.5_beta (inicio) y 0.9_beta (reconstrucción nativa). Info ofrece el historial, GitHub del creador, el repositorio y la donación voluntaria en Buy Me a Coffee. Ajustes se abre en SettingsActivity, interna y no exportada, con las mismas restricciones y bloqueo de la app. Los indicadores ondulados oficiales de Material Components 1.13.0 se integran en Compose mediante AppLoadingIndicator; se conserva Compose Material 3 estable 1.4.0.

## Sugerencias de tags

Desde la primera letra, Danbooru y Rule34 usan sus endpoints de autocomplete con una espera de 350 ms y cancelación al cambiar el texto o la fuente. Rule34 necesita sus credenciales. Shimmie2 y Paheal no ofrecen autocomplete por prefijo en la extensión documentada: se sugieren tags de publicaciones cargadas durante la sesión y metadatos guardados, respetando fuente y clasificaciones visibles. La UI indica ese alcance y explica errores o ausencia de sugerencias; no hace pasar un fallo por una búsqueda vacía. El vocabulario en memoria está limitado a 1000 publicaciones por fuente. Elegir un chip completa el último tag, conserva exclusiones/alternativas y añade un espacio para escribir el siguiente. La búsqueda de publicaciones se envía al confirmar.

# Validación de RuleApp 0.9_beta

Fecha: 10/10/2026. Rama: `codex/native-booru-rebuild`. APK debug para uso personal; release requiere la firma del propietario.

## Alcance implementado

Galería nativa sin WebView ni scraping; cuatro adaptadores aislados (Shimmie2, Paheal, Rule34.xxx y Danbooru), búsqueda, paginación, visor, GIF/video, descargas, favoritos, colecciones múltiples e historial. Credenciales cifradas, bloqueo propio sin biometría, restricción administrada `app_disabled`, acentos por fuente y prioridad del color dinámico. Bienvenida e historial 0.5_beta → 0.9_beta, pestaña Info con enlaces reales y Ajustes en actividad interna propia.

## Evidencia previa a la última actualización

- 17 pruebas JVM de parsers, paginación y red pasaron antes del reinicio del PC. Se regenera y verifica el resultado de la versión final por separado.
- Android 36 emulado: 11 pruebas locales pasaron; lecturas públicas reales de Shimmie2, Paheal y Danbooru verificadas.
- Android 28 emulado: 11 pruebas locales pasaron. Las pruebas de red/descarga quedaron limitadas por la conectividad del emulador.
- Android 26 emulado: pruebas locales verificadas entre dos ejecuciones; WebM se cambió a fixture VP8 compatible y las dos pruebas multimedia pasaron al repetirlas.
- Dispositivo físico disponible: Samsung SM-A065M, Android 36, 720 × 1600 px, densidad 300 dpi. Instalación inicial mediante ADB completada conservando datos.

Las pruebas físicas usan almacenamiento y alias de Keystore propios de QA. No se ejecuta NativeAppTest en el teléfono porque ese conjunto altera preferencias de onboarding. Macrobenchmark utiliza un paquete separado con paisajes sintéticos.

## Requisitos pendientes de aceptación

- Rule34.xxx: introducir claves personales dentro de Ajustes y validar una respuesta autenticada real. Los fixtures no demuestran compatibilidad definitiva con la cuenta.
- TalkBack y revisión manual completa de texto ampliado, ventana amplia y rotación en el APK final.
- Objetivo de rendimiento: 95 % de frames dentro del presupuesto; debe respaldarse con medición, no inferirse de la fluidez visual.
- Firma release del propietario para distribución. El APK debug no es una entrega firmada para publicación.

## Restricciones comprobadas

Rule34.xxx web devolvió 403 con `cf-mitigated: challenge` desde el PC; su API devolvió HTTP 200 con error de autenticación sin claves. Se distinguen ambos estados. Family Link no ofrece una API pública documentada para consultar supervisión desde una app ordinaria; el supervisor puede bloquear la app y RuleApp admite configuraciones administradas Android, sin afirmar integración automática con Family Link. Más contexto y referencias oficiales en README.md.

## Resultado de la compilación y pruebas finales

- Compilación final: `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, `assembleBenchmark`, `testDebugUnitTest` y `lintDebug`: BUILD SUCCESSFUL. Ver `docs/validation/build-final.txt`.
- JVM: 20 pruebas, 0 fallos, 0 errores (red 7, parsers 7, paginación 3, sugerencias 3).
- Lint: 0 errores y 58 advertencias, principalmente dependencias/recursos y recomendaciones. No equivale a un informe sin advertencias.
- Dispositivo físico: 15 pruebas pasaron nuevamente en el APK final tras la mejora de sugerencias (30,839 s). Ver `docs/validation/physical-tests-final.txt`.
- Shimmie2 y Paheal: lista y detalle reales confirmados desde el Samsung. Danbooru: listado público confirmado. DownloadManager: original autorizado descargado y archivo de QA retirado. Evidencia en `docs/validation/physical-live-sources.txt` y `physical-download.txt`.
- Macrobenchmark 1.4.1 se ejecutó en el Samsung con el APK profileable de paisajes sintéticos, pero falló al procesar la primera traza: `Observed no renderthread slices in trace`. No produjo una medición válida ni demuestra el objetivo de 95 %. Ver `docs/validation/macrobenchmark-attempt.txt`.
- APK final: `dist/RuleApp-0.9_beta-debug.apk`, versionCode 3, SHA-256 `E99C131D25A38F4EEC2262EFABA1EEC5DD62F9B61FA198AB48C340799EB997C2`.
- Capturas de bienvenida y galería sintética revisadas visualmente en el dispositivo: `docs/screenshots/physical-welcome.png` y `physical-gallery.png`. La captura de bienvenida precede a la línea añadida sobre sugerencias.

- UI física del APK benchmark final: al escribir `l` apareció el chip `landscape` con datos sintéticos. Se verificó el alcance local de Shimmie y se revisó la captura `docs/screenshots/physical-tags.png`.

## Daparushi 0.9.1_beta · 12/10/2026

Nombre oficial Daparushi, paleta marrón pastel e iconos PNG proporcionados por el propietario. Se conserva `com.leninasto.ruleapp` y la biblioteca existente. Configuración de firma privada excluida de Git, alias validado y firmas APK verificadas.

La entrega final tiene español/inglés, selector de idioma persistente, bienvenida una vez por versión, archivo ver.txt, aviso de actualizaciones, última fuente y visitas, formulario obligatorio Rule34, tarjetas compactas, administrador de descargas, almacenamiento y copias JSON de favoritos/colecciones.

- Debug, release, benchmark, APK de instrumentación, tests y lint compilan correctamente. 26 pruebas JVM y 19 pruebas físicas pasan en Samsung SM-A065M, API 36. Lint: 0 errores, 61 advertencias.
- Recursos: 281 claves ES/EN con argumentos equivalentes. Cambio a inglés, vuelta entre actividades y reinicio del proceso verificados visualmente con contenido sintético. Capturas en docs/screenshots/0.9.1_beta.
- Cinco APK release firmados: ARM64, ARMv7, x86, x86_64 y universal. Se verificó el filtro real de bibliotecas nativas y la firma de cada paquete. APK/checksums en dist; evidencia en docs/validation/0.9.1_beta.
- El teléfono recibe el debug actualizado para conservar sus datos. El release tiene certificado diferente: no se desinstala ni se reemplaza forzosamente. Ver docs/RELEASE.md para exportar/restaurar la biblioteca al migrar.
- Workflow preparado y validado como YAML/shell; ejecución remota/publicación pendiente de configurar secretos y subir código/tag. ver.txt debe publicarse en master para que el comprobador remoto funcione.
- Se retiraron de Git 399 archivos de macrobenchmark/build, conservándolos localmente. No se versionan claves, contraseñas ni cachés de compilación.

Continúan pendientes la validación autenticada Rule34 con credenciales introducidas por el propietario, la medición válida de Macrobenchmark y una revisión completa de TalkBack/ventana amplia del APK final. No se afirma que el objetivo del 95 % de frames se haya medido.

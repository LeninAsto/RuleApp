# Daparushi: firma y distribución

La versión se toma de `ver.txt` (por ejemplo `0.9.1_beta`). El versionCode deriva de los componentes numéricos y sitúa la versión estable después de su beta. Para publicar una nueva versión, sube el número, actualiza el historial de Info y usa un tag idéntico: `v0.9.1_beta`.

## Firma local

Se utiliza el archivo privado `keystore`, alias `rul34`, mediante `keystore.properties`. El archivo de configuración contiene `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Ni el keystore ni la configuración deben añadirse a Git. No se imprimen las contraseñas en los scripts. Guarda una copia privada del keystore y sus contraseñas; perderlos impide actualizar APK release existentes con la misma identidad.

Las variables `RULEAPP_STOREFILE`, `RULEAPP_STOREPASSWORD`, `RULEAPP_KEYALIAS`, `RULEAPP_KEYPASSWORD` tienen prioridad sobre la configuración local. Sin ellas se puede compilar debug y un release sin firma para desarrollo; CI exige todas antes de construir una entrega.

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:assembleRelease -PtargetAbi=arm64-v8a
```

ABI admitidas: `arm64-v8a` (ARM 64), `armeabi-v7a` (ARM 32), `x86`, `x86_64` y `universal`. Se filtran bibliotecas nativas; minSdk 26 y applicationId se mantienen. No son versiones diferentes por API de Android. Usa universal si no conoces la arquitectura. Instalar un APK de una arquitectura incompatible falla normalmente en Android.

## Transición desde debug

El certificado release es distinto del debug que se instaló antes. Android rechazará `install -r` del release sobre debug. No se desinstala automáticamente ni se fuerza una actualización.

1. En el APK debug actualizado: Ajustes → Exportar biblioteca. Conserva el archivo fuera del almacenamiento privado de la app.
2. Si decides cambiar a release, respalda la biblioteca antes de desinstalar debug. La desinstalación elimina preferencias, claves locales y caché.
3. Instala el APK release firmado y usa Restaurar biblioteca. Configura otra vez el PIN y las credenciales; se excluyen intencionalmente del JSON.
4. Las siguientes versiones release se actualizan conservando datos si usan el mismo keystore y un versionCode superior.

Los originales en Downloads son externos al JSON; las descargas en curso y su registro no se transfieren. No se garantiza su acceso desde una nueva instalación. El APK release conserva la identidad original del paquete.

## GitHub Actions

`.github/workflows/android.yml` ejecuta tests/lint y una matriz de cinco APK firmados. Publica una release solo al subir un tag `v*` que coincida con `ver.txt`; las beta son prereleases. Las PR ejecutan checks sin secretos ni publicación. Este cambio prepara el workflow; no sube el keystore ni configura secretos de GitHub automáticamente.

Los jobs usan `ubuntu-24.04`, JDK 21 y acciones con runtime Node 24. El script `.github/scripts/setup-android-sdk.sh` utiliza el SDK incluido en el runner, acepta sus licencias e instala explícitamente `platform-tools`, `platforms;android-36` y `build-tools;36.0.0`. No utiliza `android-actions/setup-android@v3`, cuyo paquete predeterminado `tools` ya no está disponible. Si falla la preparación del SDK, el job falla conservando el error; solo se suben informes cuando existen, mientras que la ausencia de APKs sí falla la entrega.

Configura en Settings → Secrets and variables → Actions:

- `KEYSTORE_BASE64`: archivo del keystore codificado en base64.
- `KEYSTORE_PASSWORD`: contraseña del almacén.
- `KEY_ALIAS`: `rul34`.
- `KEY_PASSWORD`: contraseña de la clave.

No escribas estos valores en el YAML ni en issues. CI restaura la clave en un archivo temporal, comprueba cada APK con apksigner, sube APK/checksums y retira la clave temporal. El aviso de actualizaciones consulta el `ver.txt` publicado en la rama `master`; debe actualizarse al publicar, no únicamente en una rama de desarrollo.

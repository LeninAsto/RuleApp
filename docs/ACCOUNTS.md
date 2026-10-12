# Cuentas y subidas: posibilidades

Esta actualización mantiene lectura remota: no se envían imágenes ni se modifican cuentas.

| Fuente | Cuenta | Subidas posibles |
| --- | --- | --- |
| Danbooru | Login + API key; autenticación API independiente de la sesión web. | Sí: el controlador oficial admite crear uploads autenticados, sujeto a permisos, límites y moderación. Requiere implementar el flujo completo de upload/post. |
| Shimmie2 | El motor permite autenticación con login/contraseña o sesión según endpoint. | Sí en el motor: `/api/danbooru/add_post` o `/api/danbooru/post/create.xml`, con archivo multipart y permiso `CREATE_IMAGE`. Debe confirmarse en la instancia concreta antes de habilitarlo. |
| Paheal | Al usar Shimmie, existe la posibilidad técnica anterior; no se ha validado login real en Paheal. | Posible si la instancia mantiene esa extensión y autoriza la cuenta. No se deduce el permiso de subida solo por funcionar la lectura XML. |
| Rule34.xxx | La API documenta `user_id` + API key para el acceso. Esto no inicia una sesión web en el navegador. | La documentación pública consultada describe lectura y autocomplete, sin contrato de subida. Se puede usar la web autenticada para las acciones que ofrezca; una subida nativa necesita documentación/confirmación del proveedor. |

Para Rule34: inicia sesión en la web, abre opciones de cuenta, copia user_id y API key en el formulario. La app prueba una petición autorizada y guarda las claves cifradas solo si responde correctamente. El ejemplo `123456` + `TU_API_KEY` es ilustrativo y no funciona. Las claves no deben compartirse ni enviarse en chats.

Fuentes primarias consultadas el 12/10/2026:

- [Rule34 API y opciones de cuenta](https://api.rule34.xxx/).
- [Shimmie: autenticación y creación de posts](https://github.com/shish/shimmie2/blob/main/ext/danbooru_api/main.php).
- [Shimmie: permisos y flujo de upload](https://github.com/shish/shimmie2/blob/main/ext/upload/main.php).
- [Danbooru: controlador de uploads](https://github.com/danbooru/danbooru/blob/master/app/controllers/uploads_controller.rb).
- [Danbooru: sesiones y autenticación](https://github.com/danbooru/danbooru/blob/master/app/logical/session_loader.rb).

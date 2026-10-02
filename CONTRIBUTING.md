# Colaborar en Horario Sandra

Este repositorio es la fuente de verdad de la aplicacion. Las solicitudes de cambio
se registran en GitHub para que queden claras, revisables y asociadas a una version.

## Pedir un cambio

1. Abre una incidencia nueva con la plantilla `Solicitar un cambio`.
2. Describe que debe ocurrir, donde debe aparecer y que sucede ahora.
3. Adjunta una captura si el cambio es visual o depende de un horario.
4. Evita incluir datos personales de alumnado, familias, contrasenas o enlaces privados.

Tambien se puede usar el proyecto compartido de ChatGPT `Horario-Sandra` para
conversar sobre la idea antes de abrir la incidencia.

## Preparar una modificacion

1. Parte de la rama `main` actualizada.
2. Haz el cambio en una rama con un nombre claro, por ejemplo
   `cambio/widget-tareas`.
3. Ejecuta:

```powershell
.\gradlew.bat test lint assembleDebug
```

4. Actualiza `CHANGELOG.md` y la version de `app/build.gradle.kts` cuando el
   cambio llegue a usuarios.
5. Sube la rama y abre una pull request para revisar los cambios antes de unirlos.

## Publicar una version

Tras unir el cambio en `main`, crea una etiqueta `vX.Y.Z`. GitHub Actions
compilara la APK firmada y publicara la descarga permanente automaticamente.

```powershell
git tag vX.Y.Z
git push origin vX.Y.Z
```

No compartas el almacén de claves ni secretos de GitHub. La automatizacion de
publicacion ya utiliza el secreto configurado en el repositorio.

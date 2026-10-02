# Horario Sandra

Aplicación Android para consultar el horario de Sandra, los turnos de patio y biblioteca, el apoyo en el aula y las tareas desde la pantalla principal o su widget.

[Descargar siempre la última versión](https://github.com/5538180/horario-sandra/releases/latest/download/Horario-Sandra.apk) · [Página de presentación](https://5538180.github.io/horario-sandra/) · [Historial de cambios](CHANGELOG.md)

## Colaborar

Para proponer una mejora, abre una [solicitud de cambio](../../issues/new?template=change-request.yml) y describe el resultado esperado. El proyecto compartido de ChatGPT `Horario-Sandra` sirve para hablar de las ideas; GitHub conserva el codigo, las revisiones y las versiones publicadas.

Quien vaya a modificar la aplicacion necesita su propia cuenta de GitHub y acceso de colaborador al repositorio. No necesita acceso al ordenador donde se desarrollo la app.

Consulta [CONTRIBUTING.md](CONTRIBUTING.md) para el flujo completo de pruebas y publicacion.

## Qué hace

- Horario diario y semanal, con días no lectivos y festivos de Murcia y Sangonera la Verde.
- Turnos de patio y biblioteca alineados con el calendario del centro.
- Persona de apoyo identificada en cada clase que la tenga.
- Exportación de la semana a Google Calendar.
- Widget con cambio de día o semana, navegación y lista de tareas sincronizada.

## Arquitectura

- `model`: modelos fuertemente tipados.
- `data`: `ScheduleRepository`, fuente local del horario.
- `util`: utilidades de fecha y cálculo de días laborables.
- `ui`: pantalla principal con Jetpack Compose y Material 3.
- `widget`: widget Glance, acciones y receptores de actualización.

## Abrir en Android Studio

1. Abre esta carpeta como proyecto Android.
2. Sincroniza Gradle.
3. Comprueba que el SDK 36 esté instalado. Este entorno usa `local.properties` apuntando a un SDK local con `android-36`.

## Ejecutar tests

```powershell
.\gradlew.bat test lint assembleDebug
```

## Compilar APK debug

```powershell
.\gradlew.bat assembleDebug
```

El APK esperado queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Instalar APK

1. Copia o abre el APK en el móvil.
2. Android puede pedir permiso para "Instalar apps desconocidas" desde Archivos, navegador o Drive.
3. Instala la app.

## Añadir el widget en Android

1. Mantén pulsada la pantalla de inicio.
2. Entra en Widgets.
3. Busca "Horario Sandra".
4. Añade el widget, preferiblemente en tamaño 4x4.

## Modificar horario

Edita `app/src/main/java/es/sandra/horario/data/ScheduleRepository.kt`.

## Modificar colores

Edita los valores `hexColor` de `ScheduleType` en `app/src/main/java/es/sandra/horario/model/Models.kt`.

## Cambiar inicio o fin del curso

Edita `courseStart` y `courseEnd` en `app/src/main/java/es/sandra/horario/util/DateUtils.kt`.

## Modificar patrón de patios

Edita `patioStatus` en `ScheduleRepository.kt`. El cálculo actual usa la rueda de cuatro parejas
del calendario de patios, empezando el 07/09/2026:

- `workdayIndex % 4` para localizar el turno de Sandra/Mª Carmen.
- Todos los días de lunes a viernes avanzan la rueda, incluso si el centro no tiene clase ese día;
  así el siguiente día lectivo coincide con el calendario impreso de patios.
- Cuando aparece Sandra/Mª Carmen, Sandra es la responsable de biblioteca y se resalta en amarillo.

## Añadir festivos en el futuro

Añade fechas a `SchoolCalendarRepository.kt`. Los festivos y días no lectivos ocultan el horario del
día, pero no detienen la rotación del calendario de patios.

## Cambiar enlace de Google Sheets

Edita `googleSheetUrl` en `DateUtils.kt`.

## Publicar una actualización

1. Actualiza `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Añade los cambios relevantes a `CHANGELOG.md`.
3. Ejecuta las pruebas y sube la rama `main`.
4. Crea y publica una etiqueta con el formato `vX.Y.Z`.

```powershell
git tag v1.5.4
git push origin v1.5.4
```

GitHub Actions compilará la APK, creará una publicación de GitHub Releases y actualizará automáticamente el enlace permanente de descarga.

## Estructura pública

- `docs/`: página pública de presentación para GitHub Pages.
- `.github/workflows/`: publicación automática de la web y de cada APK.
- `CHANGELOG.md`: seguimiento de versiones y cambios.

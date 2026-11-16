# PM2E1Grupo1

**Examen Segundo Parcial · Programación Móvil I (PMO-0602) · Grupo 1 · Periodo III / 2026**

Catedrático: Ing. Ricardo Enrique Lagos Mendoza

|    IntegranteE                 |   Cuenta     |
|--------------------------------|--------------|
| Angel Manuel Rivera            | 202330010471 |
| Anthony Joel Gonzales Juárez   |202310010114  |
| Brayan wilfredo Torres aguilar |201820030041  |
| Johan Moises Lopez Moran       |202220010502  |
| Luis David Diaz Medina         |202210010189  |

## Tecnología asignada

| | |
|---|---|
| **Librería de red** | OkHttp 4.12 (Sílabo, Unidad 8.4) |
| **MIME type a subir** | `image/jpeg` |
| **Tamaño máximo** | 5 MB |
| **API** | C# · ASP.NET Core Web API (.NET 10) · Entity Framework Core (SQLite) · arquitectura limpia · Swagger |
| **App** | Android (Java para red y modelos, Jetpack Compose en Kotlin para las pantallas) · `minSdk 24` · `targetSdk 37` |

## URL base de la API

| Dónde corre la app | URL base |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:5000` |
| Teléfono físico (misma red Wi-Fi que la PC) | `http://<IP-de-la-PC>:5000` |

Swagger: `http://localhost:5000/swagger`

La URL que usa la app está en [`ApiConfig.java`](android/app/src/main/java/com/example/pm2e1grupo1/red/ApiConfig.java).

## Estructura del repositorio

```
android/   App Android (Compose + OkHttp)
api/       API REST en C# (solución con 4 proyectos)
postman/   Colección de Postman exportada
```

## La aplicación

Aplicación de contactos con fotografía y ubicación. Evolucionó de la agenda del primer parcial (que guardaba en SQLite) para guardar todo en la API REST.

1. **Pantalla 1 · Registro:** toma una fotografía con la cámara, obtiene la ubicación en tiempo real (Fused Location) con latitud y longitud bloqueadas, valida los datos y sube el contacto con `POST` multipart (`image/jpeg`).
2. **Pantalla 2 · Contactos salvados:** lista desde la API (`GET`), búsqueda por texto y fotografía descargada de la API. Al tocar un contacto se despliegan sus opciones: **Editar** (`PUT`), **Eliminar** (`DELETE`), **Llamar** y **Ver ubicación** (abre el mapa).
3. **Pantalla 3 · Mapa:** muestra el punto del contacto en Google Maps con un marcador con su nombre.

Detalles que pide el enunciado:

- Un único `OkHttpClient` (singleton) con `HttpLoggingInterceptor` y tiempos de espera de conexión, lectura y escritura ([`ClienteHttp.java`](android/app/src/main/java/com/example/pm2e1grupo1/red/ClienteHttp.java)).
- Peticiones con `enqueue()`: ninguna corre en el hilo principal; la interfaz se actualiza con `runOnUiThread()`.
- Archivo armado con `MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("archivo", nombre, RequestBody.create(archivo, MediaType.parse("image/jpeg")))`.
- Fotografía con `ActivityResultContracts.TakePicture` y `FileProvider`; el permiso de cámara denegado se maneja (con acceso a los ajustes si fue "no volver a preguntar").
- Ubicación con `FusedLocationProviderClient`; se manejan el GPS apagado y el permiso denegado.
- Indicador de carga en cada petición y, ante un error de conectividad, un mensaje con **Reintentar**.
- Si la API responde 400 o 415, la app muestra el `mensaje` del JSON del servidor.
- Si la foto supera 5 MB (las cámaras modernas lo hacen), la app la reduce antes de subirla.

## La API REST

| Método y ruta | Descripción |
|---|---|
| `POST /api/contactos` | Crea un contacto. `multipart/form-data`: `nombre`, `telefono`, `latitud`, `longitud`, `archivo` |
| `GET /api/contactos?buscar=` | Lista los contactos; con `buscar` filtra por nombre o teléfono |
| `GET /api/contactos/{id}` | Un contacto |
| `GET /api/contactos/{id}/archivo` | Descarga la fotografía con el `Content-Type` correcto |
| `PUT /api/contactos/{id}` | Actualiza los datos; la fotografía es opcional |
| `DELETE /api/contactos/{id}` | Elimina el contacto y su fotografía |

- La fotografía se guarda en el servidor (`App_Data/uploads`); la base de datos guarda solo la **ruta, el MIME type y el tamaño**.
- El servidor valida el MIME type (`image/jpeg`, incluida la firma real del archivo) y el tamaño máximo de 5 MB. Responde **400** (datos o tamaño inválidos), **404** (no existe) o **415** (tipo no permitido) con un JSON `{ "mensaje": "...", "errores": [...] }`.
- Arquitectura limpia: `Domain` → `Application` → `Infrastructure` → `Api`.
- Documentada con Swagger/OpenAPI en `/swagger`. La colección de Postman está en [`postman/`](postman/PM2E1Grupo1.postman_collection.json).

## Cómo ejecutarlo

### 1. La API

Requisitos: [.NET SDK 10](https://dotnet.microsoft.com/download).

```bash
cd api/PM2E1Grupo1.Api
dotnet run
```

Queda escuchando en `http://0.0.0.0:5000` y abre Swagger. La base de datos SQLite y la carpeta de fotografías se crean solas la primera vez.

### 2. La app Android

1. Abrir la carpeta `android/` en Android Studio y esperar la sincronización de Gradle.
2. Con la API ya corriendo, ejecutar la app en un emulador (Android 7.0 / API 24 o superior).
3. Para que el GPS tenga datos en el emulador: **⋯ (Extended controls) → Location** y fijar una ubicación.
4. En **Android 17 o superior** la app pide el permiso de **red local** (`ACCESS_LOCAL_NETWORK`). Hay que concederlo: sin él Android bloquea la conexión con la API (que corre en la PC, dentro de la red local) y la app muestra "Sin conexión".
5. En un teléfono físico, cambiar `BASE_URL` en `ApiConfig.java` por la IP de la PC y permitir el puerto 5000 en el firewall de Windows.

### 3. El mapa (API key de Google Maps)

Sin API key el mapa se ve en blanco (la app avisa en pantalla). Para configurarla:

1. Entrar a [Google Cloud Console](https://console.cloud.google.com/) y crear un proyecto.
2. En **APIs y servicios → Biblioteca**, habilitar **Maps SDK for Android**.
3. En **APIs y servicios → Credenciales → Crear credenciales → Clave de API**, copiar la clave.
4. En `android/local.properties` (archivo que no se sube a git) agregar:

   ```properties
   MAPS_API_KEY=tu_clave_aqui
   ```

5. Sincronizar Gradle y volver a ejecutar la app.

## Capturas

_(agregar en la carpeta `capturas/` e insertarlas aquí: registro, alertas, lista, actualizar, mapa y Swagger)_

## Rúbrica

| Requisito | Dónde está |
|---|---|
| Toma fotográfica (permisos) | `util/CapturaFoto.kt` |
| Ubicación GPS (lat/lng) | `util/GestorUbicacion.java`, `vista/MainActivity.kt` |
| Validaciones de input | `controlador/ContactoControlador.java` (en la app) y `ContactoValidador.cs` (en el servidor) |
| Subir image/jpeg (POST) | `red/ContactosApi.java` → `crear()` |
| Obtener lista (GET) y buscar | `red/ContactosApi.java` → `listar()` |
| Ver fotografía de la API | `red/CargadorImagenes.java`, `ui/Componentes.kt` → `FotoApi` |
| Actualizar (PUT) y eliminar (DELETE) | `red/ContactosApi.java` → `actualizar()`, `eliminar()` |
| Punto en el mapa | `vista/MapaActivity.kt`, `ui/PantallaMapa.kt` |
| REST API C# + Swagger | `api/` |
| Implementación OkHttp | `red/ClienteHttp.java`, `red/ContactosApi.java` |
| Extra: Jetpack Compose | pantallas en `ui/` y actividades en `vista/` |

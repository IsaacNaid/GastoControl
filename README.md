# GastoControl

## Descripción

GastoControl es una aplicación móvil para Android que permite registrar y administrar ingresos y gastos personales. Su objetivo es ayudar a los usuarios a llevar un mejor control de sus finanzas mediante categorías, historial de movimientos y un resumen de su información financiera.

El proyecto integra una aplicación Android desarrollada con Kotlin y Jetpack Compose, una API REST desarrollada con Spring Boot y una base de datos Oracle Cloud.

## Integrantes

| Integrante | Matrícula |
|---|---|
| Isaac Naid García Benítez | 20253ds038 |
| Miriam Valeria Arellano Diegues | 20253ds032 |
| Guillermo Lara Rosales | 20253ds044 |
| Brian Emiliano Gálvez Villalba | 20253rd056 |

## Funcionalidades

- Registro e inicio de sesión de usuarios.
- Autenticación mediante Spring Security.
- Acceso biométrico mediante huella digital en dispositivos compatibles.
- Registro, consulta, edición y eliminación de movimientos.
- Administración de categorías de ingresos y gastos.
- Visualización de ingresos, gastos y saldo.
- Historial con filtros.
- Uso del acelerómetro para detectar el movimiento de agitar el teléfono.
- Persistencia local de categorías mediante Room.
- Comunicación con la API REST y almacenamiento remoto en Oracle.

## Tecnologías

### Aplicación Android
- Kotlin
- Jetpack Compose
- MVVM
- Navigation Compose
- Room
- DataStore
- Retrofit y Gson
- OkHttp
- BiometricPrompt
- Sensor acelerómetro

### Backend
- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Oracle Database
- Maven

## Arquitectura

La aplicación utiliza una arquitectura cliente-servidor.

Android -> Retrofit -> API REST Spring Boot -> JPA/Hibernate -> Oracle Cloud

En Android, las pantallas de Jetpack Compose interactúan con los ViewModels. Los repositorios administran los datos y utilizan Retrofit para realizar solicitudes HTTP al servidor.

Spring Boot recibe las solicitudes, aplica las validaciones y utiliza los repositorios JPA para consultar o modificar la información almacenada en Oracle.

## Estructura del repositorio

- `android/`: aplicación móvil Android.
- `backend/gastocontrol-backend/`: servidor API REST Spring Boot.
- `docs/`: documentación y pruebas del proyecto.

## Requisitos

- Android Studio con Android SDK compatible.
- Dispositivo Android con versión 13 o superior (API 33).
- JDK 21 para el backend.
- Acceso a una base de datos Oracle compatible.
- Credenciales de Oracle y Wallet cuando la conexión lo requiera.
- Conectividad entre Android y el servidor Spring Boot.

## Ejecutar el backend

1. Abrir una terminal en `backend/gastocontrol-backend`.
2. Preparar la conexión a Oracle.
3. Definir las variables de entorno:
   - `DB_URL`: URL JDBC de Oracle.
   - `DB_USER`: usuario de Oracle.
   - `DB_PASSWORD`: contraseña de Oracle.
4. Ejecutar:

```bash
./mvnw spring-boot:run
```

Por defecto, la API utiliza el puerto 8080.

Las credenciales y el Wallet no se incluyen en el repositorio. Cada desarrollador debe configurar su propio acceso autorizado a Oracle.

Para compilar el backend sin ejecutar pruebas:

```bash
./mvnw clean package -DskipTests
```

Las pruebas que requieren Oracle necesitan que las variables de conexión estén configuradas.

## Ejecutar Android

1. Abrir la carpeta `android/` en Android Studio.
2. Sincronizar Gradle.
3. Ejecutar el backend Spring Boot.
4. Configurar la dirección de la API.
5. Ejecutar la aplicación en un emulador o dispositivo compatible.

La propiedad Gradle `API_BASE_URL` define la URL del backend.

Para compilar utilizando el emulador Android:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8080/
```

Para utilizar un celular físico:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://IP_DE_LA_COMPUTADORA:8080/
```

Se debe reemplazar `IP_DE_LA_COMPUTADORA` por la dirección IPv4 real del equipo que ejecuta Spring Boot.

La computadora y el celular deben estar conectados a una red que permita la comunicación entre ambos.

La configuración HTTP local se utiliza para pruebas de desarrollo. Para un despliegue público se recomienda utilizar HTTPS.

## Endpoints de la API REST

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registrar usuario |
| GET | `/api/auth/me` | Comprobar autenticación |

El registro es público. La consulta `/me` requiere HTTP Basic y responde sin cuerpo cuando la autenticación es válida.

Ejemplo de registro:

```json
{
  "name": "Usuario Ejemplo",
  "email": "usuario@example.com",
  "password": "Ejemplo123"
}
```

### Categorías

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/categories` | Consultar categorías |
| GET | `/api/categories/{id}` | Consultar una categoría |
| POST | `/api/categories` | Crear categoría |
| PUT | `/api/categories/{id}` | Actualizar categoría |
| DELETE | `/api/categories/{id}` | Eliminar categoría |

Ejemplo de categoría:

```json
{
  "name": "Alimentación",
  "type": "EXPENSE",
  "color": "#4CAF50",
  "icon": "restaurant"
}
```

Los tipos admitidos son `INCOME` y `EXPENSE`.

### Movimientos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/movements` | Consultar movimientos del usuario |
| GET | `/api/movements/{id}` | Consultar movimiento |
| POST | `/api/movements` | Crear movimiento |
| PUT | `/api/movements/{id}` | Actualizar movimiento |
| DELETE | `/api/movements/{id}` | Eliminar movimiento |

La consulta general admite los parámetros opcionales `type`, `from` y `to` para filtrado.

Ejemplo de estructura de un movimiento:

```json
{
  "type": "EXPENSE",
  "amount": "150.00",
  "description": "Comida",
  "date": "2026-11-26",
  "categoryId": 1
}
```

El identificador de categoría debe existir en la base de datos. El formato y las reglas de negocio se validan en el backend.

## Seguridad

La API utiliza Spring Security y autenticación HTTP Basic.

Las contraseñas de los usuarios se almacenan utilizando BCrypt.

Las rutas de categorías y movimientos requieren autenticación. Las operaciones sobre movimientos se realizan dentro del contexto del usuario autenticado.

No se deben publicar contraseñas, archivos Wallet ni otros secretos en GitHub.

## Sensores

El acelerómetro detecta movimientos de agitación del dispositivo para facilitar el acceso al registro de movimientos. Su funcionamiento depende de la disponibilidad del sensor.

La autenticación biométrica se utiliza en dispositivos que cuentan con hardware y configuración compatible.

## Estado del proyecto

Proyecto integrador de la Unidad III de Aplicaciones Móviles, desarrollado con Android, Spring Boot y Oracle Cloud.

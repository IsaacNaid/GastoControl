\# GastoControl - Registro de integración y pruebas



\*\*Fecha:\*\* 26 de noviembre de 2026



\*\*Rama:\*\* feature/integration-movements



\## 1. Ramas integradas



\- feature/isaac-core-categories

\- feature/miriam-auth

\- feature/guillermo-movements

\- feature/brian-dashboard-sensor



\## 2. Resultados de integración



\- Resolución de conflictos entre las cuatro ramas.

\- Navegación centralizada mediante NavHost.

\- Cliente Retrofit y SessionManager compartidos.

\- Centralización de DTO.

\- Integración de autenticación, categorías, movimientos, dashboard e historial.

\- Confirmación biométrica para guardar movimientos.

\- Confirmación convencional cuando no existe disponibilidad biométrica.

\- Comunicación HTTP habilitada únicamente en debug.

\- Configuración de Oracle mediante variables de entorno.



\## 3. Pruebas realizadas



| Prueba | Resultado |

|---|---|

| Gradle clean assembleDebug | Exitoso |

| Gradle test | Exitoso |

| Maven package sin pruebas | Exitoso |

| Revisión de espacios mediante Git | Sin errores |

| Revisión de nombres de archivos sensibles | Sin coincidencias |

| Verificación de navegación única | Correcta |



\## 4. Pruebas funcionales pendientes



\- Conexión del backend con Oracle.

\- Pruebas automatizadas del backend.

\- Registro de usuarios.

\- Inicio de sesión con credenciales correctas e incorrectas.

\- Cierre de sesión.

\- Creación, edición y eliminación de categorías.

\- Creación, edición y eliminación de ingresos y gastos.

\- Actualización del dashboard e historial.

\- Filtros por fecha y tipo de movimiento.

\- Funcionamiento del acelerómetro.

\- Confirmación biométrica y confirmación alternativa.

\- Manejo de sesiones no autorizadas.

\- Aislamiento de información entre usuarios.

\- Persistencia de información en Oracle.

\- Pruebas en emulador y dispositivo físico.



\## 5. Observaciones



La compilación de Android y del backend fue exitosa. Sin embargo, todavía se requiere comprobar el funcionamiento completo de la aplicación con datos reales.



Se encuentra pendiente la revisión de la sincronización de categorías entre Room y Oracle, incluyendo la eliminación remota y el aislamiento de categorías por usuario.



También se deberá comprobar la actualización de información al regresar al dashboard y el comportamiento de la confirmación biométrica durante el inicio de sesión.



No se modificó el esquema de base de datos para resolver estos pendientes.



La integración deberá ser revisada y autorizada por el líder del equipo antes de incorporarse a develop.

# Documentación Práctica 4

## Cambios técnicos introducidos

Esta práctica amplía la aplicación ToDoList con la página de cuenta del usuario,  la edición del perfil, la prioridad de las tareas, una hoja de estilos común y  la configuración de la imagen Docker para producción.

### Página de cuenta
Se añadió la página `/cuenta`, accesible desde el menú de usuario. El método `UsuarioController.cuenta()` obtiene el identificador del usuario logueado y carga sus datos mediante `UsuarioService.findById()`. Si no hay sesión o el usuario no existe, redirige a `/login`.

La plantilla `cuenta.html` muestra correo electrónico, nombre y fecha de nacimiento, además del botón para editar el perfil y un enlace para volver al listado de tareas. La fecha se formatea como `dd/MM/yyyy` y el resto de campos se obtienen del `UsuarioData`.

### Edición del perfil
Se ha añadido la edición de los datos básicos del usuario autenticado:

- `GET /cuenta/editar`: muestra `formEditarPerfil.html` con los datos actuales precargados.
- `POST /cuenta/editar`: valida los datos, actualiza el perfil y redirige a `/cuenta` con un mensaje flash de confirmación.

El formulario usa el DTO `PerfilData`, que contiene únicamente `email`, `nombre` y `fechaNacimiento`. El correo se valida con `@NotBlank` y `@Email`, y la fecha con `@DateTimeFormat(pattern = "dd-MM-yyyy")`. La lógica se realiza en `UsuarioService.actualizarPerfil()`, que busca el usuario por el identificador de la sesión, rechaza correos vacíos, comprueba que el correo no pertenezca a otro usuario, actualiza solo correo, nombre y fecha de nacimiento y conserva sin cambios la contraseña, el rol y el estado de bloqueo.

Si hay errores de validación o el correo ya está registrado, se vuelve a mostrar el formulario conservando los datos introducidos. Los mensajes de validación se definen en `messages.properties`.

### Prioridad de las tareas
Se añadió el enumerado `Prioridad` con los valores `BAJO`, `MEDIO`, `ALTO` y `CRITICO`. La entidad `Tarea` incorpora el campo `prioridad`, que es opcional y vale `null` cuando la tarea no tiene prioridad asignada.

Se han realizado cambios en `TareaData`, que ahora transporta la prioridad entre capas; `TareaService.modificaTarea()`, que recibe la nueva prioridad; `TareaController`, que la precarga y la guarda; y las plantillas `formEditarTarea.html` y `listaTareas.html`, que añaden el desplegable y la columna de prioridad respectivamente

### Estilos de la aplicación

Se añadió `src/main/resources/static/css/estilos.css`, cargado desde `fragments.html` después de Bootstrap. La hoja define una paleta común mediante variables CSS y estiliza. De este modo, todas las vistas comparten el mismo aspecto sin duplicar estilos en cada plantilla.


## Despliegue de producción

### Desarrollo local con PostgreSQL
Con PostgreSQL, reutilizando el contenedor `db-equipo02`:
```bash
docker start db-equipo02
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```
Si el contenedor de base de datos no existe, se crea con la red `network-equipo02`, el alias `postgres` y las credenciales `mads/mads`.

### Producción
En producción se usa el perfil `postgres-prod`, que configura PostgreSQL y`spring.jpa.hibernate.ddl-auto=validate`. Por tanto, la base de datos debe tener ya el esquema y los datos antes de arrancar la aplicación.

Despliegue con dos contenedores:
```bash
docker network create network-equipo02 2>/dev/null || true

docker run -d --name db-equipo02 --network network-equipo02 \
  --network-alias postgres \
  -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads \
  postgres:13

docker run -d --name mads-todolist --network network-equipo02 \
  -p 8080:8080 \
  israelizqdo9/mads-todolist-equipo02:1.3.0 \
  --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres
```
La aplicación queda disponible en http://localhost:8080 y usa la base de datos mads del contenedor db-equipo02, accesible mediante el alias postgres.

## Cambios en el esquema de Base de Datos
#### Versión 1.2.0
El esquema de la versión 1.2.0 está definido en `sql/schema-1.2.0.sql` y se compone de las siguientes tablas:
| Tabla | Campos principales | Relaciones |
|------------------|---------------------------------------------------------------------------------------|-------------------------------------------------------|
| `usuarios` | `id`, `email`, `nombre`, `password`, `fecha_nacimiento`, `administrador`, `bloqueado` | — |
| `tareas` | `id`, `titulo`, `usuario_id` | `usuario_id` referencia a `usuarios.id` |
| `equipos` | `id`, `nombre` | — |
| `equipo_usuario` | `fk_equipo`, `fk_usuario` | Relación muchos-a-muchos entre `equipos` y `usuarios` |

Las tablas `usuarios`, `tareas` y `equipos` utilizan claves primarias autoincrementales mediante secuencias de PostgreSQL. La tabla `equipo_usuario` tiene clave primaria compuesta por `fk_equipo` y `fk_usuario`.

#### Versión 1.3.0
La versión 1.3.0 mantiene la estructura de la versión 1.2.0 e incorpora la prioridad de las tareas. La entidad `Tarea` incluye el campo opcional
`prioridad`, por lo que el esquema debe añadir una nueva columna `prioridad` en la tabla `tareas`.

URL de la imagen Docker:
```text
https://hub.docker.com/r/israelizqdo9/mads-todolist-equipo02
```
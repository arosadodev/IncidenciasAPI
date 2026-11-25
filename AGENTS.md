# AGENTS.md

Guía para agentes de IA (y para cualquiera que toque el proyecto) sobre las
tecnologías, la estructura, las convenciones y los límites de este repositorio.

## 1. Proyecto

- API REST para gestionar incidencias de un centro educativo + frontend básico
  en HTML/CSS/JS.
- Paquete base: `ovh.emserv.incidenciasapi`.
- Build: **Maven** con el wrapper (`./mvnw`), igual que el proyecto de clase
  `26_27_MatriculasSpring`.
- **Java 21**, Spring Boot 4.1.1, Spring MVC, Spring Data JPA (Hibernate),
  Spring Security 7, MySQL 8/MariaDB 11, Lombok, Jakarta Bean Validation.
- JWT con `jjwt` 0.13.0 para firmar y
  `spring-boot-starter-security-oauth2-resource-server` (Nimbus) para validar.
- Contraseñas con `BCryptPasswordEncoder`.
- Base de datos: `jdbc:mysql://localhost:3307/incidencias?createDatabaseIfNotExist=true`
  con `spring.jpa.hibernate.ddl-auto=update` (el esquema se crea solo).

### Herramienta de IA utilizada

El proyecto se desarrolló con **DeepSeek Harness**, un agente de programación
integrado en **IntelliJ IDEA** mediante el plugin `dsh-idea` (no es un chat).
El agente puede leer/escribir archivos, compilar, arrancar la aplicación y
lanzar peticiones HTTP para comprobar los cambios. En el `README.md` están
transcritos los prompts reales utilizados.

## 2. Restricciones sobre lo visto en clase

El proyecto debe poder explicarse con lo trabajado en el módulo. Por tanto:

- **No añadas** frameworks, librerías, lenguajes ni herramientas nuevas sin
  justificarlo en el README (qué hace, cómo se usa y por qué no se resuelve con
  lo ya conocido). En particular, no introduzcas frameworks de frontend
  (React, Vue, Angular), gestores de estado, ORMs distintos de Spring Data JPA
  ni bases de datos embebidas para los flujos principales.
- Se admiten las construcciones ya presentes en el proyecto aunque sean
  específicas de Spring Security/Boot: `@EnableMethodSecurity` y `@PreAuthorize`,
  `oauth2ResourceServer(...)`, `AuthenticationEntryPoint`,
  `AccessDeniedHandler`, `JwtDecoder`, `JwtAuthenticationConverter`.
- Si añades una dependencia, actualiza `pom.xml` **y** el apartado de
  tecnologías del README.
- No inventes comandos ni resultados de pruebas en la documentación.

## 3. Estructura

```
src/main/java/ovh/emserv/incidenciasapi/
├── config/       SecurityConfig, CorsConfig, RestAuthenticationEntryPoint (401),
│                 RestAccessDeniedHandler (403)
├── controller/   AuthController, IncidenciaController, UsuarioController
├── DTO/          DTO de entrada (*Request) y de salida (*Response)
├── entity/       Usuario (UserDetails), Incidencia, EstadoIncidencia, Roles
├── exception/    Excepciones propias + GlobalExceptionHandler
├── repository/   UsuarioRepository, IncidenciaRepository
├── service/      AuthService, IncidenciaService, UsuarioService, JwtService,
│                 CustomUserDetailsService
└── IncidenciasApiApplication.java

frontend/         index.html, css/styles.css, js/{config,api,app}.js
docs/             comprobaciones.sh, guion-video.md
db/roles.sql      Promoción del primer ROLE_ADMIN por SQL
postman/          Colección de Postman de la API
```

## 4. Convenciones de implementación

- Todo el código, los comentarios y los mensajes de la API **en español**.
- Inyección de dependencias **por constructor**. Con Lombok, `@RequiredArgsConstructor`
  en servicios y controladores; sin `@Autowired` en atributos.
- Lombok en entidades y DTO: `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`
  en entidades; `record` para los DTO de entrada y de salida.
- Los controladores solo se ocupan de HTTP: validan el DTO, llaman al servicio
  y devuelven `ResponseEntity` cuando el código depende del resultado
  (`201` al crear con `Location`, `204` al borrar, `200` al consultar).
- La lógica de negocio (y las comprobaciones de permisos de negocio) va en
  `service/`, nunca en el controlador. Los controladores no usan repositorios
  directamente.
- Rutas en plural (`/incidencias`, `/usuarios`) y nombres de métodos en español
  (`crear`, `listar`, `obtener`, `cambiarEstado`, `eliminar`, `cambiarRol`).
- Errores: `{ "estado": 404, "mensaje": "..." }` mediante `ErrorResponse`. Los
  errores de `@Valid` se devuelven como mapa `campo: mensaje` con 400.
  **No** añadas un `@ExceptionHandler(Exception.class)` genérico: capturaría
  `AccessDeniedException` de `@PreAuthorize` y convertiría los 403 en 500.
- `@Transactional` en los métodos del servicio que leen relaciones `LAZY` y
  mapean a DTO (si no, `LazyInitializationException`).

## 5. Reglas de seguridad que no se pueden romper

1. **El autor de una incidencia sale de la autenticación**
   (`Authentication.getName()`, que es el `sub` del JWT). Nunca del cuerpo de la
   petición. `IncidenciaRequest` no tiene campo `autor` ni `estado`.
2. **Un `ROLE_USER` solo puede ver sus incidencias**, tanto en `GET /incidencias`
   como en `GET /incidencias/{id}`. La comprobación de propiedad debe estar en el
   backend (`IncidenciaService.exigirAcceso`), no solo en el frontend.
3. **Los roles se validan contra `Roles.VALIDOS`**; cualquier otro valor
   devuelve 400. El registro siempre asigna `ROLE_USER`.
4. **Los permisos de administración** (`GET /usuarios`, `PATCH /usuarios/{id}/rol`)
   exigen `ROLE_ADMIN` en `SecurityConfig`, con `@PreAuthorize` y, cuando aplica,
   en el servicio (defensa en profundidad).
5. **401 y 403 tienen significados distintos**: 401 = no autenticado o token no
   válido (`RestAuthenticationEntryPoint`); 403 = autenticado sin permiso
   (`RestAccessDeniedHandler` o `AccesoProhibidoException`).
6. **La clave JWT se lee de `app.jwt.secret`** (≥ 32 bytes) y se firma **HS256
   explícito**; el decodificador valida firma, emisor y caducidad. No generes la
   clave de forma aleatoria al arrancar: los tokens deben sobrevivir a un
   reinicio con la misma clave.
7. Nunca expongas entidades JPA con la contraseña ni datos internos: usa DTO.

## 6. Configuración y secretos

- No incluyas contraseñas reales, tokens ni claves de producción en el
  repositorio, en los logs ni en los commits.
- La clave de desarrollo de `app.jwt.secret` es solo para prácticas; en un
  entorno real se pasa con la variable de entorno `JWT_SECRET`.
- Los valores de conexión se pueden sobrescribir con `DB_URL`, `DB_USER` y
  `DB_PASSWORD`. No cambies el nombre de la base de datos (`incidencias`) ni el
  puerto (3307) sin justificarlo.
- No crees la base de datos ni las tablas a mano: `createDatabaseIfNotExist=true`
  y `ddl-auto=update` lo hacen al arrancar. El único paso manual es el primer
  `ROLE_ADMIN` (`db/roles.sql`).

## 7. Validación

- Compilar: `./mvnw -DskipTests package`.
- Arrancar: `./mvnw spring-boot:run`.
- Comprobar la API (30 casos del enunciado):
  `./docs/comprobaciones.sh` con el backend arrancado. Requiere `curl`, `jq` y
  el cliente `mysql` (variables `MYSQL_*`, o `SIN_MYSQL=1` si el admin ya está
  promocionado).
- Frontend: `python3 -m http.server 5500 --directory frontend`.
- Si no puedes ejecutar una comprobación (sin JDK, sin MySQL…), dilo y no
  afirmes que ha pasado.
- No recompiles el jar mientras la aplicación esté ejecutándose desde ese mismo
  archivo: provoca `NoClassDefFoundError` y peticiones colgadas.

## 8. Git y documentación

- Commits pequeños y temáticos con prefijo: `chore:`, `feat(...)`, `fix:`,
  `docs:`, `test:`. Mensajes en español, en imperativo.
- Antes de hacer commit, revisa `git status` para no subir `target/`,
  `.localbuild/`, `.idea/` ni archivos de configuración locales.
- Mantén actualizados `README.md` (endpoints, permisos, prompts reales,
  problemas y decisiones) y este `AGENTS.md` cuando cambien las tecnologías, la
  estructura o las convenciones.
- No inventes prompts, resultados ni enlaces: si el repositorio o el vídeo no
  están publicados, deja el hueco indicado como pendiente.

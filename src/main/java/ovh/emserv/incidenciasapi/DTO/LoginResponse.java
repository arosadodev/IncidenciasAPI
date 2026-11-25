package ovh.emserv.incidenciasapi.DTO;

/**
 * Respuesta del login: el JWT y los datos públicos del usuario autenticado.
 *
 * @param token    JWT firmado que el cliente debe enviar como "Authorization: Bearer ..."
 * @param tipo     siempre "Bearer"
 * @param expiraEnSegundos segundos que le quedan de vida al token
 * @param usuario  datos del usuario al que pertenece el token
 */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {
}

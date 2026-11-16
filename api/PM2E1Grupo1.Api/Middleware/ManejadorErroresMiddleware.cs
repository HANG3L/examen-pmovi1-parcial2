using System.Text.Json;
using PM2E1Grupo1.Api.Contratos;
using PM2E1Grupo1.Application.Excepciones;

namespace PM2E1Grupo1.Api.Middleware;

/// <summary>
/// Convierte las excepciones de la aplicación en respuestas JSON { mensaje, errores }
/// con el código HTTP adecuado (400, 404, 415 o 500).
/// </summary>
public class ManejadorErroresMiddleware
{
    private static readonly JsonSerializerOptions OpcionesJson = new(JsonSerializerDefaults.Web);

    private readonly RequestDelegate _siguiente;
    private readonly ILogger<ManejadorErroresMiddleware> _logger;

    public ManejadorErroresMiddleware(RequestDelegate siguiente, ILogger<ManejadorErroresMiddleware> logger)
    {
        _siguiente = siguiente;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext contexto)
    {
        try
        {
            await _siguiente(contexto);
        }
        catch (ValidacionException ex)
        {
            await Responder(contexto, StatusCodes.Status400BadRequest, ex.Message, ex.Errores);
        }
        catch (TipoMediaNoSoportadoException ex)
        {
            await Responder(contexto, StatusCodes.Status415UnsupportedMediaType, ex.Message);
        }
        catch (NoEncontradoException ex)
        {
            await Responder(contexto, StatusCodes.Status404NotFound, ex.Message);
        }
        catch (BadHttpRequestException ex) when (!contexto.Response.HasStarted)
        {
            // Cuerpo demasiado grande, multipart mal formado, etc.
            _logger.LogWarning(ex, "Petición inválida");
            await Responder(contexto, StatusCodes.Status400BadRequest,
                "La petición no es válida o el archivo es demasiado grande.");
        }
        catch (InvalidDataException ex) when (!contexto.Response.HasStarted)
        {
            _logger.LogWarning(ex, "Formulario multipart inválido");
            await Responder(contexto, StatusCodes.Status400BadRequest,
                "El formulario multipart/form-data no es válido.");
        }
        catch (Exception ex) when (!contexto.Response.HasStarted)
        {
            _logger.LogError(ex, "Error no controlado");
            await Responder(contexto, StatusCodes.Status500InternalServerError,
                "Ocurrió un error interno en el servidor.");
        }
    }

    private static Task Responder(HttpContext contexto, int codigo, string mensaje, IReadOnlyList<string>? errores = null)
    {
        contexto.Response.Clear();
        contexto.Response.StatusCode = codigo;
        contexto.Response.ContentType = "application/json; charset=utf-8";

        var cuerpo = new ErrorRespuesta(mensaje, errores ?? new[] { mensaje });
        return contexto.Response.WriteAsync(JsonSerializer.Serialize(cuerpo, OpcionesJson));
    }
}

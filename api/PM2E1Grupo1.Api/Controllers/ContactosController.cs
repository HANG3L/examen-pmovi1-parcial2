using Microsoft.AspNetCore.Mvc;
using PM2E1Grupo1.Api.Contratos;
using PM2E1Grupo1.Application.Contactos;

namespace PM2E1Grupo1.Api.Controllers;

[ApiController]
[Route("api/contactos")]
[Produces("application/json")]
public class ContactosController : ControllerBase
{
    private const long LimiteCuerpoBytes = 20 * 1024 * 1024; // tope de seguridad; el máximo de negocio (5 MB) lo valida el servicio

    private readonly ContactoServicio _servicio;

    public ContactosController(ContactoServicio servicio) => _servicio = servicio;

    /// <summary>Lista los contactos. Con ?buscar= filtra por nombre o teléfono.</summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<ContactoDto>), StatusCodes.Status200OK)]
    public async Task<ActionResult<List<ContactoDto>>> Listar([FromQuery] string? buscar, CancellationToken ct)
        => Ok(await _servicio.ListarAsync(buscar, ct));

    /// <summary>Obtiene un contacto por id.</summary>
    [HttpGet("{id:int}")]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ContactoDto>> Obtener(int id, CancellationToken ct)
        => Ok(await _servicio.ObtenerAsync(id, ct));

    /// <summary>Descarga la fotografía del contacto con su Content-Type original.</summary>
    [HttpGet("{id:int}/archivo")]
    [Produces("image/jpeg")]
    [ProducesResponseType(typeof(FileResult), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Archivo(int id, CancellationToken ct)
    {
        var archivo = await _servicio.ObtenerArchivoAsync(id, ct);
        return File(archivo.Contenido, archivo.TipoMime);
    }

    /// <summary>Crea un contacto con su fotografía (multipart/form-data).</summary>
    [HttpPost]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(LimiteCuerpoBytes)]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status415UnsupportedMediaType)]
    public async Task<ActionResult<ContactoDto>> Crear([FromForm] CrearContactoFormulario formulario, CancellationToken ct)
    {
        await using var flujo = await AbrirArchivoAsync(formulario.Archivo, ct);

        var creado = await _servicio.CrearAsync(new CrearContactoComando
        {
            Nombre = formulario.Nombre,
            Telefono = formulario.Telefono,
            Latitud = formulario.Latitud,
            Longitud = formulario.Longitud,
            Archivo = Adaptar(formulario.Archivo, flujo)
        }, ct);

        return CreatedAtAction(nameof(Obtener), new { id = creado.Id }, creado);
    }

    /// <summary>Actualiza los datos de un contacto; la fotografía es opcional.</summary>
    [HttpPut("{id:int}")]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(LimiteCuerpoBytes)]
    [ProducesResponseType(typeof(ContactoDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status415UnsupportedMediaType)]
    public async Task<ActionResult<ContactoDto>> Actualizar(
        int id, [FromForm] ActualizarContactoFormulario formulario, CancellationToken ct)
    {
        await using var flujo = await AbrirArchivoAsync(formulario.Archivo, ct);

        var actualizado = await _servicio.ActualizarAsync(id, new ActualizarContactoComando
        {
            Nombre = formulario.Nombre,
            Telefono = formulario.Telefono,
            Latitud = formulario.Latitud,
            Longitud = formulario.Longitud,
            Archivo = Adaptar(formulario.Archivo, flujo)
        }, ct);

        return Ok(actualizado);
    }

    /// <summary>Elimina el contacto y su fotografía.</summary>
    [HttpDelete("{id:int}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ErrorRespuesta), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(int id, CancellationToken ct)
    {
        await _servicio.EliminarAsync(id, ct);
        return NoContent();
    }

    /// <summary>
    /// Copia el archivo a un MemoryStream (máximo ~20 MB por el límite del cuerpo) para poder
    /// leer su firma JPEG y luego guardarlo sin depender del IFormFile.
    /// </summary>
    private static async Task<MemoryStream?> AbrirArchivoAsync(IFormFile? archivo, CancellationToken ct)
    {
        if (archivo is null) return null;

        var memoria = new MemoryStream();
        await archivo.CopyToAsync(memoria, ct);
        memoria.Position = 0;
        return memoria;
    }

    private static ArchivoSubido? Adaptar(IFormFile? archivo, Stream? flujo)
        => archivo is null || flujo is null
            ? null
            : new ArchivoSubido
            {
                Contenido = flujo,
                TipoMime = archivo.ContentType ?? string.Empty,
                Tamano = archivo.Length
            };
}

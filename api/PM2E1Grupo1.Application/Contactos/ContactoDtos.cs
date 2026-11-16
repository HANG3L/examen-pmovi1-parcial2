namespace PM2E1Grupo1.Application.Contactos;

/// <summary>Archivo recibido, independiente de ASP.NET (la capa Api lo adapta desde IFormFile).</summary>
public sealed class ArchivoSubido
{
    public required Stream Contenido { get; init; }
    public required string TipoMime { get; init; }
    public required long Tamano { get; init; }
}

public sealed class CrearContactoComando
{
    public string? Nombre { get; init; }
    public string? Telefono { get; init; }
    public double? Latitud { get; init; }
    public double? Longitud { get; init; }
    public ArchivoSubido? Archivo { get; init; }
}

public sealed class ActualizarContactoComando
{
    public string? Nombre { get; init; }
    public string? Telefono { get; init; }
    public double? Latitud { get; init; }
    public double? Longitud { get; init; }

    /// <summary>Opcional: si viene, reemplaza la fotografía actual.</summary>
    public ArchivoSubido? Archivo { get; init; }
}

public sealed record ContactoDto(
    int Id,
    string Nombre,
    string Telefono,
    double Latitud,
    double Longitud,
    string TipoMime,
    long TamanoBytes,
    string ArchivoUrl,
    DateTime FechaCreacion);

public sealed record ArchivoDescargable(Stream Contenido, string TipoMime, string NombreSugerido);

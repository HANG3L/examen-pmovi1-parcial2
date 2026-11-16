namespace PM2E1Grupo1.Domain.Entidades;

/// <summary>
/// Contacto con su fotografía. La imagen vive en el disco del servidor;
/// la base de datos solo guarda la ruta, el MIME type y el tamaño.
/// </summary>
public class Contacto
{
    public int Id { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public string Telefono { get; set; } = string.Empty;
    public double Latitud { get; set; }
    public double Longitud { get; set; }

    /// <summary>Ruta relativa del archivo dentro de la carpeta de almacenamiento.</summary>
    public string RutaArchivo { get; set; } = string.Empty;
    public string TipoMime { get; set; } = string.Empty;
    public long TamanoBytes { get; set; }

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;
}

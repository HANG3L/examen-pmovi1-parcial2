namespace PM2E1Grupo1.Application.Interfaces;

/// <summary>Guarda y lee los archivos subidos (el almacenamiento real es detalle de infraestructura).</summary>
public interface IAlmacenArchivos
{
    /// <summary>Guarda el contenido y devuelve la ruta relativa donde quedó.</summary>
    Task<string> GuardarAsync(Stream contenido, string extension, CancellationToken ct = default);

    /// <summary>Abre el archivo para lectura; null si no existe.</summary>
    Stream? Abrir(string rutaRelativa);

    void Eliminar(string rutaRelativa);
}

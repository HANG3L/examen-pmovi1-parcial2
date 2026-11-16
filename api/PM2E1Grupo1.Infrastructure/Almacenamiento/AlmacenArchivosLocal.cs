using PM2E1Grupo1.Application.Interfaces;

namespace PM2E1Grupo1.Infrastructure.Almacenamiento;

/// <summary>Guarda los archivos en una carpeta del disco del servidor con nombres únicos (GUID).</summary>
public class AlmacenArchivosLocal : IAlmacenArchivos
{
    private readonly string _carpetaRaiz;

    public AlmacenArchivosLocal(string carpetaRaiz)
    {
        _carpetaRaiz = Path.GetFullPath(carpetaRaiz);
        Directory.CreateDirectory(_carpetaRaiz);
    }

    public async Task<string> GuardarAsync(Stream contenido, string extension, CancellationToken ct = default)
    {
        var nombre = $"{Guid.NewGuid():N}{extension}";
        var rutaCompleta = Path.Combine(_carpetaRaiz, nombre);

        await using var destino = new FileStream(rutaCompleta, FileMode.CreateNew, FileAccess.Write, FileShare.None);
        await contenido.CopyToAsync(destino, ct);

        return nombre;
    }

    public Stream? Abrir(string rutaRelativa)
    {
        var ruta = ResolverSeguro(rutaRelativa);
        return ruta is not null && File.Exists(ruta)
            ? new FileStream(ruta, FileMode.Open, FileAccess.Read, FileShare.Read)
            : null;
    }

    public void Eliminar(string rutaRelativa)
    {
        var ruta = ResolverSeguro(rutaRelativa);
        if (ruta is not null && File.Exists(ruta))
            File.Delete(ruta);
    }

    /// <summary>Evita salir de la carpeta de almacenamiento (path traversal).</summary>
    private string? ResolverSeguro(string rutaRelativa)
    {
        var completa = Path.GetFullPath(Path.Combine(_carpetaRaiz, rutaRelativa));
        return completa.StartsWith(_carpetaRaiz + Path.DirectorySeparatorChar, StringComparison.Ordinal)
            ? completa
            : null;
    }
}

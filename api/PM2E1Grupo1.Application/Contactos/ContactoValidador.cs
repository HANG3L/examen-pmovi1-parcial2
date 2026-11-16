using System.Text.RegularExpressions;
using PM2E1Grupo1.Application.Excepciones;

namespace PM2E1Grupo1.Application.Contactos;

/// <summary>Reglas de validación del servidor (la app también valida, pero el servidor es quien manda).</summary>
public static partial class ContactoValidador
{
    public const string MimePermitido = "image/jpeg";
    public const long TamanoMaximoBytes = 5 * 1024 * 1024; // 5 MB

    [GeneratedRegex(@"^[\p{L}][\p{L} ]*$")]
    private static partial Regex PatronNombre();

    [GeneratedRegex(@"^\d{8}$")]
    private static partial Regex PatronTelefono();

    public static (string nombre, string telefono, double latitud, double longitud) ValidarDatos(
        string? nombre, string? telefono, double? latitud, double? longitud)
    {
        var errores = new List<string>();

        nombre = nombre?.Trim();
        telefono = telefono?.Trim();

        if (string.IsNullOrWhiteSpace(nombre))
            errores.Add("El nombre es obligatorio.");
        else if (nombre.Length > 100)
            errores.Add("El nombre no puede superar 100 caracteres.");
        else if (!PatronNombre().IsMatch(nombre))
            errores.Add("El nombre solo puede contener letras y espacios.");

        if (string.IsNullOrWhiteSpace(telefono))
            errores.Add("El teléfono es obligatorio.");
        else if (!PatronTelefono().IsMatch(telefono))
            errores.Add("El teléfono debe tener exactamente 8 dígitos.");

        if (latitud is null)
            errores.Add("La latitud es obligatoria.");
        else if (latitud < -90 || latitud > 90)
            errores.Add("La latitud debe estar entre -90 y 90.");

        if (longitud is null)
            errores.Add("La longitud es obligatoria.");
        else if (longitud < -180 || longitud > 180)
            errores.Add("La longitud debe estar entre -180 y 180.");

        if (errores.Count > 0)
            throw new ValidacionException(errores);

        return (nombre!, telefono!, latitud!.Value, longitud!.Value);
    }

    /// <summary>
    /// Valida tamaño (400) y MIME type (415). Además comprueba la firma real del JPEG
    /// para que no se pueda subir otro formato solo cambiando el Content-Type.
    /// </summary>
    public static void ValidarArchivo(ArchivoSubido archivo)
    {
        if (archivo.Tamano <= 0)
            throw new ValidacionException("El archivo está vacío.");

        if (archivo.Tamano > TamanoMaximoBytes)
            throw new ValidacionException(
                $"El archivo supera el tamaño máximo de {TamanoMaximoBytes / (1024 * 1024)} MB.");

        if (!string.Equals(archivo.TipoMime, MimePermitido, StringComparison.OrdinalIgnoreCase))
            throw new TipoMediaNoSoportadoException(
                $"Tipo de archivo no permitido ({archivo.TipoMime}). Solo se acepta {MimePermitido}.");

        if (!EsJpegReal(archivo.Contenido))
            throw new TipoMediaNoSoportadoException(
                "El contenido del archivo no es una imagen JPEG válida.");
    }

    private static bool EsJpegReal(Stream contenido)
    {
        if (!contenido.CanSeek) return true;

        var inicio = contenido.Position;
        var cabecera = new byte[3];
        var leidos = contenido.Read(cabecera, 0, 3);
        contenido.Position = inicio;

        return leidos == 3 && cabecera[0] == 0xFF && cabecera[1] == 0xD8 && cabecera[2] == 0xFF;
    }
}

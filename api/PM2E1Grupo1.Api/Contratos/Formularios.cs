using Microsoft.AspNetCore.Mvc;

namespace PM2E1Grupo1.Api.Contratos;

/// <summary>Cuerpo multipart/form-data de POST /api/contactos.</summary>
public class CrearContactoFormulario
{
    /// <example>Jose de Jesus Sarmiento</example>
    [FromForm(Name = "nombre")] public string? Nombre { get; set; }

    /// <example>99765645</example>
    [FromForm(Name = "telefono")] public string? Telefono { get; set; }

    /// <example>14.48</example>
    [FromForm(Name = "latitud")] public double? Latitud { get; set; }

    /// <example>-88.78</example>
    [FromForm(Name = "longitud")] public double? Longitud { get; set; }

    /// <summary>Fotografía image/jpeg, máximo 5 MB.</summary>
    [FromForm(Name = "archivo")] public IFormFile? Archivo { get; set; }
}

/// <summary>Cuerpo multipart/form-data de PUT /api/contactos/{id}. El archivo es opcional.</summary>
public class ActualizarContactoFormulario
{
    [FromForm(Name = "nombre")] public string? Nombre { get; set; }
    [FromForm(Name = "telefono")] public string? Telefono { get; set; }
    [FromForm(Name = "latitud")] public double? Latitud { get; set; }
    [FromForm(Name = "longitud")] public double? Longitud { get; set; }

    /// <summary>Si se envía, reemplaza la fotografía actual (image/jpeg, máximo 5 MB).</summary>
    [FromForm(Name = "archivo")] public IFormFile? Archivo { get; set; }
}

/// <summary>Formato de error JSON que la app Android muestra al usuario.</summary>
public sealed record ErrorRespuesta(string Mensaje, IReadOnlyList<string> Errores);

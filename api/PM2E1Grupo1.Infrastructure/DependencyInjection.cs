using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using PM2E1Grupo1.Application.Interfaces;
using PM2E1Grupo1.Infrastructure.Almacenamiento;
using PM2E1Grupo1.Infrastructure.Persistencia;

namespace PM2E1Grupo1.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AgregarInfraestructura(
        this IServiceCollection servicios, IConfiguration configuracion, string carpetaBase)
    {
        var cadena = configuracion.GetConnectionString("Contactos")
                     ?? $"Data Source={Path.Combine(carpetaBase, "contactos.db")}";
        var carpetaArchivos = configuracion["Almacenamiento:Carpeta"] ?? "uploads";

        // Las rutas relativas se resuelven contra la carpeta de la aplicación, no contra el directorio actual.
        if (!Path.IsPathRooted(carpetaArchivos))
            carpetaArchivos = Path.Combine(carpetaBase, carpetaArchivos);

        servicios.AddDbContext<AppDbContext>(o => o.UseSqlite(cadena));
        servicios.AddScoped<IContactoRepositorio, ContactoRepositorio>();
        servicios.AddSingleton<IAlmacenArchivos>(_ => new AlmacenArchivosLocal(carpetaArchivos));

        return servicios;
    }
}

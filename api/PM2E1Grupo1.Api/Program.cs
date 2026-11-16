using System.Globalization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi;
using PM2E1Grupo1.Api.Contratos;
using PM2E1Grupo1.Api.Middleware;
using PM2E1Grupo1.Application.Contactos;
using PM2E1Grupo1.Infrastructure;
using PM2E1Grupo1.Infrastructure.Persistencia;

// Los decimales de latitud/longitud llegan con punto ("14.48") sin importar el idioma del servidor.
CultureInfo.DefaultThreadCurrentCulture = CultureInfo.InvariantCulture;
CultureInfo.DefaultThreadCurrentUICulture = CultureInfo.InvariantCulture;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AgregarInfraestructura(builder.Configuration, builder.Environment.ContentRootPath);
builder.Services.AddScoped<ContactoServicio>();

builder.Services
    .AddControllers(opciones =>
    {
        var mensajes = opciones.ModelBindingMessageProvider;
        mensajes.SetAttemptedValueIsInvalidAccessor((valor, campo) => $"El valor '{valor}' no es válido para {campo.ToLowerInvariant()}.");
        mensajes.SetValueIsInvalidAccessor(valor => $"El valor '{valor}' no es válido.");
        mensajes.SetUnknownValueIsInvalidAccessor(campo => $"El valor enviado no es válido para {campo.ToLowerInvariant()}.");
        mensajes.SetValueMustBeANumberAccessor(campo => $"El campo {campo.ToLowerInvariant()} debe ser un número.");
    })
    .ConfigureApiBehaviorOptions(opciones =>
    {
        // Errores de binding (p. ej. latitud = "abc") con el mismo formato JSON que el resto.
        opciones.InvalidModelStateResponseFactory = contexto =>
        {
            var errores = contexto.ModelState.Values
                .SelectMany(v => v.Errors)
                .Select(e => string.IsNullOrWhiteSpace(e.ErrorMessage) ? "Valor inválido." : e.ErrorMessage)
                .ToList();
            return new BadRequestObjectResult(new ErrorRespuesta("Los datos enviados no son válidos.", errores));
        };
    });

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(opciones =>
{
    opciones.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "PM2E1Grupo1 API",
        Version = "v1",
        Description = "API REST de contactos con fotografía (image/jpeg, máximo 5 MB). " +
                      "Examen segundo parcial · Programación Móvil I · Grupo 1."
    });

    var xml = Path.Combine(AppContext.BaseDirectory, $"{typeof(Program).Assembly.GetName().Name}.xml");
    if (File.Exists(xml)) opciones.IncludeXmlComments(xml);
});

var app = builder.Build();

// Crea la carpeta de datos y la base de datos SQLite si no existen.
Directory.CreateDirectory(Path.Combine(app.Environment.ContentRootPath, "App_Data"));
using (var alcance = app.Services.CreateScope())
{
    alcance.ServiceProvider.GetRequiredService<AppDbContext>().Database.EnsureCreated();
}

app.UseMiddleware<ManejadorErroresMiddleware>();

// Swagger siempre activo: es parte de lo que se evalúa y la API solo corre en local.
app.UseSwagger();
app.UseSwaggerUI(o =>
{
    o.SwaggerEndpoint("/swagger/v1/swagger.json", "PM2E1Grupo1 API v1");
    o.RoutePrefix = "swagger";
});

app.MapGet("/", () => Results.Redirect("/swagger")).ExcludeFromDescription();
app.MapControllers();

app.Run();

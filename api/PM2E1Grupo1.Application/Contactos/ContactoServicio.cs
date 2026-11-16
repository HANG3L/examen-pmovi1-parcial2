using PM2E1Grupo1.Application.Excepciones;
using PM2E1Grupo1.Application.Interfaces;
using PM2E1Grupo1.Domain.Entidades;

namespace PM2E1Grupo1.Application.Contactos;

public class ContactoServicio
{
    private readonly IContactoRepositorio _repositorio;
    private readonly IAlmacenArchivos _almacen;

    public ContactoServicio(IContactoRepositorio repositorio, IAlmacenArchivos almacen)
    {
        _repositorio = repositorio;
        _almacen = almacen;
    }

    public async Task<List<ContactoDto>> ListarAsync(string? buscar, CancellationToken ct = default)
    {
        var contactos = await _repositorio.ListarAsync(buscar?.Trim(), ct);
        return contactos.Select(ADto).ToList();
    }

    public async Task<ContactoDto> ObtenerAsync(int id, CancellationToken ct = default)
        => ADto(await ObtenerOFallarAsync(id, ct));

    public async Task<ContactoDto> CrearAsync(CrearContactoComando comando, CancellationToken ct = default)
    {
        var (nombre, telefono, latitud, longitud) =
            ContactoValidador.ValidarDatos(comando.Nombre, comando.Telefono, comando.Latitud, comando.Longitud);

        if (comando.Archivo is null)
            throw new ValidacionException("Debe adjuntar la fotografía en el campo 'archivo'.");

        ContactoValidador.ValidarArchivo(comando.Archivo);

        var ruta = await _almacen.GuardarAsync(comando.Archivo.Contenido, ".jpg", ct);

        var contacto = new Contacto
        {
            Nombre = nombre,
            Telefono = telefono,
            Latitud = latitud,
            Longitud = longitud,
            RutaArchivo = ruta,
            TipoMime = ContactoValidador.MimePermitido,
            TamanoBytes = comando.Archivo.Tamano
        };

        try
        {
            await _repositorio.AgregarAsync(contacto, ct);
        }
        catch
        {
            _almacen.Eliminar(ruta); // no dejar archivos huérfanos si falla la BD
            throw;
        }

        return ADto(contacto);
    }

    public async Task<ContactoDto> ActualizarAsync(int id, ActualizarContactoComando comando, CancellationToken ct = default)
    {
        var contacto = await ObtenerOFallarAsync(id, ct);

        var (nombre, telefono, latitud, longitud) =
            ContactoValidador.ValidarDatos(comando.Nombre, comando.Telefono, comando.Latitud, comando.Longitud);

        string? rutaAnterior = null;

        if (comando.Archivo is not null)
        {
            ContactoValidador.ValidarArchivo(comando.Archivo);

            rutaAnterior = contacto.RutaArchivo;
            contacto.RutaArchivo = await _almacen.GuardarAsync(comando.Archivo.Contenido, ".jpg", ct);
            contacto.TipoMime = ContactoValidador.MimePermitido;
            contacto.TamanoBytes = comando.Archivo.Tamano;
        }

        contacto.Nombre = nombre;
        contacto.Telefono = telefono;
        contacto.Latitud = latitud;
        contacto.Longitud = longitud;

        await _repositorio.GuardarCambiosAsync(ct);

        if (rutaAnterior is not null)
            _almacen.Eliminar(rutaAnterior);

        return ADto(contacto);
    }

    public async Task EliminarAsync(int id, CancellationToken ct = default)
    {
        var contacto = await ObtenerOFallarAsync(id, ct);
        await _repositorio.EliminarAsync(contacto, ct);
        _almacen.Eliminar(contacto.RutaArchivo);
    }

    public async Task<ArchivoDescargable> ObtenerArchivoAsync(int id, CancellationToken ct = default)
    {
        var contacto = await ObtenerOFallarAsync(id, ct);

        var contenido = _almacen.Abrir(contacto.RutaArchivo)
            ?? throw new NoEncontradoException("El archivo del contacto no existe en el servidor.");

        return new ArchivoDescargable(contenido, contacto.TipoMime, $"contacto-{contacto.Id}.jpg");
    }

    private async Task<Contacto> ObtenerOFallarAsync(int id, CancellationToken ct)
        => await _repositorio.ObtenerAsync(id, ct)
           ?? throw new NoEncontradoException($"No existe el contacto con id {id}.");

    private static ContactoDto ADto(Contacto c) => new(
        c.Id, c.Nombre, c.Telefono, c.Latitud, c.Longitud,
        c.TipoMime, c.TamanoBytes, $"/api/contactos/{c.Id}/archivo", c.FechaCreacion);
}

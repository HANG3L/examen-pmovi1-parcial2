using PM2E1Grupo1.Domain.Entidades;

namespace PM2E1Grupo1.Application.Interfaces;

public interface IContactoRepositorio
{
    Task<List<Contacto>> ListarAsync(string? buscar, CancellationToken ct = default);
    Task<Contacto?> ObtenerAsync(int id, CancellationToken ct = default);
    Task AgregarAsync(Contacto contacto, CancellationToken ct = default);
    Task GuardarCambiosAsync(CancellationToken ct = default);
    Task EliminarAsync(Contacto contacto, CancellationToken ct = default);
}

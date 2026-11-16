using Microsoft.EntityFrameworkCore;
using PM2E1Grupo1.Application.Interfaces;
using PM2E1Grupo1.Domain.Entidades;

namespace PM2E1Grupo1.Infrastructure.Persistencia;

public class ContactoRepositorio : IContactoRepositorio
{
    private readonly AppDbContext _db;

    public ContactoRepositorio(AppDbContext db) => _db = db;

    public Task<List<Contacto>> ListarAsync(string? buscar, CancellationToken ct = default)
    {
        var consulta = _db.Contactos.AsNoTracking();

        if (!string.IsNullOrWhiteSpace(buscar))
        {
            // Escapa los comodines de LIKE para que '%' o '_' se busquen como texto normal.
            var patron = "%" + buscar.Replace("\\", "\\\\").Replace("%", "\\%").Replace("_", "\\_") + "%";
            consulta = consulta.Where(c =>
                EF.Functions.Like(c.Nombre, patron, "\\") ||
                EF.Functions.Like(c.Telefono, patron, "\\"));
        }

        return consulta.OrderBy(c => c.Nombre).ThenBy(c => c.Id).ToListAsync(ct);
    }

    public Task<Contacto?> ObtenerAsync(int id, CancellationToken ct = default)
        => _db.Contactos.FirstOrDefaultAsync(c => c.Id == id, ct);

    public async Task AgregarAsync(Contacto contacto, CancellationToken ct = default)
    {
        _db.Contactos.Add(contacto);
        await _db.SaveChangesAsync(ct);
    }

    public Task GuardarCambiosAsync(CancellationToken ct = default) => _db.SaveChangesAsync(ct);

    public async Task EliminarAsync(Contacto contacto, CancellationToken ct = default)
    {
        _db.Contactos.Remove(contacto);
        await _db.SaveChangesAsync(ct);
    }
}

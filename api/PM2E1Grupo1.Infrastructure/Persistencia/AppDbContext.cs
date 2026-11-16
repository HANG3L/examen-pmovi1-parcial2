using Microsoft.EntityFrameworkCore;
using PM2E1Grupo1.Domain.Entidades;

namespace PM2E1Grupo1.Infrastructure.Persistencia;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<Contacto> Contactos => Set<Contacto>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Contacto>(e =>
        {
            e.ToTable("contactos");
            e.HasKey(c => c.Id);
            e.Property(c => c.Nombre).IsRequired().HasMaxLength(100);
            e.Property(c => c.Telefono).IsRequired().HasMaxLength(8);
            e.Property(c => c.RutaArchivo).IsRequired().HasMaxLength(260);
            e.Property(c => c.TipoMime).IsRequired().HasMaxLength(50);
        });
    }
}

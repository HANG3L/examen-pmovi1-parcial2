namespace PM2E1Grupo1.Application.Excepciones;

/// <summary>Datos inválidos: se responde 400.</summary>
public class ValidacionException : Exception
{
    public IReadOnlyList<string> Errores { get; }

    public ValidacionException(string mensaje) : base(mensaje)
    {
        Errores = new[] { mensaje };
    }

    public ValidacionException(IEnumerable<string> errores)
        : base(string.Join(" ", errores))
    {
        Errores = errores.ToList();
    }
}

/// <summary>El archivo no es image/jpeg: se responde 415.</summary>
public class TipoMediaNoSoportadoException : Exception
{
    public TipoMediaNoSoportadoException(string mensaje) : base(mensaje) { }
}

/// <summary>El contacto o su archivo no existe: se responde 404.</summary>
public class NoEncontradoException : Exception
{
    public NoEncontradoException(string mensaje) : base(mensaje) { }
}

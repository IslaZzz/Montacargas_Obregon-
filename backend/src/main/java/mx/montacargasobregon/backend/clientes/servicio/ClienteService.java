package mx.montacargasobregon.backend.clientes.servicio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.dto.ClienteRequest;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.clientes.repositorio.ClienteRepository;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.comun.dto.DireccionRequest;

/**
 * Reglas de negocio de los clientes.
 */
@Service
public class ClienteService {

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra un cliente con sus razones sociales
     **/
    @Transactional // Si algo falla a la mitad, se deshace todo y no queda nada guardado
    public Cliente registrar(ClienteRequest datos) {
        // Crea la entidad con los datos de contacto recibidos
        Cliente cliente = new Cliente(
                datos.nombre(), datos.apellidoPaterno(), datos.apellidoMaterno(),
                datos.empresa(), datos.telefono(), datos.correo());

        // Convierte cada razón social recibida en una entidad y la liga al cliente
        datos.razonesSociales().forEach(r -> {
            // Se crea la dirección fiscal con los datos recibidos
            DireccionRequest d = r.direccionFiscal();
            DireccionFiscal direccion = new DireccionFiscal(
                    d.calle(), d.numero(), d.colonia(), d.ciudad(), d.codigoPostal());

            // Se crea la razón social, el RFC se guarda siempre en mayúsculas
            cliente.agregarRazonSocial(
                    new RazonSocial(r.nombre(), r.rfc().toUpperCase(), direccion));
        });

        // Guarda el cliente y, por el cascade PERSIST, también sus razones sociales
        // y la dirección fiscal de cada una
        return repositorio.save(cliente);
    }

    /** Busca clientes por texto. Sin texto, devuelve todos. */
    @Transactional(readOnly = true) // Solo lectura, no modifica datos
    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return repositorio.findAll();
        }
        return repositorio.buscar(texto.trim());
    }

    /** Obtiene un cliente por su id; si no existe, responde 404. */
    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

}
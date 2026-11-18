package mx.montacargasobregon.backend.equipos.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.equipos.dto.ModeloRequest;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.CategoriaRepository;
import mx.montacargasobregon.backend.equipos.repositorio.DefinicionCampoRepository;
import mx.montacargasobregon.backend.equipos.repositorio.ModeloRepository;

/** Registro y edición de los modelos de equipo con sus especificaciones (HU-36). */
@Service
public class ModeloService {

    static final String OBLIGATORIO = "Este dato es obligatorio";
    static final String NOMBRE_REPETIDO = "Ya existe un modelo con ese nombre";
    static final String MARCA_MODELO_REPETIDO = "Ya existe un modelo con esa marca y modelo";
    static final String MARCA_MODELO_INCOMPLETO = "Escribe la marca y el modelo, por ejemplo Toyota 8FBE30";

    private final ModeloRepository repositorio;
    private final CategoriaRepository categorias;
    private final DefinicionCampoRepository campos;

    public ModeloService(ModeloRepository repositorio, CategoriaRepository categorias,
                         DefinicionCampoRepository campos) {
        this.repositorio = repositorio;
        this.categorias = categorias;
        this.campos = campos;
    }

    /** Todos los modelos en el orden en que se registraron: el nuevo queda al final. */
    @Transactional(readOnly = true)
    public List<Modelo> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Modelo obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));
    }

    /** Campos de especificación que pide la categoría, en el orden del formulario. */
    @Transactional(readOnly = true)
    public List<DefinicionCampo> camposDe(Long categoriaId) {
        return campos.findByCategoriaIdOrderByOrden(categoriaId);
    }

    /** Registra un modelo sin tarifa; la tarifa se captura después en «Tarifas» (HU-04). */
    @Transactional
    public Modelo crear(ModeloRequest datos) {
        DatosLimpios limpios = revisar(datos);

        Map<String, String> repetidos = new LinkedHashMap<>();
        if (repositorio.existsByNombreIgnoreCase(limpios.nombre())) {
            repetidos.put("nombre", NOMBRE_REPETIDO);
        }
        if (repositorio.existsByCodigoIgnoreCase(limpios.codigo())) {
            repetidos.put("marcaModelo", MARCA_MODELO_REPETIDO);
        }
        rechazarSiHay(HttpStatus.CONFLICT, repetidos);

        Modelo modelo = new Modelo(limpios.nombre(), limpios.marca(), limpios.codigo(),
                limpios.descripcion(), null, limpios.categoria());
        modelo.cambiarEspecificaciones(limpios.especificaciones());
        return repositorio.save(modelo);
    }

    /** Cambia los datos del modelo; sus unidades los muestran porque apuntan a él. */
    @Transactional
    public Modelo actualizar(Long id, ModeloRequest datos) {
        Modelo modelo = obtener(id);
        DatosLimpios limpios = revisar(datos);

        Map<String, String> repetidos = new LinkedHashMap<>();
        if (repositorio.existsByNombreIgnoreCaseAndIdNot(limpios.nombre(), id)) {
            repetidos.put("nombre", NOMBRE_REPETIDO);
        }
        if (repositorio.existsByCodigoIgnoreCaseAndIdNot(limpios.codigo(), id)) {
            repetidos.put("marcaModelo", MARCA_MODELO_REPETIDO);
        }
        rechazarSiHay(HttpStatus.CONFLICT, repetidos);

        modelo.actualizar(limpios.nombre(), limpios.marca(), limpios.codigo(),
                limpios.descripcion(), limpios.categoria());
        modelo.cambiarEspecificaciones(limpios.especificaciones());
        return modelo;
    }

    private record DatosLimpios(Categoria categoria, String nombre, String marca, String codigo,
                                String descripcion, Map<DefinicionCampo, String> especificaciones) {
    }

    /** Quita espacios sobrantes, separa marca y código y revisa cada especificación. */
    private DatosLimpios revisar(ModeloRequest datos) {
        Map<String, String> errores = new LinkedHashMap<>();

        Categoria categoria = categorias.findById(datos.categoriaId()).orElse(null);
        if (categoria == null) {
            errores.put("categoriaId", "Elige una categoría de la lista");
        }

        String nombre = datos.nombre().trim().replaceAll("\\s+", " ");

        // "Toyota 8FBE30": la marca es la primera palabra y el código, lo demás
        String marcaModelo = datos.marcaModelo().trim().replaceAll("\\s+", " ");
        int espacio = marcaModelo.indexOf(' ');
        String marca = espacio > 0 ? marcaModelo.substring(0, espacio) : marcaModelo;
        String codigo = espacio > 0 ? marcaModelo.substring(espacio + 1) : "";
        if (codigo.isEmpty()) {
            errores.put("marcaModelo", MARCA_MODELO_INCOMPLETO);
        } else if (codigo.length() > 50) {
            errores.put("marcaModelo", "El modelo admite máximo 50 caracteres después de la marca");
        }

        String descripcion = datos.descripcion() == null || datos.descripcion().isBlank()
                ? null
                : datos.descripcion().trim();

        Map<DefinicionCampo, String> especificaciones = new LinkedHashMap<>();
        if (categoria != null) {
            Map<Long, String> valores = datos.especificaciones() == null ? Map.of() : datos.especificaciones();
            for (DefinicionCampo campo : campos.findByCategoriaIdOrderByOrden(categoria.getId())) {
                String texto = valores.get(campo.getId());
                String llave = "especificaciones." + campo.getId();
                if (texto == null || texto.isBlank()) {
                    if (campo.isRequerido()) {
                        errores.put(llave, OBLIGATORIO);
                    }
                    continue;
                }
                try {
                    especificaciones.put(campo, normalizar(campo, texto.trim()));
                } catch (IllegalArgumentException ex) {
                    errores.put(llave, ex.getMessage());
                }
            }
        }

        rechazarSiHay(HttpStatus.BAD_REQUEST, errores);
        return new DatosLimpios(categoria, nombre, marca, codigo, descripcion, especificaciones);
    }

    /** Revisa que el valor sea del tipo del campo y lo deja en un formato fijo, como "3000" o "4.5". */
    static String normalizar(DefinicionCampo campo, String texto) {
        return switch (campo.getTipo()) {
            case TEXTO -> {
                if (texto.length() > 120) {
                    throw new IllegalArgumentException("Admite máximo 120 caracteres");
                }
                yield texto;
            }
            case ENTERO -> {
                String sinComas = texto.replace(",", "");
                if (!sinComas.matches("\\d{1,9}")) {
                    throw new IllegalArgumentException("Escribe un número entero, por ejemplo 3000");
                }
                yield String.valueOf(Long.parseLong(sinComas));
            }
            case DECIMAL -> {
                String sinComas = texto.replace(",", "");
                if (!sinComas.matches("\\d{1,9}(\\.\\d{1,3})?")) {
                    throw new IllegalArgumentException("Escribe un número, por ejemplo 4.5");
                }
                yield new BigDecimal(sinComas).stripTrailingZeros().toPlainString();
            }
            case BOOLEANO -> {
                if (!texto.equals("Sí") && !texto.equals("No")) {
                    throw new IllegalArgumentException("Elige Sí o No");
                }
                yield texto;
            }
            case FECHA -> {
                try {
                    yield LocalDate.parse(texto).toString();
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Escribe una fecha válida");
                }
            }
            case LISTA -> {
                if (!campo.getOpciones().contains(texto)) {
                    throw new IllegalArgumentException("Elige una opción de la lista");
                }
                yield texto;
            }
        };
    }

    private static void rechazarSiHay(HttpStatus estado, Map<String, String> errores) {
        if (!errores.isEmpty()) {
            throw new DatosModeloException(estado, errores);
        }
    }
}

package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.persistence.data.Detalle_CarritoRepository;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;
import es.uca.orderflow.persistence.data.Producto_IngredienteRepository;
import es.uca.orderflow.presentation.dto.ProductoCardDTO;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GestionarProducto {

    private final ProductoRepository productoRepository;
    private final Producto_IngredienteRepository productoIngredienteRepository;
    private final Detalle_CarritoRepository detalleCarritoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;

    public GestionarProducto(ProductoRepository productoRepository,
                             Producto_IngredienteRepository productoIngredienteRepository,
                             Detalle_PedidoRepository detallePedidoRepository,
                             Detalle_CarritoRepository detalleCarritoRepository) {
        this.productoRepository = productoRepository;
        this.productoIngredienteRepository = productoIngredienteRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    public Producto crearProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    @Cacheable(cacheNames = "catalogo", key = "'cards:' + #q + ':' + #page + ':' + #size + ':' + #sort")
    public Page<ProductoCardDTO> catalogoCards(String q, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return productoRepository.findCatalogoCards(q, pageable);
    }

    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Transactional
    public Producto actualizarProducto(Producto producto, List<Producto_Ingrediente> relaciones) {

        if (producto.getId() == null || !productoRepository.existsById(producto.getId())) {
            throw new IllegalArgumentException("No existe el producto con id: " + producto.getId());
        }

        try {
            Producto actualizado = productoRepository.save(producto);

            // eliminar relaciones antiguas
            productoIngredienteRepository.hardDeleteByProductoId(actualizado.getId());
            productoIngredienteRepository.flush();

            Map<Long, Producto_Ingrediente> porIngrediente = new LinkedHashMap<>();
            for (Producto_Ingrediente pi : relaciones) {
                if (pi.getIngrediente() == null || pi.getIngrediente().getId() == null) {
                    throw new IllegalArgumentException("Ingrediente inválido en la fila.");
                }
                pi.setId(null);
                pi.setProducto(actualizado);
                porIngrediente.put(pi.getIngrediente().getId(), pi);
            }

            List<Producto_Ingrediente> aGuardar = new ArrayList<>(porIngrediente.values());
            if (aGuardar.isEmpty()) throw new IllegalArgumentException("Añade al menos un ingrediente.");

            productoIngredienteRepository.saveAll(aGuardar);
            return actualizado;

        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException(
                    "El producto ha sido modificado por otro dueño. Recarga la página y vuelve a intentarlo."
            );
        }
    }

    public List<Producto> consultarProductos() {
        return productoRepository.findAll();
    }


    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Transactional
    public void eliminarProducto(Long productoId) {

        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto con id: " + productoId));

        // 1) Referencias históricas (detalle_pedido) -> NUNCA hard delete
        long refsPedidos = detallePedidoRepository.countByProductoIdNative(productoId);

        // 2) Usado como componente en menús -> NUNCA hard delete
        long refsMenu = productoRepository.countUsosComoComponenteEnMenu(productoId);

        if (refsPedidos > 0 || refsMenu > 0) {
            archivarProducto(p);
            return;
        }

        // Limpiamos dependencias no históricas
        detalleCarritoRepository.deleteByProducto_Id(productoId);
        productoIngredienteRepository.hardDeleteByProductoId(productoId);
        productoIngredienteRepository.flush();

        // Hard delete
        productoRepository.delete(p);
        productoRepository.flush();
    }

    private void archivarProducto(Producto p) {
        p.setActivo(false);
        p.setStock(0);
        productoRepository.save(p);

        if (p.getId() != null) {
            detalleCarritoRepository.deleteByProducto_Id(p.getId());
        }
    }


    public Producto guardarProducto_Ingrediente(Producto producto, List<Producto_Ingrediente> relaciones) {
        Producto p = productoRepository.save(producto);
        productoIngredienteRepository.saveAll(relaciones);
        return p;
    }

    public Producto buscarProductoPorId(Long idProducto) {
        return productoRepository.findById(idProducto).orElse(null);
    }

    @Cacheable(cacheNames = "catalogo", key = "'topProductos10'")
    public List<ProductoCardDTO> topProductosHome10() {
        return productoRepository
                .findTop10ByActivoTrueAndTipoNotOrderByLastModifiedDateDesc(ProductoTipo.MENU)
                .stream()
                .map(p -> new ProductoCardDTO(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getFoto(), p.getTipo()))
                .toList();
    }

    @Cacheable(cacheNames = "catalogo", key = "'topMenus10'")
    public List<ProductoCardDTO> topMenusHome10() {
        return productoRepository
                .findTop10ByActivoTrueAndTipoOrderByLastModifiedDateDesc(ProductoTipo.MENU)
                .stream()
                .map(p -> new ProductoCardDTO(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getFoto(), p.getTipo()))
                .toList();
    }

    public List<Producto_Ingrediente> encontrarIngredientesPorProductoId(Long idProducto) {
        return productoIngredienteRepository.findByProductoIdWithIngrediente(idProducto);
    }

    public List<Producto> consultarSoloProductos() {
        return productoRepository.findAll().stream()
                .filter(Producto::isActivo)
                .filter(p -> p.getTipo() == null || p.getTipo() != ProductoTipo.MENU)
                .toList();
    }

    public List<Producto> consultarCartaCompleta() {
        return productoRepository.findAll();
    }
}

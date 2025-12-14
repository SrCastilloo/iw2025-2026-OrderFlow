package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.MenuComposicionRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GestionarMenu {

    private final ProductoRepository productoRepository;
    private final MenuComposicionRepository menuComposicionRepository;

    @Transactional
    public Producto crearMenu(String nombre,
                              String descripcion,
                              BigDecimal precio,
                              String foto,
                              Map<Long, Integer> productosConCantidad) {

        if (productosConCantidad == null || productosConCantidad.isEmpty()) {
            throw new IllegalArgumentException("Un menú debe tener al menos 1 producto.");
        }

        Producto menu = new Producto();
        menu.setNombre(nombre);
        menu.setDescripcion(descripcion);
        menu.setPrecio(precio);
        menu.setFoto(foto);
        menu.setStock(999999); // o el criterio que uses
        menu.setTipo(ProductoTipo.MENU);

        menu = productoRepository.save(menu);

        for (var e : productosConCantidad.entrySet()) {
            Long prodId = e.getKey();
            Integer qty = e.getValue() == null ? 1 : e.getValue();

            Producto prod = productoRepository.findById(prodId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + prodId));

            MenuComposicion mc = new MenuComposicion();
            mc.setMenuProducto(menu);
            mc.setProducto(prod);
            mc.setCantidad(Math.max(1, qty));
            menuComposicionRepository.save(mc);
        }

        return menu;
    }

    public List<MenuComposicion> composicion(Long menuProductoId) {
        return menuComposicionRepository.findByMenuProducto_Id(menuProductoId);
    }
    /** NUEVO: listar todos los menús (Producto tipo MENU) */
    public List<Producto> listarMenus() {
        return productoRepository.findByTipo(ProductoTipo.MENU);
    }

    /** NUEVO: obtener un menú por id (debe ser tipo MENU) */
    public Producto obtenerMenu(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el menú: " + id));
        if (p.getTipo() != ProductoTipo.MENU) {
            throw new IllegalArgumentException("El producto " + id + " no es un menú.");
        }
        return p;
    }

    /** NUEVO: actualizar menú y su composición */
    @Transactional
    public Producto actualizarMenu(Long menuId,
                                   String nombre,
                                   String descripcion,
                                   BigDecimal precio,
                                   String foto,
                                   Map<Long, Integer> productosConCantidad) {

        if (productosConCantidad == null || productosConCantidad.isEmpty()) {
            throw new IllegalArgumentException("Un menú debe tener al menos 1 producto.");
        }

        Producto menu = obtenerMenu(menuId);
        menu.setNombre(nombre);
        menu.setDescripcion(descripcion);
        menu.setPrecio(precio);
        menu.setFoto(foto);

        menu = productoRepository.save(menu);

        // borra composición anterior y guarda la nueva
        menuComposicionRepository.deleteByMenuProducto_Id(menuId);
        menuComposicionRepository.flush();

        guardarComposicion(menu, productosConCantidad);
        return menu;
    }

    /** NUEVO: eliminar menú (incluye composición) */
    @Transactional
    public void eliminarMenu(Long menuId) {
        Producto menu = obtenerMenu(menuId);
        menuComposicionRepository.deleteByMenuProducto_Id(menuId);
        menuComposicionRepository.flush();
        productoRepository.delete(menu);
    }

    private void guardarComposicion(Producto menu, Map<Long, Integer> productosConCantidad) {
        for (var e : productosConCantidad.entrySet()) {
            Long prodId = e.getKey();
            Integer qty = e.getValue() == null ? 1 : e.getValue();

            Producto prod = productoRepository.findById(prodId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + prodId));

            // si no quieres permitir meter MENÚ dentro de MENÚ, descomenta:
            // if (prod.getTipo() == ProductoTipo.MENU) throw new IllegalArgumentException("Un menú no puede contener otro menú.");

            MenuComposicion mc = new MenuComposicion();
            mc.setMenuProducto(menu);
            mc.setProducto(prod);
            mc.setCantidad(Math.max(1, qty));
            menuComposicionRepository.save(mc);
        }
    }



}

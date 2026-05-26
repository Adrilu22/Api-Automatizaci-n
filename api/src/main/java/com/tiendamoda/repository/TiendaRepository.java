package com.tiendamoda.repository;

import com.tiendamoda.model.Categoria;
import com.tiendamoda.model.Producto;
import com.tiendamoda.model.ProductoRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TiendaRepository {

    private static final AtomicInteger ID_SEQ = new AtomicInteger(16);

    private static final List<Producto> PRODUCTOS = new ArrayList<>(List.of(
            new Producto(1,  "Vestido Floral de Verano",     "Vestidos",    89.99,  "XS,S,M,L",    "Multicolor",   true,  15),
            new Producto(2,  "Vestido Elegante de Noche",    "Vestidos",   159.99,  "S,M,L,XL",    "Negro",        true,   8),
            new Producto(3,  "Vestido Casual Rayado",        "Vestidos",    65.00,  "XS,S,M",      "Blanco/Azul",  true,  20),
            new Producto(4,  "Stilettos Rojos Satinados",    "Calzado",     79.99,  "35-41",       "Rojo",         true,  12),
            new Producto(5,  "Sneakers Blancas Premium",     "Calzado",     65.00,  "35-42",       "Blanco",       true,  30),
            new Producto(6,  "Sandalias Boho Doradas",       "Calzado",     49.99,  "35-40",       "Dorado",       true,  18),
            new Producto(7,  "Bolso de Cuero Marrón",        "Accesorios", 129.99,  "Única",       "Marrón",       true,   5),
            new Producto(8,  "Collar Dorado Minimalista",    "Accesorios",  34.99,  "Única",       "Dorado",       true,  40),
            new Producto(9,  "Gafas de Sol Retro",           "Accesorios",  44.99,  "Única",       "Negro/Oro",    true,  22),
            new Producto(10, "Camisa de Lino Blanca",        "Camisas",     45.00,  "XS,S,M,L,XL", "Blanco",       true,  25),
            new Producto(11, "Camisa Oversize Azul",         "Camisas",     55.00,  "S,M,L,XL",    "Azul",         true,  17),
            new Producto(12, "Pantalón de Tela Negro",       "Pantalones",  69.99,  "34-44",       "Negro",        true,  14),
            new Producto(13, "Jeans Slim Fit Azul",          "Pantalones",  79.99,  "28-36",       "Azul",         false,  0),
            new Producto(14, "Blazer Estructurado Beige",    "Blazers",    119.99,  "S,M,L",       "Beige",        true,   9),
            new Producto(15, "Blazer Negro Clásico",         "Blazers",    134.99,  "XS,S,M,L,XL", "Negro",        true,  11)
    ));

    private static final List<Categoria> CATEGORIAS = List.of(
            new Categoria(1, "Vestidos",    "Vestidos para todas las ocasiones",     3),
            new Categoria(2, "Calzado",     "Zapatos, sandalias y sneakers",          3),
            new Categoria(3, "Accesorios",  "Bolsos, joyería y complementos",         3),
            new Categoria(4, "Camisas",     "Camisas y blusas de temporada",          2),
            new Categoria(5, "Pantalones",  "Pantalones y jeans de moda",             2),
            new Categoria(6, "Blazers",     "Blazers y chaquetas formales",           2)
    );

    public List<Producto> findAll() {
        return PRODUCTOS;
    }

    public List<Producto> findByCategoria(String categoria) {
        return PRODUCTOS.stream()
                .filter(p -> p.categoria().equalsIgnoreCase(categoria))
                .toList();
    }

    public Optional<Producto> findById(int id) {
        return PRODUCTOS.stream().filter(p -> p.id() == id).findFirst();
    }

    public List<Producto> search(String query) {
        String q = query.toLowerCase();
        return PRODUCTOS.stream()
                .filter(p -> p.nombre().toLowerCase().contains(q)
                        || p.categoria().toLowerCase().contains(q)
                        || p.color().toLowerCase().contains(q))
                .toList();
    }

    public Producto save(ProductoRequest req) {
        Producto nuevo = new Producto(
                ID_SEQ.getAndIncrement(),
                req.nombre(),
                req.categoria(),
                req.precio(),
                req.tallas(),
                req.color(),
                req.stock() > 0,
                req.stock()
        );
        PRODUCTOS.add(nuevo);
        return nuevo;
    }

    public List<Categoria> findAllCategorias() {
        return CATEGORIAS;
    }
}

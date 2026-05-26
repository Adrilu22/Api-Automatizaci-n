package com.tiendamoda.repository;

import com.tiendamoda.model.Cliente;
import com.tiendamoda.model.DescuentoCampana;
import com.tiendamoda.model.DescuentoCampanaRequest;
import com.tiendamoda.model.Venta;
import com.tiendamoda.model.VentaRequest;
import com.tiendamoda.model.VentaUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TiendaRepository {

    private static final AtomicInteger VENTA_SEQ = new AtomicInteger(29);

    private static final List<Venta> VENTAS = new ArrayList<>(List.of(
            // Febrero 2026 — 5 ventas, 2 con descuento
            new Venta(1,  "2026-02",  3,  89.99, false),
            new Venta(2,  "2026-02",  7, 159.99, true),
            new Venta(3,  "2026-02",  1,  65.00, false),
            new Venta(4,  "2026-02",  5, 129.99, true),
            new Venta(5,  "2026-02",  9,  79.99, false),
            // Marzo 2026 — 8 ventas, 3 con descuento
            new Venta(6,  "2026-03",  2,  89.99, false),
            new Venta(7,  "2026-03",  4,  45.00, true),
            new Venta(8,  "2026-03",  6,  65.00, false),
            new Venta(9,  "2026-03",  8,  44.99, false),
            new Venta(10, "2026-03", 10,  55.00, true),
            new Venta(11, "2026-03",  1,  49.99, false),
            new Venta(12, "2026-03",  3, 134.99, true),
            new Venta(13, "2026-03",  5,  69.99, false),
            // Abril 2026 — 9 ventas, 4 con descuento
            new Venta(14, "2026-04",  2, 159.99, false),
            new Venta(15, "2026-04",  7,  79.99, true),
            new Venta(16, "2026-04",  4,  65.00, false),
            new Venta(17, "2026-04",  9, 119.99, true),
            new Venta(18, "2026-04",  6,  34.99, false),
            new Venta(19, "2026-04", 11,  89.99, true),
            new Venta(20, "2026-04",  2,  65.00, false),
            new Venta(21, "2026-04",  8, 129.99, true),
            new Venta(22, "2026-04",  1,  55.00, false),
            // Mayo 2026 — 6 ventas, 2 con descuento
            new Venta(23, "2026-05",  3, 134.99, false),
            new Venta(24, "2026-05",  5,  89.99, true),
            new Venta(25, "2026-05",  7,  79.99, false),
            new Venta(26, "2026-05", 10, 159.99, true),
            new Venta(27, "2026-05",  2,  65.00, false),
            new Venta(28, "2026-05",  4, 119.99, false)
    ));

    private static final List<Cliente> CLIENTES = new ArrayList<>(List.of(
            new Cliente(1,  "Sofía Martínez",   true),
            new Cliente(2,  "Valentina López",  true),
            new Cliente(3,  "Isabella García",  false),
            new Cliente(4,  "Camila Rodríguez", false),
            new Cliente(5,  "Mariana Herrera",  true),
            new Cliente(6,  "Daniela Torres",   false),
            new Cliente(7,  "Alejandra Pérez",  true),
            new Cliente(8,  "Natalia Flores",   false),
            new Cliente(9,  "Gabriela Sánchez", false),
            new Cliente(10, "Luciana Ramírez",  false),
            new Cliente(11, "Fernanda Vargas",  false),
            new Cliente(12, "Adriana Castro",   false)
    ));

    private static final AtomicInteger CAMPANA_SEQ = new AtomicInteger(5);

    private static final List<DescuentoCampana> CAMPANAS = new ArrayList<>(List.of(
            new DescuentoCampana(1, "2026-02", "2×1 en abrigos de temporada",  "2026-02-28"),
            new DescuentoCampana(2, "2026-03", "Descuento primavera 15%",       null),
            new DescuentoCampana(3, "2026-04", "Liquidación fin de temporada",  "2026-04-30"),
            new DescuentoCampana(4, "2026-05", "Promo clientes nuevos 10%",     "2026-06-15")
    ));

    // ── Consultas ──────────────────────────────────────────────────────────

    public List<Venta> findAllVentas() {
        return VENTAS;
    }

    public Optional<Venta> findVentaById(int id) {
        return VENTAS.stream().filter(v -> v.id() == id).findFirst();
    }

    public long countVentasByMes(String mes) {
        return VENTAS.stream().filter(v -> v.mes().equals(mes)).count();
    }

    public double sumIngresosByMes(String mes) {
        return VENTAS.stream().filter(v -> v.mes().equals(mes)).mapToDouble(Venta::total).sum();
    }

    public long countDescuentosByMes(String mes) {
        return VENTAS.stream().filter(v -> v.mes().equals(mes) && v.descuentoAplicado()).count();
    }

    public List<Cliente> findAllClientes() {
        return CLIENTES;
    }

    public Optional<Cliente> findClienteById(int id) {
        return CLIENTES.stream().filter(c -> c.id() == id).findFirst();
    }

    public long countClientesPremium() {
        return CLIENTES.stream().filter(Cliente::premium).count();
    }

    // ── Operaciones ────────────────────────────────────────────────────────

    public Venta saveVenta(VentaRequest req) {
        Venta nueva = new Venta(VENTA_SEQ.getAndIncrement(), req.mes(), req.clienteId(),
                Math.round(req.total() * 100.0) / 100.0, req.descuentoAplicado());
        VENTAS.add(nueva);
        return nueva;
    }

    public boolean deleteVenta(int id) {
        return VENTAS.removeIf(v -> v.id() == id);
    }

    public Optional<Venta> updateVenta(int id, VentaUpdateRequest req) {
        for (int i = 0; i < VENTAS.size(); i++) {
            Venta v = VENTAS.get(i);
            if (v.id() == id) {
                Venta actualizada = new Venta(v.id(), v.mes(), v.clienteId(),
                        Math.round(req.total() * 100.0) / 100.0, req.descuentoAplicado());
                VENTAS.set(i, actualizada);
                return Optional.of(actualizada);
            }
        }
        return Optional.empty();
    }

    public List<DescuentoCampana> findAllCampanas() {
        return CAMPANAS;
    }

    public DescuentoCampana saveCampana(DescuentoCampanaRequest req) {
        DescuentoCampana nueva = new DescuentoCampana(
                CAMPANA_SEQ.getAndIncrement(), req.mes(), req.descripcion(), req.fechaExpiracion());
        CAMPANAS.add(nueva);
        return nueva;
    }

    public boolean deleteCampana(int id) {
        return CAMPANAS.removeIf(c -> c.id() == id);
    }

    public Optional<Cliente> updateClientePremium(int id, boolean premium) {
        for (int i = 0; i < CLIENTES.size(); i++) {
            Cliente c = CLIENTES.get(i);
            if (c.id() == id) {
                Cliente actualizado = new Cliente(c.id(), c.nombre(), premium);
                CLIENTES.set(i, actualizado);
                return Optional.of(actualizado);
            }
        }
        return Optional.empty();
    }
}

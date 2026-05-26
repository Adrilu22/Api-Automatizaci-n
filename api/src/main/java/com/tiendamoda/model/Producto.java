package com.tiendamoda.model;

public record Producto(
        int id,
        String nombre,
        String categoria,
        double precio,
        String tallas,
        String color,
        boolean disponible,
        int stock
) {}

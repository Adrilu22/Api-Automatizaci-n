package com.tiendamoda.model;

public record ProductoRequest(
        String nombre,
        String categoria,
        double precio,
        String tallas,
        String color,
        int stock
) {}

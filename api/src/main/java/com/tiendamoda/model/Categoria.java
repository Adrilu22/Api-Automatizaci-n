package com.tiendamoda.model;

public record Categoria(
        int id,
        String nombre,
        String descripcion,
        int totalProductos
) {}

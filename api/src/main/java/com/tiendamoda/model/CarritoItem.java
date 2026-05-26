package com.tiendamoda.model;

public record CarritoItem(
        String sesionId,
        int productoId,
        int cantidad
) {}

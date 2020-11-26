package com.geekbrains.cloud.server.DAO;

import java.util.List;

public abstract class DAO<E> {
    public abstract int insert(E entity);
    public abstract int update(E entity);
    public abstract int delete(E entity);
    public abstract List<E> getAll();
}

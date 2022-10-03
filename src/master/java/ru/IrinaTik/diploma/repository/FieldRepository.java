package ru.IrinaTik.diploma.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.IrinaTik.diploma.entity.Field;

@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
}

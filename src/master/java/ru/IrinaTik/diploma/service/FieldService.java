package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.repository.FieldRepository;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FieldService {

    private final FieldRepository repository;

    public List<Field> getAll() {
        return repository.findAll();
    }
}

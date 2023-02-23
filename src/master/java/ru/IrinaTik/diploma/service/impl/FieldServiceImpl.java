package ru.IrinaTik.diploma.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.repository.FieldRepository;
import ru.IrinaTik.diploma.service.FieldService;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FieldServiceImpl implements FieldService {

    private final FieldRepository repository;

    @Override
    public List<Field> getAll() {
        return repository.findAll();
    }
}

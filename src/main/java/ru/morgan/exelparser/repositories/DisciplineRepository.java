package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.Discipline;

import java.util.List;
import java.util.Set;

public interface DisciplineRepository extends CrudRepository<Discipline,Integer> {
    Discipline findByTitle(String title);
    List<Discipline> findAllByIdIn(Set<Integer> ids);
}

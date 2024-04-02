package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.Discipline;
import ru.morgan.exelparser.models.Sport;

import java.util.List;
import java.util.Set;

public interface DisciplineRepository extends CrudRepository<Discipline,Long> {
    Discipline findByTitle(String title);
    List<Discipline> findAllByIdIn(Set<Integer> ids);
    List<Discipline> findByTitleLikeIgnoreCase(String title);
}

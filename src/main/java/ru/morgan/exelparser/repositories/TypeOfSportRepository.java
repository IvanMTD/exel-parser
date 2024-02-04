package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.TypeOfSport;

import java.util.List;

public interface TypeOfSportRepository extends CrudRepository<TypeOfSport,Integer> {
    TypeOfSport findByTitle(String title);
    List<TypeOfSport> findAllByTitleIn(List<String> titles);
}

package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.SportSchool;

public interface SportSchoolRepository extends CrudRepository<SportSchool,Integer> {
    SportSchool findByTitle(String title);
}

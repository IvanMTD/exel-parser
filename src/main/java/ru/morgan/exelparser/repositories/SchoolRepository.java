package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.School;

public interface SchoolRepository extends CrudRepository<School,Long> {
}

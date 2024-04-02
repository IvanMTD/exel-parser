package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.Sport;

public interface SportRepository extends CrudRepository<Sport,Long> {
    Sport findByTitleLikeIgnoreCase(String title);
}

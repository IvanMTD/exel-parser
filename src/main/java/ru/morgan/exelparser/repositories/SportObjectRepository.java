package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.SportObject;

public interface SportObjectRepository extends CrudRepository<SportObject,Long> {
}

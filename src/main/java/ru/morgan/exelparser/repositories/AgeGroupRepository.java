package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.AgeGroup;

import java.util.List;
import java.util.Set;

public interface AgeGroupRepository extends CrudRepository<AgeGroup,Integer> {
    List<AgeGroup> findAllByIdIn(Set<Integer> ids);
}

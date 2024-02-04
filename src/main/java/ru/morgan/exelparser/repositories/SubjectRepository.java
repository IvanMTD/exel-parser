package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.Subject;

public interface SubjectRepository extends CrudRepository<Subject,Integer> {
    Subject findByTitle(String title);
}

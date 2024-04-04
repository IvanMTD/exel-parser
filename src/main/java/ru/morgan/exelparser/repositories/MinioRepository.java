package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.MinioFile;

public interface MinioRepository extends CrudRepository<MinioFile, Long> {
}

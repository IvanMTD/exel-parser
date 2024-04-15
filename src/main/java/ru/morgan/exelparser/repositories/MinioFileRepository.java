package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.MinioFile;

public interface MinioFileRepository extends CrudRepository<MinioFile, Long> {
}

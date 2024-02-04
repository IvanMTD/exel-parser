package ru.morgan.exelparser.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.morgan.exelparser.models.Participant;

import java.time.LocalDate;

public interface ParticipantRepository extends CrudRepository<Participant, Integer> {
    Participant findByLastnameAndNameAndBirthday(String lastname, String name, LocalDate birthday);
}

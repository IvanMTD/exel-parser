package ru.morgan.exelparser.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/*
create table if not exists type_of_sport(
    id int GENERATED BY DEFAULT AS IDENTITY,
    title text not null,
    sport_filter_type text,
    subject_ids integer[],
    discipline_ids integer[]
);
 */

@Data
@Entity
@RequiredArgsConstructor
public class TypeOfSport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    @Enumerated(EnumType.STRING)
    private Season season;
    @Enumerated(EnumType.STRING)
    private SportFilterType sportFilterType;
    Set<Integer> subjectIds = new HashSet<>();
    Set<Integer> disciplineIds = new HashSet<>();

    public void addDiscipline(Discipline discipline){
        disciplineIds.add(discipline.getId());
    }

    public void addSubject(Subject subject){
        subjectIds.add(subject.getId());
    }
}

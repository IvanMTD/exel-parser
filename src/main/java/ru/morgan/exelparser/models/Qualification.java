package ru.morgan.exelparser.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/*
create table if not exists qualification(
    id int GENERATED BY DEFAULT AS IDENTITY,
    category text not null,
    age_group_id int,
    participant_id int
);
 */

@Data
@Entity
@RequiredArgsConstructor
public class Qualification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private Category category;
    private int ageGroupId;
    private int participantId;
}
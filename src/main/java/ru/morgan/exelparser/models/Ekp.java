package ru.morgan.exelparser.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
public class Ekp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String ekp;
    private String num;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private Status status;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginning;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate ending;
    private String category;
    private String location;
    private String organization;
    private long sportId;
    private Set<Long> disciplineIds = new HashSet<>();
    private long logo;
    private long image;
    private float s;
    private float d;
}

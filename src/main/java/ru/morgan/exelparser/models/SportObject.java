package ru.morgan.exelparser.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
public class SportObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;
    protected String location;
    private String address;
    private LocalDate registerDate;
    private String url;
    private float s;
    private float d;
    private long logoId;
    private Set<Long> imageIds = new HashSet<>();

    @Transient
    private String logo;
    @Transient
    private List<String> images = new ArrayList<>();
}

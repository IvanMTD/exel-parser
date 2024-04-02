package ru.morgan.exelparser.models.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Component {
    @JsonProperty("kind")
    private String kind;

    @JsonProperty("name")
    private String name;
}

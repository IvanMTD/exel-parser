package ru.morgan.exelparser.models.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Envelope {
    @JsonProperty("lowerCorner")
    private String lowerCorner;

    @JsonProperty("upperCorner")
    private String upperCorner;
}

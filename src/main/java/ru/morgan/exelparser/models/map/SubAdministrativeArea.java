package ru.morgan.exelparser.models.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubAdministrativeArea {
    @JsonProperty("SubAdministrativeAreaName")
    private String subAdministrativeAreaName;

    @JsonProperty("Locality")
    private Locality locality;
}

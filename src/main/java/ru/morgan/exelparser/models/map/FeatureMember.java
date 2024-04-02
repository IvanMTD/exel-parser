package ru.morgan.exelparser.models.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeatureMember {
    @JsonProperty("GeoObject")
    private GeoObject geoObject;
}

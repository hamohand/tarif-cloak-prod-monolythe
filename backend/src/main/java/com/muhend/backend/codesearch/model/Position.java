package com.muhend.backend.codesearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    private String code;
    private String description;
    private String justification ;

    public Position(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

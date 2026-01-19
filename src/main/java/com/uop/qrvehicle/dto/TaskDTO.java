package com.uop.qrvehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dashboard task cards
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private String name;
    private String link;
    private String icon;
    private String color;
}

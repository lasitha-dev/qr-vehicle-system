package com.uop.qrvehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for dropdown items used in person selection.
 * Used by AJAX endpoints to populate Select2 dropdowns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDropdownItem {
    private String id;
    private String label;
}

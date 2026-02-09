package com.uop.qrvehicle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for student registration request.
 * Migrated from: register.php
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationDTO {

    @NotBlank(message = "Enrollment number is required")
    private String enrolNo;

    @NotBlank(message = "Name with initials is required")
    @Size(max = 200)
    private String nameWithInitials;

    @NotBlank(message = "NIC is required")
    @Size(max = 12)
    private String nic;

    @NotBlank(message = "Faculty is required")
    private String faculty;

    @NotBlank(message = "Course is required")
    private String course;

    @Size(max = 15)
    private String mobile;

    @Email(message = "Invalid email format")
    private String email;

    private String address;
}

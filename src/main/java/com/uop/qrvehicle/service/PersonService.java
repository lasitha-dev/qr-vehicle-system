package com.uop.qrvehicle.service;

import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.dto.StudentDetailDTO;
import com.uop.qrvehicle.model.*;
import com.uop.qrvehicle.repository.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Person Service - Handles searching for students, staff, and visitors
 */
@Service
public class PersonService {

    private final StaffRepository staffRepository;
    private final TemporaryStaffRepository temporaryStaffRepository;
    private final VisitorRepository visitorRepository;
    private final VehicleRepository vehicleRepository;
    private final StudentService studentService;

    public PersonService(StaffRepository staffRepository,
                        TemporaryStaffRepository temporaryStaffRepository,
                        VisitorRepository visitorRepository,
                        VehicleRepository vehicleRepository,
                        StudentService studentService) {
        this.staffRepository = staffRepository;
        this.temporaryStaffRepository = temporaryStaffRepository;
        this.visitorRepository = visitorRepository;
        this.vehicleRepository = vehicleRepository;
        this.studentService = studentService;
    }

    /**
     * Search for a person by ID
     * Determines the type based on ID pattern
     */
    public Optional<PersonSearchResult> searchPerson(String searchId) {
        if (searchId == null || searchId.trim().isEmpty()) {
            return Optional.empty();
        }

        searchId = searchId.trim();

        // Student pattern: e.g., "A/23/001" or similar
        if (searchId.matches("^[A-Z]+/\\d{2}/.*")) {
            return searchStudent(searchId);
        }

        // Permanent staff prefix
        if (searchId.startsWith("PER_")) {
            String empNo = searchId.replace("PER_", "");
            return searchPermanentStaff(empNo);
        }

        // Temporary/Institute staff prefix
        if (searchId.startsWith("TEM_") || searchId.startsWith("INS_")) {
            String empNo = searchId.replaceAll("^(TEM_|INS_)", "");
            return searchTemporaryStaff(empNo);
        }

        // Visitor prefix
        if (searchId.startsWith("VIS_")) {
            String visitorId = searchId.replace("VIS_", "");
            return searchVisitor(visitorId);
        }

        // Try to find by employee number (no prefix)
        Optional<PersonSearchResult> result = searchPermanentStaff(searchId);
        if (result.isPresent()) return result;

        result = searchTemporaryStaff(searchId);
        if (result.isPresent()) return result;

        // Try as visitor ID
        return searchVisitor(searchId);
    }

    /**
     * Search for a student by registration number
     * Now uses the StudentService with secondary datasource for student database
     */
    public Optional<PersonSearchResult> searchStudent(String regNo) {
        return studentService.getStudentBasicInfo(regNo)
            .map(student -> {
                PersonSearchResult result = new PersonSearchResult();
                result.setId(student.getRegNo());
                result.setType("Student");
                result.setName(student.getFullName());
                result.setFaculty(student.getFacultyName());
                result.setCourse(student.getCourseName());
                result.setSemester(student.getSemesterName());
                result.setGender(student.getGender());
                result.setImageUrl(student.getImageUrl());
                result.setVehicles(vehicleRepository.findByEmpIdOrderByCreateDateDesc(regNo));
                return result;
            });
    }

    /**
     * Search for permanent staff by employee number
     */
    public Optional<PersonSearchResult> searchPermanentStaff(String empNo) {
        return staffRepository.findLatestByEmpNo(empNo)
            .map(staff -> {
                PersonSearchResult result = new PersonSearchResult();
                result.setId(staff.getEmpNo());
                result.setType("Permanent Staff");
                result.setName(staff.getEmpName());
                result.setDesignation(staff.getDesignation());
                result.setCategory(staff.getCategory());
                result.setDepartment(staff.getDepartment());
                result.setNic(staff.getNic());
                result.setGender(staff.getSex());
                result.setEmployeeType(staff.getEmployeeType());
                result.setVehicles(vehicleRepository.findByEmpIdOrderByCreateDateDesc(empNo));
                return result;
            });
    }

    /**
     * Search for temporary staff by employee number
     */
    public Optional<PersonSearchResult> searchTemporaryStaff(String empNo) {
        return temporaryStaffRepository.findByEmpNo(empNo)
            .map(staff -> {
                PersonSearchResult result = new PersonSearchResult();
                result.setId(staff.getEmpNo());
                result.setType("Temporary Staff");
                result.setName(staff.getEmpName());
                result.setDesignation(staff.getDesignation());
                result.setCategory(staff.getCategory());
                result.setDepartment(staff.getDepartment());
                result.setNic(staff.getNic());
                result.setGender(staff.getSex());
                result.setVehicles(vehicleRepository.findByEmpIdOrderByCreateDateDesc(empNo));
                return result;
            });
    }

    /**
     * Search for visitor by ID (String)
     */
    public Optional<PersonSearchResult> searchVisitor(String visitorId) {
        return visitorRepository.findById(visitorId)
            .map(visitor -> {
                PersonSearchResult result = new PersonSearchResult();
                result.setId(visitor.getId());
                result.setType("Visitor");
                result.setName(visitor.getName());
                result.setReason(visitor.getReason());
                result.setDateFrom(visitor.getDateFrom());
                result.setDateTo(visitor.getDateTo());
                result.setVehicles(vehicleRepository.findByEmpIdOrderByCreateDateDesc(visitor.getId()));
                return result;
            });
    }
}

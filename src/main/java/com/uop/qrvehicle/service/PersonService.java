package com.uop.qrvehicle.service;

import com.uop.qrvehicle.dto.PersonDropdownItem;
import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.model.*;
import com.uop.qrvehicle.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Person Service - Handles searching for students, staff, and visitors
 */
@Service
public class PersonService {

    private static final Logger log = LoggerFactory.getLogger(PersonService.class);

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
     * List all permanent staff for dropdown.
     * Mirrors PHP: SELECT s.EmpNo AS id, CONCAT(s.EmpNo, ' - ', s.EmpNm, ' (Permanent)') AS label
     */
    public List<PersonDropdownItem> listPermanentStaff() {
        try {
            return staffRepository.findAllLatestRecords().stream()
                    .map(s -> new PersonDropdownItem(
                            s.getEmpNo(),
                            s.getEmpNo() + " - " + s.getEmpName() + " (Permanent)"))
                    .toList();
        } catch (Exception e) {
            log.error("Error listing permanent staff: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<PersonDropdownItem> listPermanentStaffByUserType(String userType) {
        try {
            List<Staff> rows;
            if (isNonAcademicUserType(userType)) {
                rows = staffRepository.findAllLatestNonAcademicRecords();
            } else if (isAcademicUserType(userType)) {
                rows = staffRepository.findAllLatestAcademicRecords();
            } else {
                rows = staffRepository.findAllLatestRecords();
            }
            return rows.stream()
                    .map(s -> new PersonDropdownItem(
                            s.getEmpNo(),
                            s.getEmpNo() + " - " + s.getEmpName() + " (Permanent)"))
                    .toList();
        } catch (Exception e) {
            log.error("Error listing permanent staff by user type {}: {}", userType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean isCategoryAllowedForUserType(String category, String userType) {
        if (category == null || category.isBlank()) {
            return true;
        }
        if (isAcademicUserType(userType) || isNonAcademicUserType(userType)) {
            return "permanent".equalsIgnoreCase(category);
        }
        return true;
    }

    public boolean canViewPermanentStaffForUserType(Staff staff, String userType) {
        if (staff == null) {
            return false;
        }
        if (isNonAcademicUserType(userType)) {
            return "Non Academic".equalsIgnoreCase(staff.getEmployeeType());
        }
        if (isAcademicUserType(userType)) {
            return staff.getEmployeeType() == null || !"Non Academic".equalsIgnoreCase(staff.getEmployeeType());
        }
        return true;
    }

    public boolean isAcademicUserType(String userType) {
        return "ACADEMIC".equals(normalizeUserType(userType));
    }

    public boolean isNonAcademicUserType(String userType) {
        return "NONACADEMIC".equals(normalizeUserType(userType));
    }

    private String normalizeUserType(String userType) {
        if (userType == null) {
            return "";
        }
        return userType.replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .trim()
                .toUpperCase();
    }

    /**
     * List temporary/casual/contract/institute staff filtered by category.
     * Mirrors PHP: SELECT EmpNo AS id, CONCAT(EmpNo,' - ',EmpNm,' (',EmpCat1Nm,')') AS label
     *              FROM temporarystaff WHERE EmpCat1Nm LIKE ?
     */
    public List<PersonDropdownItem> listStaffByCategory(String category) {
        try {
            return temporaryStaffRepository.findByCategory(category).stream()
                    .map(s -> new PersonDropdownItem(
                            s.getEmpNo(),
                            s.getEmpNo() + " - " + s.getEmpName() + " (" + s.getCategory() + ")"))
                    .toList();
        } catch (Exception e) {
            log.error("Error listing staff by category {}: {}", category, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * List all visitors for dropdown.
     * Mirrors PHP: SELECT ID AS id, CONCAT(ID, ' - ', Name, ' (Visitor)') AS label FROM visitor
     */
    public List<PersonDropdownItem> listVisitors() {
        try {
            return visitorRepository.findAllByOrderByIdDesc().stream()
                    .map(v -> new PersonDropdownItem(
                            v.getId(),
                            v.getId() + " - " + v.getName() + " (Visitor)"))
                    .toList();
        } catch (Exception e) {
            log.error("Error listing visitors: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
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

        try {
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
        } catch (Exception e) {
            log.error("Error searching for person with ID '{}': {}", searchId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Safely load vehicles for a given person ID.
     * Returns empty list if any error occurs during loading.
     */
    private List<Vehicle> safeLoadVehicles(String empId) {
        try {
            return vehicleRepository.findByEmpIdOrderByCreateDateDesc(empId);
        } catch (Exception e) {
            log.warn("Failed to load vehicles for '{}': {}", empId, e.getMessage());
            return Collections.emptyList();
        }
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
                result.setVehicles(safeLoadVehicles(regNo));
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
                result.setVehicles(safeLoadVehicles(empNo));
                return result;
            });
    }

    /**
     * Search for temporary staff by employee number
     */
    public Optional<PersonSearchResult> searchTemporaryStaff(String empNo) {
        return temporaryStaffRepository.findFirstByEmpNo(empNo)
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
                result.setVehicles(safeLoadVehicles(empNo));
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
                result.setVehicles(safeLoadVehicles(visitor.getId()));
                return result;
            });
    }
}

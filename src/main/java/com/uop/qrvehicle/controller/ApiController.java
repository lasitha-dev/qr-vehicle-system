package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.PersonDropdownItem;
import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.model.Visitor;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.repository.TemporaryStaffRepository;
import com.uop.qrvehicle.repository.VehicleRepository;
import com.uop.qrvehicle.repository.VisitorRepository;
import com.uop.qrvehicle.service.PersonService;
import com.uop.qrvehicle.service.StudentService;
import com.uop.qrvehicle.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API Controller
 * Migrated from PHP: checkVehicle.php, get_user_info.php, get_user_email.php, get_master.php,
 *                     person_redirect.php
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final VehicleRepository vehicleRepository;
    private final StaffRepository staffRepository;
    private final TemporaryStaffRepository temporaryStaffRepository;
    private final VisitorRepository visitorRepository;
    private final PersonService personService;
    private final StudentService studentService;

    public ApiController(VehicleRepository vehicleRepository,
                        StaffRepository staffRepository,
                        TemporaryStaffRepository temporaryStaffRepository,
                        VisitorRepository visitorRepository,
                        PersonService personService,
                        StudentService studentService) {
        this.vehicleRepository = vehicleRepository;
        this.staffRepository = staffRepository;
        this.temporaryStaffRepository = temporaryStaffRepository;
        this.visitorRepository = visitorRepository;
        this.personService = personService;
        this.studentService = studentService;
    }

    // =========================================================================
    // 1. CHECK VEHICLE API  (replaces api/checkVehicle.php)
    //    GET /api/vehicle/check?vehicleno=XXX
    // =========================================================================
    @GetMapping("/vehicle/check")
    public ResponseEntity<Map<String, Object>> checkVehicle(
            @RequestParam(required = false) String vehicleno) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (vehicleno == null || vehicleno.trim().isEmpty()) {
            response.put("error", "no_veh");
            return ResponseEntity.badRequest().body(response);
        }

        vehicleno = vehicleno.trim();

        // Search across all vehicles for exact match
        List<Vehicle> vehicles = vehicleRepository.findByVehicleNoContainingIgnoreCase(vehicleno);

        // Find exact match
        for (Vehicle v : vehicles) {
            if (v.getVehicleNo().equalsIgnoreCase(vehicleno)) {
                response.put("found", true);
                response.put("category", v.getType());
                response.put("id", v.getEmpId());
                return ResponseEntity.ok(response);
            }
        }

        response.put("found", false);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 2. GET USER INFO API  (replaces get_user_info.php)
    //    GET /api/user/info?userid=XXX
    // =========================================================================
    @GetMapping("/user/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestParam(required = false) String userid) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (userid == null || userid.trim().isEmpty()) {
            response.put("error", "No user ID provided");
            return ResponseEntity.badRequest().body(response);
        }

        userid = userid.trim();

        // Try student first (reg number pattern: XX/YY/ZZZ)
        if (userid.matches("^[A-Z]+/\\d{2}/.*")) {
            try {
                var studentOpt = studentService.getStudentBasicInfo(userid);
                if (studentOpt.isPresent()) {
                    var student = studentOpt.get();
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("Reg_No", student.getRegNo());
                    data.put("FullName", student.getFullName());
                    data.put("Faculty", student.getFacultyName());
                    data.put("Course", student.getCourseName());
                    data.put("Status", student.getStatus());
                    data.put("Gender", student.getGender());
                    data.put("ImageUrl", student.getImageUrl());

                    response.put("category", "student");
                    response.put("data", data);
                    return ResponseEntity.ok(response);
                }
            } catch (Exception e) {
                log.debug("Student lookup failed for {}: {}", userid, e.getMessage());
            }
        }

        // Try permanent staff
        Optional<Staff> staffOpt = staffRepository.findLatestByEmpNo(userid);
        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("EmpNo", staff.getEmpNo());
            data.put("EmpNm", staff.getEmpName());
            data.put("DesgNm", staff.getDesignation());
            data.put("EmpCat1Nm", staff.getCategory());
            data.put("NIC", staff.getNic());
            data.put("Sex", staff.getSex());
            data.put("BuNm", staff.getDepartment());
            data.put("EmpTypCd", staff.getEmployeeType());
            data.put("DtBirth", staff.getDateOfBirth());

            response.put("category", "staff");
            response.put("data", data);
            return ResponseEntity.ok(response);
        }

        // Try temporary/contract/institute staff
        var tempOpt = temporaryStaffRepository.findFirstByEmpNo(userid);
        if (tempOpt.isPresent()) {
            var temp = tempOpt.get();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("EmpNo", temp.getEmpNo());
            data.put("EmpNm", temp.getEmpName());
            data.put("DesgNm", temp.getDesignation());
            data.put("EmpCat1Nm", temp.getCategory());
            data.put("NIC", temp.getNic());
            data.put("Sex", temp.getSex());
            data.put("BuNm", temp.getDepartment());

            response.put("category", "temporary_staff");
            response.put("data", data);
            return ResponseEntity.ok(response);
        }

        response.put("error", "User not found");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 3. GET USER EMAIL API  (replaces get_user_email.php)
    //    GET /api/user/email?userid=XXX&type=student|staff&nic=YYY
    // =========================================================================
    @GetMapping("/user/email")
    public ResponseEntity<Map<String, Object>> getUserEmail(
            @RequestParam(required = false) String userid,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String nic) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (userid == null || userid.trim().isEmpty()) {
            response.put("error", "No user ID provided");
            return ResponseEntity.badRequest().body(response);
        }

        userid = userid.trim();

        if ("student".equalsIgnoreCase(type)) {
            // Get student email from studdb
            try {
                var studentOpt = studentService.getStudentDetail(userid);
                if (studentOpt.isPresent()) {
                    var student = studentOpt.get();
                    response.put("email", student.getEmail() != null ? student.getEmail() : "");
                    response.put("type", "student");
                    response.put("status", "Registered");
                    return ResponseEntity.ok(response);
                }
            } catch (Exception e) {
                log.debug("Student email lookup failed for {}: {}", userid, e.getMessage());
            }

            response.put("error", "Student not found or not registered");
            return ResponseEntity.ok(response);

        } else if ("staff".equalsIgnoreCase(type)) {
            // Staff lookup requires NIC verification
            if (nic == null || nic.trim().isEmpty()) {
                response.put("error", "NIC is required for staff email lookup");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify by empNo AND NIC
            Optional<Staff> staffOpt = staffRepository.findLatestByEmpNo(userid);
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                if (staff.getNic() != null && staff.getNic().equalsIgnoreCase(nic.trim())) {
                    response.put("empno", staff.getEmpNo());
                    response.put("nic", staff.getNic());
                    response.put("latest_salary_date", staff.getSalaryDate());
                    response.put("type", "staff");
                    response.put("status", "Login confirmed");
                    // Note: Staff table doesn't have Email column directly
                    // email would typically come from a separate system
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "NIC verification failed");
                    return ResponseEntity.ok(response);
                }
            }

            response.put("error", "Staff not found");
            return ResponseEntity.ok(response);
        }

        response.put("error", "Invalid type parameter. Use 'student' or 'staff'.");
        return ResponseEntity.badRequest().body(response);
    }

    // =========================================================================
    // 4. GET MASTER (PERSON + VEHICLES) API  (replaces get_master.php)
    //    GET /api/person/master?empid=XXX&type=Student|Permanent|Temporary|...
    // =========================================================================
    @GetMapping("/person/master")
    public ResponseEntity<Map<String, Object>> getMaster(
            @RequestParam(required = false) String empid,
            @RequestParam(required = false, defaultValue = "Permanent") String type) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (empid == null || empid.trim().isEmpty()) {
            response.put("error", "No ID provided");
            return ResponseEntity.badRequest().body(response);
        }

        empid = empid.trim();

        Map<String, Object> person = new LinkedHashMap<>();
        boolean found = false;

        switch (type.toLowerCase()) {
            case "student":
                try {
                    var studentOpt = studentService.getStudentBasicInfo(empid);
                    if (studentOpt.isPresent()) {
                        var s = studentOpt.get();
                        person.put("EmpNo", s.getRegNo());
                        person.put("EmpNm", s.getFullName());
                        person.put("Faculty", s.getFacultyName());
                        person.put("Course", s.getCourseName());
                        person.put("Status", s.getStatus());
                        found = true;
                    }
                } catch (Exception e) {
                    log.debug("Student master lookup failed: {}", e.getMessage());
                }
                break;

            case "permanent":
                Optional<Staff> staffOpt = staffRepository.findLatestByEmpNo(empid);
                if (staffOpt.isPresent()) {
                    Staff s = staffOpt.get();
                    person.put("EmpNo", s.getEmpNo());
                    person.put("EmpNm", s.getEmpName());
                    person.put("DesgNm", s.getDesignation());
                    person.put("EmpCat1Nm", s.getCategory());
                    person.put("NIC", s.getNic());
                    person.put("BuNm", s.getDepartment());
                    person.put("DtBirth", s.getDateOfBirth());
                    found = true;
                }
                break;

            case "temporary":
            case "casual":
            case "contract":
            case "institute":
                var tempOpt = temporaryStaffRepository.findFirstByEmpNo(empid);
                if (tempOpt.isPresent()) {
                    var t = tempOpt.get();
                    person.put("EmpNo", t.getEmpNo());
                    person.put("EmpNm", t.getEmpName());
                    person.put("DesgNm", t.getDesignation());
                    person.put("EmpCat1Nm", t.getCategory());
                    person.put("NIC", t.getNic());
                    person.put("BuNm", t.getDepartment());
                    found = true;
                }
                break;

            case "visit":
            case "visitor":
                Optional<Visitor> vOpt = visitorRepository.findById(empid);
                if (vOpt.isPresent()) {
                    Visitor v = vOpt.get();
                    person.put("EmpNo", v.getId());
                    person.put("EmpNm", v.getName());
                    person.put("DesgNm", v.getReason());
                    person.put("DateFrom", v.getDateFrom());
                    person.put("DateTo", v.getDateTo());
                    found = true;
                }
                break;

            default:
                // Fallback: try permanent, then temporary, then visitor
                Optional<Staff> fallbackStaff = staffRepository.findLatestByEmpNo(empid);
                if (fallbackStaff.isPresent()) {
                    Staff s = fallbackStaff.get();
                    person.put("EmpNo", s.getEmpNo());
                    person.put("EmpNm", s.getEmpName());
                    person.put("DesgNm", s.getDesignation());
                    person.put("EmpCat1Nm", s.getCategory());
                    person.put("NIC", s.getNic());
                    person.put("BuNm", s.getDepartment());
                    found = true;
                } else {
                    var fallbackTemp = temporaryStaffRepository.findFirstByEmpNo(empid);
                    if (fallbackTemp.isPresent()) {
                        var t = fallbackTemp.get();
                        person.put("EmpNo", t.getEmpNo());
                        person.put("EmpNm", t.getEmpName());
                        person.put("DesgNm", t.getDesignation());
                        person.put("EmpCat1Nm", t.getCategory());
                        found = true;
                    }
                }
                break;
        }

        if (!found) {
            response.put("error", "Person not found");
            return ResponseEntity.ok(response);
        }

        // Fetch vehicles
        List<Vehicle> vehicles = vehicleRepository.findByEmpIdOrderByCreateDateDesc(empid);
        List<Map<String, String>> vehicleList = new ArrayList<>();
        for (Vehicle v : vehicles) {
            Map<String, String> vm = new LinkedHashMap<>();
            vm.put("Vehino", v.getVehicleNo());
            vm.put("VehiOwner", v.getOwner());
            vm.put("Type", v.getType());
            vehicleList.add(vm);
        }

        response.put("person", person);
        response.put("vehicles", vehicleList);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 5. PERSON REDIRECT API  (replaces person_redirect.php)
    //    GET /api/person/resolve?id=XXX
    //    Returns where to redirect for this person ID
    // =========================================================================
    @GetMapping("/person/resolve")
    public ResponseEntity<Map<String, Object>> resolvePerson(
            @RequestParam(required = false) String id) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (id == null || id.trim().isEmpty()) {
            response.put("error", "No ID provided");
            return ResponseEntity.badRequest().body(response);
        }

        id = id.trim();

        Optional<PersonSearchResult> result = personService.searchPerson(id);
        if (result.isPresent()) {
            PersonSearchResult person = result.get();
            response.put("found", true);
            response.put("type", person.getType());
            response.put("id", person.getId());
            response.put("name", person.getName());

            // Determine redirect URL
            String redirectUrl;
            if ("Student".equalsIgnoreCase(person.getType())) {
                redirectUrl = "/student/detail?regno=" + person.getId();
            } else {
                redirectUrl = "/search/person?id=" + person.getId();
            }
            response.put("redirectUrl", redirectUrl);
        } else {
            response.put("found", false);
            response.put("error", "Person not found");
        }

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6. LIST PERSONS BY CATEGORY  (for vehicle insert dropdown)
    //    GET /api/persons/list?category=permanent|temporary|casual|contract|institute|visitor
    // =========================================================================
    @GetMapping("/persons/list")
    public ResponseEntity<List<PersonDropdownItem>> listPersonsByCategory(
            @RequestParam String category,
            Authentication authentication) {

        List<PersonDropdownItem> items;
        String userType = resolveUserType(authentication);

        if (!personService.isCategoryAllowedForUserType(category, userType)) {
            return ResponseEntity.ok(List.of());
        }

        switch (category.toLowerCase()) {
            case "permanent":
                items = personService.listPermanentStaffByUserType(userType);
                break;
            case "temporary":
            case "casual":
            case "contract":
            case "institute":
                items = personService.listStaffByCategory(category);
                break;
            case "visitor":
                items = personService.listVisitors();
                break;
            default:
                items = List.of();
                break;
        }

        return ResponseEntity.ok(items);
    }

    private String resolveUserType(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserType();
        }
        return "";
    }

    // =========================================================================
    // 7. STUDENT CASCADE DROPDOWNS  (for vehicle insert)
    //    GET /api/students/faculties
    //    GET /api/students/years?faculty=X
    //    GET /api/students/list?faculty=X&year=Y
    // =========================================================================
    @GetMapping("/students/faculties")
    public ResponseEntity<List<String>> getStudentFaculties() {
        return ResponseEntity.ok(studentService.getDistinctFaculties());
    }

    @GetMapping("/students/years")
    public ResponseEntity<List<String>> getStudentYears(@RequestParam String faculty) {
        return ResponseEntity.ok(studentService.getYearsByFaculty(faculty));
    }

    @GetMapping("/students/list")
    public ResponseEntity<List<PersonDropdownItem>> getStudentsList(
            @RequestParam String faculty,
            @RequestParam String year) {
        return ResponseEntity.ok(studentService.getStudentsByFacultyAndYear(faculty, year));
    }
}

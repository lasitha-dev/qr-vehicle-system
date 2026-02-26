package com.uop.qrvehicle.service;

import com.uop.qrvehicle.dto.PersonDropdownItem;
import com.uop.qrvehicle.dto.StudentDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Student Service
 * Queries the student database (studdb) for comprehensive student details.
 * Mirrors PHP view.php which joins stud, studbasic, faculty, course, district, studother tables.
 * Uses secondary JdbcTemplate since student data lives on a different database host.
 */
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final JdbcTemplate studDbJdbcTemplate;

    public StudentService(@Qualifier("studDbJdbcTemplate") JdbcTemplate studDbJdbcTemplate) {
        this.studDbJdbcTemplate = studDbJdbcTemplate;
    }

    /**
     * Get comprehensive student details by registration number.
     * Mirrors PHP view.php: SELECT * FROM stud, studbasic, faculty, course, district, studother
     * WHERE stud.NIC=studbasic.NIC AND stud.Faculty=faculty.Fac_Code 
     * AND stud.Course=course.course_ID AND studother.Dist_No=district.Dist_No 
     * AND stud.NIC=studother.NIC AND Reg_No=?
     *
     * Column mapping matched to real studdb schema (studdb (7).sql):
     *   studbasic: RegOn, SelectType, ADD1/ADD2/ADD3, Phone_No
     *   studother: DOB, Religion, Ethic, Z_Score, Home, Mobile, Email,
     *              PName, PAdd, PTelNo, EName, ETelNo, Div_No, Police
     *   district:  District (not Dist_Name)
     */
    public Optional<StudentDetailDTO> getStudentDetail(String regNo) {
        try {
            String sql = """
                SELECT 
                    s.Reg_No, s.App_Year, s.Faculty, s.Course, s.Status, s.NIC,
                    sb.Title, sb.Initials, sb.L_Name, sb.Full_Name, sb.Gender,
                    sb.SelectType, sb.RegOn, sb.Phone_No,
                    sb.ADD1, sb.ADD2, sb.ADD3,
                    f.Fac_name AS FacultyName,
                    c.Course_name AS CourseName,
                    d.District AS DistrictName,
                    so.DOB, so.Religion, so.Ethic, so.Z_Score,
                    so.Dist_No, so.Div_No, so.Police,
                    so.Home, so.Mobile, so.Email,
                    so.PName, so.PAdd, so.PTelNo, so.PRelationship,
                    so.EName, so.ETelNo, so.ERelationship
                FROM stud s
                LEFT JOIN studbasic sb ON s.NIC = sb.NIC
                LEFT JOIN faculty f ON s.Faculty = f.Fac_Code
                LEFT JOIN course c ON s.Course = c.Course_ID
                LEFT JOIN studother so ON s.NIC = so.NIC
                LEFT JOIN district d ON so.Dist_No = d.Dist_No
                WHERE s.Reg_No = ?
                LIMIT 1
                """;

            StudentDetailDTO dto = studDbJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                StudentDetailDTO student = new StudentDetailDTO();
                student.setRegNo(rs.getString("Reg_No"));
                student.setAppYear(rs.getString("App_Year"));
                student.setFaculty(rs.getString("Faculty"));
                student.setCourse(rs.getString("Course"));
                student.setStatus(rs.getString("Status"));
                student.setNic(rs.getString("NIC"));
                student.setRegisteredOn(rs.getString("RegOn"));
                student.setSelectType(rs.getString("SelectType"));

                student.setTitle(rs.getString("Title"));
                student.setInitials(rs.getString("Initials"));
                student.setLastName(rs.getString("L_Name"));
                student.setFullName(rs.getString("Full_Name"));
                student.setGender(rs.getString("Gender"));

                // DOB, Religion, Ethic, Z_Score are in studother
                student.setDateOfBirth(rs.getString("DOB"));
                student.setReligion(rs.getString("Religion"));
                student.setEthnicity(rs.getString("Ethic"));
                student.setZScore(rs.getString("Z_Score"));

                student.setFacultyName(rs.getString("FacultyName"));
                student.setCourseName(rs.getString("CourseName"));
                student.setDistrict(rs.getString("DistrictName"));

                // Address from studbasic (permanent address)
                student.setAddress1(rs.getString("ADD1"));
                student.setAddress2(rs.getString("ADD2"));
                student.setAddress3(rs.getString("ADD3"));
                student.setPoliceStation(rs.getString("Police"));

                // Contact: Home phone from studother, Mobile from studother, Email from studother
                student.setPhone(rs.getString("Home"));
                student.setMobile(rs.getString("Mobile"));
                student.setEmail(rs.getString("Email"));

                // Parent/Guardian from studother (PName, PAdd, PTelNo)
                student.setFatherName(rs.getString("PName"));
                student.setGuardianName(rs.getString("PName"));
                student.setGuardianAddress(rs.getString("PAdd"));
                student.setGuardianPhone(rs.getString("PTelNo"));

                // Emergency contact from studother (EName, ETelNo)
                student.setEmergencyContactName(rs.getString("EName"));
                student.setEmergencyContactPhone(rs.getString("ETelNo"));

                return student;
            }, regNo);

            if (dto != null) {
                loadSemesterInfo(dto);
                loadAcademicHistory(dto);
                dto.setImageUrl(buildStudentImageUrl(dto.getRegNo()));
            }

            return Optional.ofNullable(dto);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching student detail for regNo={}: {}", regNo, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Load latest semester info from studclass table.
     * Mirrors PHP: SELECT Semester, RegDate FROM studclass WHERE Reg_No=? ORDER BY RegDate DESC LIMIT 1
     */
    private void loadSemesterInfo(StudentDetailDTO dto) {
        try {
            String sql = """
                SELECT Semester, RegDate 
                FROM studclass 
                WHERE Reg_No = ? 
                ORDER BY RegDate DESC 
                LIMIT 1
                """;
            studDbJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String rawSemester = rs.getString("Semester");
                String rawRegDate = rs.getString("RegDate");
                dto.setCurrentSemester(rawSemester);
                dto.setSemesterName(convertSemesterName(rawSemester));
                dto.setSemesterRegDate(rawRegDate);
                return null;
            }, dto.getRegNo());
        } catch (EmptyResultDataAccessException e) {
            // No semester info found
        } catch (Exception e) {
            log.warn("Error loading semester info for {}: {}", dto.getRegNo(), e.getMessage());
        }
    }

    /**
     * Load academic history (promotions, transfers, deferrals, cancellations).
     * Mirrors PHP methodList.php: checkPromote, checkTransfer, checkDefer, checkCancel
     */
    private void loadAcademicHistory(StudentDetailDTO dto) {
        try {
            // Promotions
            List<String> promotions = studDbJdbcTemplate.query(
                "SELECT Semester, RegDate FROM studclass WHERE Reg_No = ? ORDER BY RegDate ASC",
                (rs, rowNum) -> "Semester " + convertSemesterName(rs.getString("Semester")) 
                    + " - " + rs.getString("RegDate"),
                dto.getRegNo()
            );
            dto.setPromotions(promotions);
        } catch (Exception e) {
            log.warn("Error loading academic history for {}: {}", dto.getRegNo(), e.getMessage());
        }
    }

    /**
     * Search students by partial registration number or name.
     * Returns basic info for search results.
     */
    public List<StudentDetailDTO> searchStudents(String query) {
        try {
            String sql = """
                SELECT s.Reg_No, s.App_Year, s.Faculty, s.Status,
                       sb.Full_Name, sb.Gender,
                       COALESCE(f.Fac_name, s.Faculty) AS FacultyName,
                       COALESCE(c.Course_name, s.Course) AS CourseName
                FROM stud s
                LEFT JOIN studbasic sb ON s.NIC = sb.NIC
                LEFT JOIN faculty f ON s.Faculty = f.Fac_Code
                LEFT JOIN course c ON s.Course = c.Course_ID
                WHERE s.Reg_No LIKE ? OR sb.Full_Name LIKE ?
                ORDER BY s.Reg_No
                LIMIT 50
                """;
            String likeQuery = "%" + query + "%";
            return studDbJdbcTemplate.query(sql, (rs, rowNum) -> {
                StudentDetailDTO dto = new StudentDetailDTO();
                dto.setRegNo(rs.getString("Reg_No"));
                dto.setAppYear(rs.getString("App_Year"));
                dto.setFaculty(rs.getString("Faculty"));
                dto.setStatus(rs.getString("Status"));
                dto.setFullName(rs.getString("Full_Name"));
                dto.setGender(rs.getString("Gender"));
                dto.setFacultyName(rs.getString("FacultyName"));
                dto.setCourseName(rs.getString("CourseName"));
                return dto;
            }, likeQuery, likeQuery);
        } catch (Exception e) {
            log.error("Error searching students: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if a registration number belongs to a registered student.
     * Mirrors PHP: SELECT * FROM stud WHERE Reg_No=? AND Status='REGISTERED'
     */
    public boolean isRegisteredStudent(String regNo) {
        try {
            Integer count = studDbJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stud WHERE Reg_No = ? AND Status = 'REGISTERED'",
                Integer.class, regNo
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Error checking student registration for {}: {}", regNo, e.getMessage());
            return false;
        }
    }

    /**
     * Get basic student info for vehicle registration self-service.
     * Mirrors PHP: SELECT s.Reg_No, sb.Full_Name, f.Fac_name, c.Course_name
     *              FROM stud s JOIN studbasic sb ... WHERE s.Reg_No=? AND s.Status='REGISTERED'
     */
    public Optional<StudentDetailDTO> getStudentBasicInfo(String regNo) {
        try {
            String sql = """
                SELECT 
                    s.Reg_No, s.NIC, sb.Full_Name, s.App_Year, 
                    s.Faculty, s.Course,
                    COALESCE(f.Fac_name, s.Faculty) AS FacultyName,
                    COALESCE(c.Course_name, s.Course) AS CourseName,
                    sb.Gender
                FROM stud s
                LEFT JOIN studbasic sb ON s.NIC = sb.NIC
                LEFT JOIN faculty f ON s.Faculty = f.Fac_Code
                LEFT JOIN course c ON s.Course = c.Course_ID
                WHERE s.Reg_No = ? AND s.Status = 'REGISTERED'
                LIMIT 1
                """;

            StudentDetailDTO dto = studDbJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                StudentDetailDTO student = new StudentDetailDTO();
                student.setRegNo(rs.getString("Reg_No"));
                student.setNic(rs.getString("NIC"));
                student.setFullName(rs.getString("Full_Name"));
                student.setAppYear(rs.getString("App_Year"));
                student.setFaculty(rs.getString("Faculty"));
                student.setCourse(rs.getString("Course"));
                student.setFacultyName(rs.getString("FacultyName"));
                student.setCourseName(rs.getString("CourseName"));
                student.setGender(rs.getString("Gender"));
                return student;
            }, regNo);

            if (dto != null) {
                loadSemesterInfo(dto);
                dto.setImageUrl(buildStudentImageUrl(dto.getRegNo()));
            }
            return Optional.ofNullable(dto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting student basic info for {}: {}", regNo, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get distinct faculty prefixes from registered students.
     * Mirrors PHP: SELECT DISTINCT SUBSTRING_INDEX(Reg_No,'/',1) AS faculty_prefix
     *              FROM stud WHERE Status='REGISTERED' ORDER BY faculty_prefix
     */
    public List<String> getDistinctFaculties() {
        try {
            return studDbJdbcTemplate.query(
                "SELECT DISTINCT SUBSTRING_INDEX(Reg_No,'/',1) AS faculty_prefix FROM stud WHERE Status='REGISTERED' ORDER BY faculty_prefix",
                (rs, rowNum) -> rs.getString("faculty_prefix")
            );
        } catch (Exception e) {
            log.error("Error fetching distinct faculties: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get distinct academic years for a given faculty prefix.
     * Mirrors PHP: SELECT DISTINCT CASE WHEN LENGTH(...)=2 THEN CONCAT('20',...) ELSE ... END AS YearFull
     *              FROM stud WHERE Status='REGISTERED' AND Reg_No LIKE '{faculty}/%' ORDER BY YearFull
     */
    public List<String> getYearsByFaculty(String faculty) {
        try {
            String sql = """
                SELECT DISTINCT
                    CASE
                        WHEN LENGTH(SUBSTRING_INDEX(SUBSTRING_INDEX(Reg_No,'/',2),'/',-1)) = 2
                            THEN CONCAT('20', SUBSTRING_INDEX(SUBSTRING_INDEX(Reg_No,'/',2),'/',-1))
                        ELSE SUBSTRING_INDEX(SUBSTRING_INDEX(Reg_No,'/',2),'/',-1)
                    END AS YearFull
                FROM stud
                WHERE Status='REGISTERED' AND Reg_No LIKE CONCAT(?,'/%')
                ORDER BY YearFull ASC
                """;
            return studDbJdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("YearFull"), faculty);
        } catch (Exception e) {
            log.error("Error fetching years for faculty={}: {}", faculty, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<PersonDropdownItem> getStudentsByFacultyAndYear(String faculty, String year) {
        List<PersonDropdownItem> items;
        try {
            String sql = """
                SELECT s.Reg_No, COALESCE(sb.Full_Name, '') AS Full_Name
                FROM stud s
                LEFT JOIN studbasic sb ON s.NIC = sb.NIC
                WHERE s.Status='REGISTERED'
                  AND s.Reg_No LIKE ?
                  AND (
                    CASE
                        WHEN LENGTH(SUBSTRING_INDEX(SUBSTRING_INDEX(s.Reg_No,'/',2),'/',-1)) = 2
                            THEN CONCAT('20', SUBSTRING_INDEX(SUBSTRING_INDEX(s.Reg_No,'/',2),'/',-1))
                        ELSE SUBSTRING_INDEX(SUBSTRING_INDEX(s.Reg_No,'/',2),'/',-1)
                    END
                  ) = ?
                ORDER BY s.Reg_No ASC
                """;
            items = new ArrayList<>(studDbJdbcTemplate.query(sql, (rs, rowNum) -> {
                String regNo = rs.getString("Reg_No");
                String fullName = rs.getString("Full_Name");
                String label = fullName != null && !fullName.isEmpty()
                        ? regNo + " - " + fullName
                        : regNo;
                return new PersonDropdownItem(regNo, label);
            }, faculty + "/%", year));
        } catch (Exception e) {
            log.error("Error fetching students for faculty={}, year={}: {}", faculty, year, e.getMessage(), e);
            items = new ArrayList<>();
        }

        return items;
    }

    /**
     * Get students for a given faculty and year, restricted to a set of allowed registration numbers.
     * Used when filtering by approval status.
     */
    public List<PersonDropdownItem> getStudentsByFacultyYearAndIds(String faculty, String year, Set<String> allowedIds) {
        try {
            return getStudentsByFacultyAndYear(faculty, year).stream()
                    .filter(item -> allowedIds.contains(item.getId()))
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching students for faculty={}, year={} with ID filter: {}", faculty, year, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Convert semester number to name.
     * Mirrors PHP getSemName() in methodList.php.
     */
    private String convertSemesterName(String semesterNum) {
        if (semesterNum == null) return "";
        return switch (semesterNum.trim()) {
            case "1" -> "First";
            case "2" -> "Second";
            case "3" -> "Third";
            case "4" -> "Fourth";
            case "5" -> "Fifth";
            case "6" -> "Sixth";
            case "7" -> "Seventh";
            case "8" -> "Eighth";
            case "9" -> "Ninth";
            case "10" -> "Tenth";
            case "11" -> "Eleventh";
            case "12" -> "Twelfth";
            default -> semesterNum;
        };
    }

    /**
     * Build student image URL.
     * Mirrors PHP findimage() / findImagePath() / displayImage() in methodList.php.
     */
    private String buildStudentImageUrl(String regNo) {
        if (regNo != null && !regNo.isBlank()) {
            String encodedRegNo = URLEncoder.encode(regNo, StandardCharsets.UTF_8);
            return "https://stud.pdn.ac.lk/student_image_view.php?regno=" + encodedRegNo;
        }
        return "/images/user.png";
    }
}

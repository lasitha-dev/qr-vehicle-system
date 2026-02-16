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
     * Mirrors PHP: SELECT * FROM stud, studbasic, faculty, course, district, studother
     * WHERE stud.NIC=studbasic.NIC AND stud.Faculty=faculty.Fac_Code 
     * AND stud.Course=course.course_ID AND studother.Dist_No=district.Dist_No 
     * AND stud.NIC=studother.NIC AND Reg_No=?
     */
    public Optional<StudentDetailDTO> getStudentDetail(String regNo) {
        try {
            String sql = """
                SELECT 
                    s.Reg_No, s.App_Year, s.Faculty, s.Course, s.Status, s.RegOn, s.SelectType, s.NIC,
                    sb.Title, sb.Initials, sb.L_Name, sb.Full_Name, sb.Gender, sb.DOB, 
                    sb.Religion, sb.Ethnicity, sb.AL_Stream, sb.AL_Dist, sb.ZScore, sb.AL_Year,
                    f.Fac_name AS FacultyName,
                    c.Course_name AS CourseName,
                    d.Dist_Name AS DistrictName,
                    so.ADD1, so.ADD2, so.ADD3, so.Dist_No, so.Div_No, so.Police,
                    so.Phone, so.Mobile, so.Email,
                    so.Father_Name, so.Mother_Name, so.Guardian_Name, so.Guardian_Add, so.Guardian_Phone,
                    so.Emergency_Name, so.Emergency_Phone
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
                student.setRegisteredOn(rs.getString("RegOn"));
                student.setSelectType(rs.getString("SelectType"));
                student.setNic(rs.getString("NIC"));

                student.setTitle(rs.getString("Title"));
                student.setInitials(rs.getString("Initials"));
                student.setLastName(rs.getString("L_Name"));
                student.setFullName(rs.getString("Full_Name"));
                student.setGender(rs.getString("Gender"));
                student.setDateOfBirth(rs.getString("DOB"));
                student.setReligion(rs.getString("Religion"));
                student.setEthnicity(rs.getString("Ethnicity"));
                student.setAlStream(rs.getString("AL_Stream"));
                student.setAlDistrict(rs.getString("AL_Dist"));
                student.setZScore(rs.getString("ZScore"));
                student.setAlYear(rs.getString("AL_Year"));

                student.setFacultyName(rs.getString("FacultyName"));
                student.setCourseName(rs.getString("CourseName"));
                student.setDistrict(rs.getString("DistrictName"));

                student.setAddress1(rs.getString("ADD1"));
                student.setAddress2(rs.getString("ADD2"));
                student.setAddress3(rs.getString("ADD3"));
                student.setPoliceStation(rs.getString("Police"));

                student.setPhone(rs.getString("Phone"));
                student.setMobile(rs.getString("Mobile"));
                student.setEmail(rs.getString("Email"));

                student.setFatherName(rs.getString("Father_Name"));
                student.setMotherName(rs.getString("Mother_Name"));
                student.setGuardianName(rs.getString("Guardian_Name"));
                student.setGuardianAddress(rs.getString("Guardian_Add"));
                student.setGuardianPhone(rs.getString("Guardian_Phone"));

                student.setEmergencyContactName(rs.getString("Emergency_Name"));
                student.setEmergencyContactPhone(rs.getString("Emergency_Phone"));

                return student;
            }, regNo);

            if (dto != null) {
                // Fetch semester info
                loadSemesterInfo(dto);
                // Fetch academic history
                loadAcademicHistory(dto);
                // Build image URL
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
     */
    public boolean isRegisteredStudent(String regNo) {
        try {
            Integer count = studDbJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stud WHERE Reg_No = ? AND Status = 'REGISTERED'",
                Integer.class, regNo
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get basic student info for vehicle registration self-service.
     * Mirrors PHP insert_vehiclemod.php student lookup.
     */
    public Optional<StudentDetailDTO> getStudentBasicInfo(String regNo) {
        try {
            String sql = """
                SELECT 
                    s.Reg_No, sb.Full_Name, s.App_Year, 
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
            String sql = """
                SELECT DISTINCT SUBSTRING_INDEX(Reg_No,'/',1) AS faculty_prefix
                FROM stud
                WHERE Status='REGISTERED'
                ORDER BY faculty_prefix ASC
                """;
            return studDbJdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("faculty_prefix"));
        } catch (Exception e) {
            log.error("Error fetching distinct faculties: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get distinct academic years for a given faculty prefix.
     * Mirrors PHP: SELECT DISTINCT CASE WHEN LENGTH(...)=2 THEN CONCAT('20',...) ELSE ... END AS YearFull
     *              FROM stud WHERE Status='REGISTERED' AND Reg_No LIKE '{faculty}/%'
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
                WHERE Status='REGISTERED' AND Reg_No LIKE ?
                ORDER BY YearFull ASC
                """;
            return studDbJdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("YearFull"),
                    faculty + "/%");
        } catch (Exception e) {
            log.error("Error fetching years for faculty {}: {}", faculty, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all registered students for a given faculty and year.
     * Mirrors PHP: SELECT Reg_No AS id, Reg_No AS label FROM stud
     *              WHERE Status='REGISTERED' AND Reg_No LIKE CONCAT(?, '/%') AND (year) = ?
     */
    public List<PersonDropdownItem> getStudentsByFacultyAndYear(String faculty, String year) {
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
            return studDbJdbcTemplate.query(sql, (rs, rowNum) -> {
                String regNo = rs.getString("Reg_No");
                String fullName = rs.getString("Full_Name");
                String label = fullName != null && !fullName.isEmpty()
                        ? regNo + " - " + fullName
                        : regNo;
                return new PersonDropdownItem(regNo, label);
            }, faculty + "/%", year);
        } catch (Exception e) {
            log.error("Error fetching students for faculty={}, year={}: {}", faculty, year, e.getMessage(), e);
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

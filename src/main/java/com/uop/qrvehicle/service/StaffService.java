package com.uop.qrvehicle.service;

import com.uop.qrvehicle.dto.StaffDetailDTO;
import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.TemporaryStaff;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.repository.TemporaryStaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Staff Service
 * Provides staff detail lookup and search
 * Mirrors PHP: view_staff.php, search_person.php (staff sections)
 */
@Service
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final StaffRepository staffRepository;
    private final TemporaryStaffRepository temporaryStaffRepository;
    private final ImageService imageService;

    public StaffService(StaffRepository staffRepository,
                       TemporaryStaffRepository temporaryStaffRepository,
                       ImageService imageService) {
        this.staffRepository = staffRepository;
        this.temporaryStaffRepository = temporaryStaffRepository;
        this.imageService = imageService;
    }

    /**
     * Get detailed staff profile by employee number.
     * Tries permanent staff first, then temporary/casual/contract/institute.
     */
    public Optional<StaffDetailDTO> getStaffDetail(String empNo) {
        if (empNo == null || empNo.trim().isEmpty()) {
            return Optional.empty();
        }
        empNo = empNo.trim();

        // Try permanent staff (latest payroll record)
        Optional<Staff> permanentOpt = staffRepository.findLatestByEmpNo(empNo);
        if (permanentOpt.isPresent()) {
            return Optional.of(mapPermanentStaff(permanentOpt.get()));
        }

        // Try temporary/casual/contract/institute
        Optional<TemporaryStaff> tempOpt = temporaryStaffRepository.findByEmpNo(empNo);
        if (tempOpt.isPresent()) {
            return Optional.of(mapTemporaryStaff(tempOpt.get()));
        }

        return Optional.empty();
    }

    /**
     * Search staff by name or employee number
     */
    public List<StaffDetailDTO> searchStaff(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        query = query.trim();

        // Search permanent staff by name
        List<StaffDetailDTO> results = staffRepository.searchByName(query).stream()
                .map(this::mapPermanentStaff)
                .collect(Collectors.toList());

        // Also search temporary staff by name
        temporaryStaffRepository.findByEmpNameContainingIgnoreCase(query).stream()
                .map(this::mapTemporaryStaff)
                .forEach(results::add);

        // Try exact empNo match if no results
        if (results.isEmpty()) {
            getStaffDetail(query).ifPresent(results::add);
        }

        return results;
    }

    /**
     * Map permanent staff entity to DTO
     */
    private StaffDetailDTO mapPermanentStaff(Staff staff) {
        StaffDetailDTO dto = new StaffDetailDTO();
        dto.setEmpNo(staff.getEmpNo());
        dto.setEmpName(staff.getEmpName());
        dto.setNic(staff.getNic());
        dto.setSex(staff.getSex());
        dto.setDateOfBirth(staff.getDateOfBirth());
        dto.setDesignation(staff.getDesignation());
        dto.setCategory(staff.getCategory());
        dto.setEmployeeType(staff.getEmployeeType());
        dto.setDepartment(staff.getDepartment());
        dto.setDepartmentCode(staff.getDepartmentCode());
        dto.setBranchName(staff.getBranchName());
        dto.setLatestSalaryDate(staff.getSalaryDate());

        // Compute expiry
        computeExpiry(dto);

        // Image
        String imgUrl = imageService.getProfileImageUrl("Permanent", staff.getEmpNo());
        if (imgUrl == null) {
            imgUrl = imageService.getProfileImageUrl("Staff", staff.getEmpNo());
        }
        dto.setImageUrl(imgUrl);

        return dto;
    }

    /**
     * Map temporary staff entity to DTO
     */
    private StaffDetailDTO mapTemporaryStaff(TemporaryStaff temp) {
        StaffDetailDTO dto = new StaffDetailDTO();
        dto.setEmpNo(temp.getEmpNo());
        dto.setEmpName(temp.getEmpName());
        dto.setNic(temp.getNic());
        dto.setSex(temp.getSex());
        dto.setDateOfBirth(temp.getDateOfBirth());
        dto.setDesignation(temp.getDesignation());
        dto.setCategory(temp.getCategory());
        dto.setEmployeeType(temp.getEmployeeType());
        dto.setDepartment(temp.getDepartment());
        dto.setDepartmentCode(temp.getDepartmentCode());
        // TemporaryStaff has no branchName field
        dto.setLatestSalaryDate(temp.getSalaryDate());

        computeExpiry(dto);

        String imgUrl = imageService.getProfileImageUrl(temp.getCategory(), temp.getEmpNo());
        if (imgUrl == null) {
            imgUrl = imageService.getProfileImageUrl("Staff", temp.getEmpNo());
        }
        dto.setImageUrl(imgUrl);

        return dto;
    }

    /**
     * Compute expiry date: Academic → DOB+65, Others → DOB+60
     */
    private void computeExpiry(StaffDetailDTO dto) {
        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isEmpty()) {
            try {
                LocalDate dob = LocalDate.parse(dto.getDateOfBirth(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                int retAge = dto.isAcademic() ? 65 : 60;
                LocalDate expiry = dob.plusYears(retAge);
                dto.setExpiryDate(expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (DateTimeParseException e) {
                log.debug("Could not parse DOB for {}: {}", dto.getEmpNo(), dto.getDateOfBirth());
            }
        }
    }
}

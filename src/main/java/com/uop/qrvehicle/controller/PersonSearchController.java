package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Person Search Controller
 * Handles searching for students, staff, and visitors
 */
@Controller
@RequestMapping("/search")
public class PersonSearchController {

    private static final Logger log = LoggerFactory.getLogger(PersonSearchController.class);

    private final PersonService personService;

    public PersonSearchController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/person")
    public String searchPerson(@RequestParam(required = false) String id,
                              @RequestParam(required = false) String regno,
                              Model model) {
        
        // Support both 'id' and 'regno' parameters for compatibility
        String searchId = id != null ? id : regno;
        
        if (searchId != null && !searchId.trim().isEmpty()) {
            try {
                Optional<PersonSearchResult> result = personService.searchPerson(searchId.trim());
                
                if (result.isPresent()) {
                    model.addAttribute("person", result.get());
                    model.addAttribute("found", true);
                } else {
                    model.addAttribute("found", false);
                    model.addAttribute("notFoundMessage", "No record found for: " + searchId);
                }
            } catch (Exception e) {
                log.error("Error processing person search for '{}': {}", searchId, e.getMessage(), e);
                model.addAttribute("found", false);
                model.addAttribute("notFoundMessage", "Error searching for: " + searchId + ". Please try again.");
            }
            
            model.addAttribute("searchId", searchId);
        }
        
        return "person/search";
    }
}

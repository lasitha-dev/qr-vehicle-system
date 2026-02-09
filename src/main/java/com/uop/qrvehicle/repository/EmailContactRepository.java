package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.EmailContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for emailtab table - stores email contacts for bulk emails.
 */
@Repository
public interface EmailContactRepository extends JpaRepository<EmailContact, String> {

    List<EmailContact> findByEmailIsNotNull();
}

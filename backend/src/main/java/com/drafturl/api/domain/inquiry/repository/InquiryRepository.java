package com.drafturl.api.domain.inquiry.repository;

import com.drafturl.api.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
}

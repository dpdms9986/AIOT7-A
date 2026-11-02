package com.aloha.project.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.common.Inquiries;

public interface InquiryRepository extends JpaRepository<Inquiries, Long> {

}
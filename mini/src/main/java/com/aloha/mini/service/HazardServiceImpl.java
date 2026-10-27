package com.aloha.mini.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aloha.mini.domain.HazardReports;
import com.aloha.mini.domain.Regions;
import com.aloha.mini.domain.Users;
import com.aloha.mini.dto.HazardCreateRequest;
import com.aloha.mini.dto.HazardResponse;
import com.aloha.mini.dto.HazardUpdateRequest;
import com.aloha.mini.repository.HazardCommentRepository;
import com.aloha.mini.repository.HazardImageRepository;
import com.aloha.mini.repository.HazardReportRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HazardServiceImpl implements HazardService {

    private final HazardReportRepository hazardReportRepository;
    private final HazardImageRepository hazardImageRepository;
    private final HazardCommentRepository hazardCommentRepository;
    private final EntityManager entityManager;

    // 위험 게시글 등록
    @Override
    @Transactional
    public HazardResponse create(HazardCreateRequest request, Long userNo) {

        Users user = entityManager.find(Users.class, userNo);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "작성자를 찾을 수 없습니다.");
        }

        HazardReports hazardReport = new HazardReports();

        hazardReport.setUser(user);
        hazardReport.setTitle(request.getTitle());
        hazardReport.setContent(request.getContent());
        hazardReport.setHazardType(request.getHazardType());
        hazardReport.setLatitude(request.getLatitude());
        hazardReport.setLongitude(request.getLongitude());
        hazardReport.setStatus("ACTIVE");

        if (request.getRegionNo() != null) {
            Regions region = entityManager.find(
                    Regions.class, request.getRegionNo());

            if (region == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다.");
            }

            hazardReport.setRegion(region);
        }

        HazardReports savedReport =
                hazardReportRepository.save(hazardReport);

        return toResponse(savedReport);
    }

    // 전체 위험 게시글 목록 조회
    @Override
    public List<HazardResponse> findAll() {

        return hazardReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 위험 게시글 상세 조회
    @Override
    public HazardResponse findById(Long no) {

        HazardReports hazardReport = getHazardReport(no);

        return toResponse(hazardReport);
    }

    // 지역별 위험 게시글 목록 조회
    @Override
    public List<HazardResponse> findByRegion(Long regionNo) {

        return hazardReportRepository
                .findByRegion_NoOrderByCreatedAtDesc(regionNo)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 위험 게시글 수정
    @Override
    @Transactional
    public HazardResponse update(
            Long no,
            HazardUpdateRequest request,
            Long userNo,
            boolean isAdmin) {

        HazardReports hazardReport = getHazardReport(no);

        checkPermission(hazardReport, userNo, isAdmin);

        hazardReport.setTitle(request.getTitle());
        hazardReport.setContent(request.getContent());
        hazardReport.setHazardType(request.getHazardType());
        hazardReport.setLatitude(request.getLatitude());
        hazardReport.setLongitude(request.getLongitude());

        if (request.getRegionNo() != null) {
            Regions region = entityManager.find(
                    Regions.class, request.getRegionNo());

            if (region == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다.");
            }

            hazardReport.setRegion(region);
        } else {
            hazardReport.setRegion(null);
        }

        HazardReports updatedReport =
                hazardReportRepository.save(hazardReport);

        return toResponse(updatedReport);
    }

    // 위험 게시글 삭제
    @Override
    @Transactional
    public void delete(Long no, Long userNo, boolean isAdmin) {

        HazardReports hazardReport = getHazardReport(no);

        checkPermission(hazardReport, userNo, isAdmin);

        // 게시글에 연결된 댓글과 이미지부터 삭제
        hazardCommentRepository.deleteAll(
                hazardCommentRepository
                        .findByHazardReport_NoOrderByCreatedAtAsc(no));

        hazardImageRepository.deleteAll(
                hazardImageRepository.findByHazardReport_No(no));

        hazardReportRepository.delete(hazardReport);
    }

    // 게시글 조회 공통 처리
    private HazardReports getHazardReport(Long no) {

        return hazardReportRepository.findById(no)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "위험 게시글을 찾을 수 없습니다."));
    }

    // 작성자 본인 또는 관리자 권한 확인
    private void checkPermission(
            HazardReports hazardReport,
            Long userNo,
            boolean isAdmin) {

        if (isAdmin) {
            return;
        }

        if (userNo == null
                || !userNo.equals(hazardReport.getUser().getNo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "게시글 작성자 또는 관리자만 처리할 수 있습니다.");
        }
    }

    // 엔터티를 응답 DTO로 변환
    private HazardResponse toResponse(HazardReports hazardReport) {

        HazardResponse response = new HazardResponse();

        response.setNo(hazardReport.getNo());
        response.setUserNo(hazardReport.getUser().getNo());
        response.setUserName(hazardReport.getUser().getUsername());

        response.setTitle(hazardReport.getTitle());
        response.setContent(hazardReport.getContent());
        response.setHazardType(hazardReport.getHazardType());

        response.setLatitude(hazardReport.getLatitude());
        response.setLongitude(hazardReport.getLongitude());

        if (hazardReport.getRegion() != null) {
            response.setRegionNo(hazardReport.getRegion().getNo());
        }

        response.setStatus(hazardReport.getStatus());
        response.setCreatedAt(hazardReport.getCreatedAt());
        response.setUpdatedAt(hazardReport.getUpdatedAt());

        return response;
    }
}


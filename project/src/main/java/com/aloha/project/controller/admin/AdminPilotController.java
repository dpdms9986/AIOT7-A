package com.aloha.project.controller.admin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aloha.project.domain.common.Pagination;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.dto.AdminPilotDto;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.TeamRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 조종사 관리
 * GET /admin/pilots?search=&teamNo=&status=&page=1&size=10
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/pilots")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPilotController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    @Transactional(readOnly = true) // WorkAssignments.pilot 이 LAZY 라서 트랜잭션 안에서 처리
    @GetMapping
    public String list(
            @RequestParam(value = "search", defaultValue = "") String search,   // 이름 / 아이디 / 연락처
            @RequestParam(value = "teamNo", defaultValue = "") String teamNo,   // 방제단 번호 (빈값=전체)
            @RequestParam(value = "status", defaultValue = "") String status,   // WORKING / READY / DISABLED
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        size = (size < 1 || size > 100) ? 10 : size;

        // TODO(백엔드) 아래는 findAll() 로 전체를 읽어 메모리에서 계산 중 (데이터가 많아지면 느려짐)
        //   - UserRepository : role=PILOT 조회 + 검색/방제단/상태 필터 + 페이징 쿼리
        //   - WorkAssignmentRepository : 조종사별 상태별 건수 집계 쿼리 (GROUP BY pilot)
        Map<Long, List<WorkAssignments>> worksByPilot = workAssignmentRepository.findAll().stream()
                .filter(w -> w.getPilot() != null)
                .collect(Collectors.groupingBy(w -> w.getPilot().getNo()));

        List<AdminPilotDto> all = userRepository.findAll().stream()
                .filter(u -> "PILOT".equals(u.getRole()))
                .map(u -> AdminPilotDto.of(u, worksByPilot.getOrDefault(u.getNo(), List.of())))
                .sorted(Comparator.comparing(AdminPilotDto::getNo).reversed())
                .toList();

        // 요약 카드 (필터와 상관없는 전체 기준)
        long totalCount = all.size();
        long workingCount = all.stream().filter(p -> "WORKING".equals(p.getStatus())).count();
        long disabledCount = all.stream().filter(p -> "DISABLED".equals(p.getStatus())).count();

        // 필터
        String kw = search.trim().toLowerCase();
        List<AdminPilotDto> filtered = all.stream()
                .filter(p -> kw.isEmpty()
                        || (p.getName() != null && p.getName().toLowerCase().contains(kw))
                        || (p.getUserName() != null && p.getUserName().toLowerCase().contains(kw))
                        || (p.getPhone() != null && p.getPhone().contains(kw)))
                .filter(p -> teamNo.isEmpty() || (p.getTeamNo() != null && p.getTeamNo().toString().equals(teamNo)))
                .filter(p -> status.isEmpty() || status.equals(p.getStatus()))
                .toList();

        // 페이징 (메모리)
        Pagination pagination = new Pagination(Math.max(page, 1), size, 10, filtered.size());
        if (pagination.getPage() > pagination.getLast()) {
            pagination = new Pagination(pagination.getLast(), size, 10, filtered.size());
        }
        int from = (int) Math.min(pagination.getIndex(), filtered.size());
        int to = (int) Math.min(from + size, filtered.size());

        model.addAttribute("pilots", filtered.subList(from, to));
        model.addAttribute("pagination", pagination);
        model.addAttribute("teams", teamRepository.findAll());
        model.addAttribute("search", search);
        model.addAttribute("teamNo", teamNo);
        model.addAttribute("status", status);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("workingCount", workingCount);
        model.addAttribute("disabledCount", disabledCount);

        return "page/admin/pilots";
    }
}
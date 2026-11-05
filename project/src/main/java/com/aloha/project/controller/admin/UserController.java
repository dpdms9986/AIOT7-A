package com.aloha.project.controller.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.Pagination;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.service.inter.users.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원 목록 페이지
     * - 관리자(ADMIN)는 목록에서 제외
     * - 권한 / 활성 / 가입일 기간 필터는 여기서 처리
     */
    @GetMapping
    public String list(
        HttpServletRequest request,
        Model model, Pagination pagination,
        QueryParams queryParams
    ) {
        // 1) 검색어 조건으로 전체 조회 (페이징은 아래에서 직접 처리)
        QueryParams all = new QueryParams();
        all.setPage(1);
        all.setSize(Integer.MAX_VALUE);
        all.setSearch(queryParams.getSearch());
        List<Users> users = userService.page(all).getList();

        // 2) 필터
        Map<String, Object> params = queryParams.getParams();
        String role = param(params, "role");        // LEADER | PILOT
        String enabled = param(params, "enabled");  // 1 | 0
        LocalDate start = toDate(param(params, "start"));
        LocalDate end = toDate(param(params, "end"));

        List<Users> filtered = users.stream()
            .filter(u -> !"ADMIN".equals(u.getRole()))                                    // 관리자 제외
            .filter(u -> role == null || role.equals(u.getRole()))                        // 권한
            .filter(u -> enabled == null
                    || ("1".equals(enabled) == Boolean.TRUE.equals(u.getEnabled())))      // 활성
            .filter(u -> start == null || (u.getCreatedAt() != null
                    && !u.getCreatedAt().toLocalDate().isBefore(start)))                  // 가입일 시작
            .filter(u -> end == null || (u.getCreatedAt() != null
                    && !u.getCreatedAt().toLocalDate().isAfter(end)))                     // 가입일 끝
            .toList();

        // 3) 페이징
        int page = queryParams.getPage() == null || queryParams.getPage() < 1 ? 1 : queryParams.getPage();
        int size = queryParams.getSize() == null || queryParams.getSize() < 1 ? 10 : queryParams.getSize();
        long total = filtered.size();
        int from = (int) Math.min((long) (page - 1) * size, total);
        int to = (int) Math.min((long) from + size, total);

        PageInfo<Users> pageInfo = new PageInfo<>(filtered.subList(from, to), total, page, size);
        model.addAttribute("pageInfo", pageInfo);

        pagination.setPage(page);
        pagination.setSize(size);
        pagination.setTotal(total);
        model.addAttribute("pagination", pagination);

        String path = request.getServletPath();
        String pageUri = UriComponentsBuilder.fromPath(path)
                                             .queryParam("size", pagination.getSize())
                                             .build()
                                             .toUriString();
        model.addAttribute("pageUri", pageUri);
        return "page/admin/users/list";
    }

    private String param(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) return null;
        String v = params.get(key).toString().trim();
        return v.isEmpty() ? null : v;
    }

    private LocalDate toDate(String v) {
        if (v == null) return null;
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 회원 등록 페이지
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("user", new Users());
        return "page/admin/users/create";
    }

}
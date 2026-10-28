package com.aloha.project.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.aloha.project.domain.common.Pagination;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.service.inter.users.UserService;
import com.aloha.project.domain.common.PageInfo;

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
     */
    @GetMapping
    public String list(
        HttpServletRequest request,
        Model model, Pagination pagination, 
        QueryParams queryParams
    ) {
        PageInfo<Users> pageInfo = userService.page(queryParams);
        log.info("pageInfo : {}",pageInfo);
        model.addAttribute("pageInfo", pageInfo);
        Long total = pageInfo.getTotal();
        pagination.setPage(queryParams.getPage());
        pagination.setSize(queryParams.getSize());
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

    /**
     * 회원 등록 페이지
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("user", new Users());
        return "page/admin/users/create";
    }

   
    /**
     * 회원 수정 페이지
     */
    @GetMapping("/update/{id}")
    public String updateForm(@PathVariable("id") String id, Model model) {
        Users user = userService.selectById(id);
        model.addAttribute("user", user);
        return "page/admin/users/update";
    }

}

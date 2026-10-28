package com.aloha.project.controller.admin;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.aloha.project.domain.common.Board;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.Pagination;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.service.inter.common.BoardService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/board")
@RequiredArgsConstructor 
public class BoardController {

  private final BoardService boardService;

    @GetMapping("")
    public String list(
        HttpServletRequest request,
        Model model, 
        Pagination pagination,
        QueryParams queryParams
    ) {
        PageInfo<Board> pageInfo = boardService.page(queryParams);
        log.info("pageInfo : {}", pageInfo);
        
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
        
        // 게시판 유형 옵션
        List<String> typeOptions = Arrays.asList("공지사항", "FAQ", "이용안내", "자료실");
        model.addAttribute("typeOptions", typeOptions);
        
        return "page/admin/board/list";
    }


    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("boardId", java.util.UUID.randomUUID().toString());
        return "page/admin/board/create";
    }

    @GetMapping("/{id}")
    public String update(@PathVariable("id") String id, Model model) {
        Board board = boardService.selectById(id);
        if (board == null) {
            log.error("Board with id {} not found", id);
            return "redirect:/admin/board";
        }
        
        model.addAttribute("board", board);
        return "page/admin/board/update";
    }
  
}

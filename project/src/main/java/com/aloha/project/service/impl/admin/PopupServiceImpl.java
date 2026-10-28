package com.aloha.project.service.impl.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aloha.project.domain.admin.Popups;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.repository.admin.PopupRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.admin.PopupService;
import com.aloha.project.service.inter.common.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class PopupServiceImpl extends BaseServiceImpl<Popups, PopupRepository> implements PopupService {

    private final MediaService mediaService;

    @Override
    public PageInfo<Popups> page(QueryParams queryParams) {
        log.info("queryParams : {}", queryParams);
        return repository.search(queryParams);
    }

    @Override
    public List<Popups> listByType(String type) {
        return repository.findByType(type);
    }

    @Override
    public List<Popups> listByTypeOpen(String type) {
        return repository.findOpenByType(type, LocalDateTime.now());
    }

    @Override
    public boolean delete(Long no) {
        try {
            // 팝업 정보 조회
            Popups popup = repository.findById(no).orElse(null);
            if (popup == null) {
                log.warn("Popup not found with no: {}", no);
                return false;
            }
            
            deleteMedia(popup);
            
            // 팝업 삭제
            repository.delete(popup);
            repository.flush();
            log.info("Popup deleted successfully - no: {}", no);
            return true;
        } catch (Exception e) {
            log.error("Error deleting popup with no: {}", no, e);
            markRollbackOnly();
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            // id로 팝업 정보 조회
            Popups popup = repository.findOneById(id).orElse(null);
            if (popup == null) {
                log.warn("Popup not found with id: {}", id);
                return false;
            }
            
            deleteMedia(popup);
            
            // 팝업 삭제
            repository.delete(popup);
            repository.flush();
            log.info("Popup deleted successfully - id: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error deleting popup with id: {}", id, e);
            markRollbackOnly();
            return false;
        }
    }

    // 팝업에 연결된 미디어 삭제 (URL 기준)
    private void deleteMedia(Popups popup) {
        if (popup.getUrl() != null && !popup.getUrl().trim().isEmpty()) {
            boolean mediaDeleted = mediaService.deleteMediaByUrl(popup.getUrl());
            if (mediaDeleted) {
                log.info("Associated media deleted for popup id: {}", popup.getId());
            } else {
                log.warn("Failed to delete associated media for popup id: {}", popup.getId());
            }
        }
    }

}

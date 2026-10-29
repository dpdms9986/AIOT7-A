
package com.aloha.mini.service.inter.admin;

import com.aloha.mini.dto.LandUpdateRequest;

public interface AdminService {

    // 농지 정보 수정
    void updateLand(Long landNo, LandUpdateRequest req);

    // 농지 삭제
    void deleteLand(Long landNo);
}

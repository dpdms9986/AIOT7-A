package com.aloha.project.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 드론·자산 한 건 (드론 기체 또는 약제)
 * ※ 백엔드 약속: 서비스는 List<AssetDto> 로 넘겨준다
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDto {

    /** 상태 코드 → 화면 글자 (드론, 약제 공통) */
    public static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();
    static {
        STATUS_LABELS.put("normal", "정상");
        STATUS_LABELS.put("low", "부족");
        STATUS_LABELS.put("operating", "운용 중");
        STATUS_LABELS.put("maintenance", "정비 필요");
        STATUS_LABELS.put("in-maintenance", "정비 중");
        STATUS_LABELS.put("fault", "고장");
    }

    private String id;               // 자산 코드  예) DR-001, CH-001
    private String type;             // drone / chemical
    private String name;             // 이름 / 모델명
    private String subtitle;         // 드론: 기체 설명 / 약제: 분류
    private String owner;            // 담당 조종사
    private String status;           // STATUS_LABELS 의 키
    private LocalDate lastManagedAt; // 최근 관리일 (없으면 null)

    // 드론만
    private Integer tankCapacity;    // 약제 탱크 용량 (L)
    private Integer batteryCycles;   // 배터리 사이클

    // 약제만
    private Double stock;            // 현재 재고량 (L)

    // 정비 이력(드론) / 입고 이력(약제)
    @Builder.Default
    private List<AssetLogDto> logs = new ArrayList<>();

    /* ===== 화면용 getter ===== */

    public boolean isDrone() {
        return "drone".equals(type);
    }

    public String getTypeLabel() {
        return isDrone() ? "🚁 드론" : "🧪 약제";
    }

    public String getStatusLabel() {
        return STATUS_LABELS.getOrDefault(status, "확인 필요");
    }

    // 정비 현황 탭에 나올 자산
    public boolean isMaintenanceNeeded() {
        return "maintenance".equals(status) || "fault".equals(status) || "in-maintenance".equals(status);
    }

    // 상세의 "현재 가동 상태" 박스 색 (style.css 의 asset-current-status.*)
    public String getCurrentStatusClass() {
        if ("normal".equals(status)) return "available";
        if ("operating".equals(status)) return "in-use";
        return "maintenance";
    }

    // 상태 변경 셀렉트에 보여 줄 옵션 (드론과 약제가 다름)
    public Map<String, String> getStatusOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        List<String> keys = isDrone()
                ? List.of("normal", "operating", "maintenance", "in-maintenance", "fault")
                : List.of("normal", "low");
        keys.forEach(k -> options.put(k, STATUS_LABELS.get(k)));
        return options;
    }
}
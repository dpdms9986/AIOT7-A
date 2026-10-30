package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 농지 등록 폼 → 컨트롤러 @ModelAttribute 로 받음
 * ※ 필드 이름 = HTML input 의 name (대소문자까지 같아야 함)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class LandCreateRequest {

    private String city;       // name="city"
    private String region;     // name="region"
    private String dong;       // name="dong"
    private String address;    // name="address"
    private Double latitude;   // name="latitude"   (비우면 null)
    private Double longitude;  // name="longitude"  (비우면 null)
    private Double areaSize;   // name="areaSize"
}
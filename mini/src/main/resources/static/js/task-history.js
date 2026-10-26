// 작업이력
// 필터 검색, 표 그리기, 합계, CSV 다운로드

// 가짜데이터로 확인 연결되면 false로 돌리기
const HISTORY_USE_MOCK = true;

// api주소 후에 확인하고 수정하기
const HISTORY_API = "/api/tasks/history";

// 가짜 데이터 true 일 때만 사용
const HISTORY_MOCK = [
    { completedAt: "2026-10-20 15:30", 
        address: "어남동 12-3", 
        region: "대전시 유성구 어남동",
        area: 800, 
        pilotName: "김조종", 
        assignedAt: "2026-10-20 09:00",
        pasticide: "프로피코나졸",
        amount: 12.5
    },
    
    { completedAt: "2026-10-21 11:10", 
        address: "노은동 2-6", 
        region: "대전시 유성구 노은동",
        area: 600, 
        pilotName: "이드론", 
        assignedAt: "2026-10-21 08:30",
        pasticide: "아족시스트로빈",
        amount: 9.0
    },
    
    { completedAt: "2026-10-22 16:45", 
        address: "관저동 9-4", 
        region: "대전시 서구 관저동",
        area: 1050, 
        pilotName: "김조종", 
        assignedAt: "2026-10-22 10:00",
        pasticide: "프로피코나졸",
        amount: 16.2
    }
] ;

document.addEventListener("DOMContentLoaded", ())
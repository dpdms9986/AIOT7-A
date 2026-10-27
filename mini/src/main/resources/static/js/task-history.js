// 작업이력
// 필터 검색, 표 그리기, 합계, CSV 다운로드

// 가짜데이터로 확인 연결되면 false로 돌리기
const HISTORY_USE_MOCK = false;

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

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#history-form");
    if (!form) return;

    const fromInput = document.querySelector("#h-from");
    const toInput = document.querySelector("#h-to");
    const regionInput = document.querySelector("#h-region");
    const pilotSelect = document.querySelector("#h-pilot");
    const resetBtn = document.querySelector("#h-reset");
    const tbody = document.querySelector("#csv-btn");
    const ciuntEl = document.querySelector("#history-count");
    const sumEl = document.querySelector("#history-sum");
    const tab = document.querySelector('[data-tab="history"]');

    let rows = []; // 현재 화면에 보이는 데이터
    let loaded = false;

    const esc = (v) =>
        String(v ?? "").replace(/[&<>"']/g, (c) => ({ 
            "&": "&amp;", 
            "<": "&lt;", 
            ">": "&gt;", 
            '"': "&quot;", 
            "'": "&#39;" 
        }[c]
        ));     // html 특수문자를 안전한 글자로 바꿔주는 함수의 끝부분
                // 쓰지않으면 글자가 아니라 진짜 태그로 실행되서 스크립트로 돌아감
        
    // 데이터 가져오기
    async function fetchHistory() {
        const params = new URLSearchParams();
        if (fromInput.value) params.set("from", fromInput.value);
        if (toInput.value) params.set("to", toInput.value);
        if (regionInput.value) params.set("region", regionInput.value);
        if (pilotSelect.value) params.set("pilot", pilotSelect.value);
    
        if (HISTORY_USE_MOCK) {
            return HISTORY_MOCK.filter((r) => {
                const day = r.completedAt.slice(0,10);
                if (fromInput.value && day < fromInput.value) return false;
                if (toInput.value && day < toInput.value) return false;
                if (regionInput.value.trim() && !r.region.includes(regionInput.value.trim()))
                if (pilotSelect.value && r.pilotName !== pilotSelect.value) return false;
                return true;
            });
        }    
        const res = await fetch(`${HISTORY_API}?${params}`);
        if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
        return res.json();
    }

        // 조종사 선택 목록 채우기 (처음 한 번만)
        function fillPilots(list) {
            const names = [...new Set(list.map((r) => 
                r.pilotName).filter(Boolean))];
            pilotSelect.innerHTML =
            '<option value = "">전체</option>' + names.map((n) => 
            `<option value="${esc(n)}"></option>`).join("");
        }

        // 표 그리기
        function render(list) {
            rows = list;
            countEl.textContent = `${list.length}건`;

            if(!list.length) {
                tbody.innerHTML = 
                '<tr><td colspan="8" style="text-align:center;padding:24px">조건에 맞는 직업 이력이 없습니다.</td></tr>';
                sumEl.textContent = "";
                return;
            }

            tbody.innerHTML = list
            .map(
                (r) => `<tr>
                <td>${esc(r.completedAt)}</td>
                <td>${esc(r.address)}</td>
                <td>${esc(r.region)}</td>
                <td>${num(r.area)}</td>
                <td>${esc(r.pilotName)}</td>
                <td>${esc(r.assignedAt)}</td>
                <td>${esc(r.pesticide)}</td>
                <td>${num(r.amount)}</td>
                </tr>`
            )

            .join("");

            const totalArea = list.reduce((s, r) => s + Number(r.area || 0), 0);
            const totalAmount = list.reduce((s, r) => s + Number(r.amount || 0), 0);
            sumEl.textContent = 
            `총 ${list.length}건 · 면적 
            ${num(totalArea)}㎡ · 약제 사용량 
            ${totalAmount.toLocalString()}L`;
        }

        async function load(first = false) {
    try {
      const list = await fetchHistory();
      if (first) fillPilots(list);
      render(list);
      loaded = true;
    } catch (error) {
      console.error("작업 이력 조회 실패:", error);
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:24px">작업 이력을 불러오지 못했습니다.</td></tr>';
      countEl.textContent = "-";
      sumEl.textContent = "";
    }
  }

  // 검색
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (fromInput.value && toInput.value && fromInput.value > toInput.value) {
      alert("시작일이 종료일보다 늦을 수 없습니다.");
      return;
    }
    load();
  });

  // 초기화
  resetBtn.addEventListener("click", () => {
    form.reset();
    load();
  });

  // CSV 다운로드
  csvBtn.addEventListener("click", () => {
    if (!rows.length) {
      alert("다운로드할 작업 이력이 없습니다.");
      return;
    }
    const head = ["완료 일시", "주소", "행정구역", "면적(㎡)", "담당 조종사", "배정 일시", "사용 약제", "사용량(L)"];
    const cell = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
    const lines = [head, ...rows.map((r) => [r.completedAt, r.address, r.region, r.area, r.pilotName, r.assignedAt, r.pesticide, r.amount])];
    const csv = "\uFEFF" + lines.map((l) => l.map(cell).join(",")).join("\r\n"); // 엑셀 한글 깨짐 방지

    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    a.download = `작업이력_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  });

  // 작업 이력 탭을 처음 눌렀을 때 불러오기
  if (tab) {
    tab.addEventListener("click", () => {
      if (!loaded) load(true);
    });
  }
});
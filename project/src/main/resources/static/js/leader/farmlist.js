// 농지 관리
// - 목록 / 검색 / 등록 / 삭제 처리 : 서버 (타임리프 + 컨트롤러)
// - JS : 상세 패널 + 지도, 삭제 칸 채우기, 등록 모달 열고 닫기
document.addEventListener("DOMContentLoaded", () => {
  const $ = (sel) => document.querySelector(sel);

  /* ===================== 1. 상세 패널 ===================== */
  const drawer = $("#farm-detail-drawer");
  const backdrop = $("#farm-detail-backdrop");
  const closeBtn = $("#close-farm-detail");
  const deleteNoInput = $("#delete-land-no");
  const deleteBtn = $("#delete-farm-button");
  const assignLink = $("#assign-farm-link");
  const drawerStatus = $("#drawer-status");
  let lastTrigger = null;

  const openDrawer = (btn) => {
    const d = btn.dataset;   // th:data-* 로 심어둔 값들
    lastTrigger = btn;

    // 패널 내용 채우기
    $("#drawer-address").textContent = d.address;
    $("#drawer-region").textContent = d.fullRegion;
    drawerStatus.textContent = d.status;
    drawerStatus.className = `status-badge ${d.statusClass}`;   // 색 클래스 교체
    $("#drawer-id").textContent = `LAND-${d.no.padStart(4, "0")}`;
    $("#drawer-coordinates").textContent = d.lat && d.lng ? `${d.lat}, ${d.lng}` : "미등록";
    $("#drawer-pilot").textContent = d.pilot;
    $("#drawer-assigned-at").textContent = d.assignedAt;

    // 삭제 폼의 숨은 칸에 번호 넣기 + 대기 상태가 아니면 버튼 비활성
    deleteNoInput.value = d.no;
    const deletable = d.deletable === "true";   // data- 값은 항상 글자
    deleteBtn.disabled = !deletable;
    deleteBtn.title = deletable ? "" : "대기 상태인 농지만 삭제할 수 있습니다.";

    // 작업 배정 링크 (주소는 타임리프가 만들어 둠)
    assignLink.href = d.assignUrl;

    // 패널 열기
    drawer.hidden = false;
    backdrop.hidden = false;
    drawer.inert = false;
    drawer.setAttribute("aria-hidden", "false");
    document.body.classList.add("farm-drawer-open");

    requestAnimationFrame(() => {
      drawer.classList.add("is-open");
      closeBtn.focus();
      showMap(d);   // 패널이 열린 다음 지도 표시 (크기 계산 때문)
    });
  };

  const closeDrawer = () => {
    drawer.classList.remove("is-open");
    drawer.inert = true;
    drawer.setAttribute("aria-hidden", "true");
    backdrop.hidden = true;
    document.body.classList.remove("farm-drawer-open");
    setTimeout(() => {
      drawer.hidden = true;
      lastTrigger?.focus();
    }, 260);   // 닫히는 애니메이션 시간
  };

  document.querySelectorAll(".detail-button").forEach((btn) => {
    btn.addEventListener("click", () => openDrawer(btn));
  });
  closeBtn.addEventListener("click", closeDrawer);
  backdrop.addEventListener("click", closeDrawer);

  /* ===================== 2. 지도 (Leaflet) ===================== */
  const DEFAULT_POS = [36.3504, 127.3845];   // 좌표 없을 때 기본 위치 (대전)
  let map = null;
  let marker = null;

  const showMap = (d) => {
    const status = $("#farm-detail-map-status");
    if (typeof L === "undefined") {
      status.textContent = "지도를 불러오지 못했습니다.";
      return;
    }

    // 지도는 처음 한 번만 만들기
    if (!map) {
      map = L.map("farm-detail-map").setView(DEFAULT_POS, 13);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors",
      }).addTo(map);
    }
    map.invalidateSize();   // 숨겨져 있던 패널이 열렸으니 크기 다시 계산

    // 이전 마커 지우기
    if (marker) { map.removeLayer(marker); marker = null; }

    const lat = Number(d.lat), lng = Number(d.lng);
    if (d.lat && d.lng && Number.isFinite(lat) && Number.isFinite(lng)) {
      map.setView([lat, lng], 16);
      marker = L.marker([lat, lng]).addTo(map).bindPopup(d.address);
      status.textContent = "농지 위치 표시 중";
    } else {
      map.setView(DEFAULT_POS, 13);
      status.textContent = "농지 좌표 미등록";
    }
  };

  /* ===================== 3. 등록 모달 ===================== */
  const modal = $("#farm-register-modal");

  const openModal = () => {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
    modal.querySelector("input[name='dong']").focus();
  };
  const closeModal = () => {
    modal.hidden = true;
    document.body.style.overflow = "";
  };

  $("#register-button").addEventListener("click", openModal);
  modal.querySelectorAll("[data-close-modal]").forEach((b) => b.addEventListener("click", closeModal));
  modal.addEventListener("click", (e) => { if (e.target === modal) closeModal(); });

  /* ===================== 4. ESC 로 닫기 ===================== */
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (!modal.hidden) closeModal();
    else if (!drawer.hidden) closeDrawer();
  });
});
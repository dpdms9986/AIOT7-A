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
  const regForm = modal.querySelector("form");
  const field = (name) => regForm.querySelector(`[name='${name}']`);
  const addrInput = $("#farm-addr");
  const addrBtn = $("#farm-addr-btn");
  const addrMsg = $("#farm-addr-msg");
  const latInput = field("latitude");
  const lngInput = field("longitude");
  const ADDR_HINT = "주소를 고르면 지도와 위도·경도가 자동으로 맞춰져요. 지도를 클릭해 미세 조정할 수 있어요.";

  let pickMap = null;
  let pickMarker = null;

  // 위도·경도 칸 + 마커 (+ 지도 이동) 한 번에 맞추기
  const setPick = (lat, lng, zoom) => {
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);
    if (!pickMap) return;
    if (pickMarker) pickMarker.setLatLng([lat, lng]);
    else pickMarker = L.marker([lat, lng]).addTo(pickMap);
    if (zoom) pickMap.setView([lat, lng], zoom);
  };

  // 마커 지우고 지도를 기본 위치로
  const resetPick = () => {
    if (!pickMap) return;
    if (pickMarker) { pickMap.removeLayer(pickMarker); pickMarker = null; }
    pickMap.setView(DEFAULT_POS, 13);
  };

  const openModal = () => {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
    regForm.reset();                    // 지난번에 입력한 값 지우기
    addrMsg.textContent = ADDR_HINT;

    // 팝업 안 작은 지도 (처음 열 때 한 번만 만들기)
    if (typeof L !== "undefined") {
      if (!pickMap) {
        pickMap = L.map("farm-pick-map").setView(DEFAULT_POS, 13);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution: "&copy; OpenStreetMap",
        }).addTo(pickMap);
        pickMap.on("click", (e) => setPick(e.latlng.lat, e.latlng.lng));   // 지도 클릭 → 위치 지정
      }
      resetPick();
      setTimeout(() => pickMap.invalidateSize(), 0);   // 숨겨져 있던 지도는 크기 다시 계산
    }
    addrBtn.focus();
  };
  const closeModal = () => {
    modal.hidden = true;
    document.body.style.overflow = "";
  };

  $("#register-button").addEventListener("click", openModal);
  modal.querySelectorAll("[data-close-modal]").forEach((b) => b.addEventListener("click", closeModal));

  /* [나중에 다시 켤 때 이 주석만 지우세요]
  // 배경에서 눌렀다가 배경에서 뗐을 때만 닫기 (팝업 안에서 드래그해도 안 꺼짐)
  let downOnBackdrop = false;
  modal.addEventListener("pointerdown", (e) => { downOnBackdrop = (e.target === modal); });
  modal.addEventListener("click", (e) => {
    if (e.target === modal && downOnBackdrop) closeModal();
    downOnBackdrop = false;
  });
  */

  // 위도·경도를 직접 고치면 지도 마커도 이동
  [latInput, lngInput].forEach((input) => {
    input.addEventListener("change", () => {
      const lat = parseFloat(latInput.value);
      const lng = parseFloat(lngInput.value);
      if (Number.isNaN(lat) || Number.isNaN(lng)) return;
      setPick(lat, lng, 16);
    });
  });

  // 주소 검색을 안 하고 등록하려 할 때 막기 (숨은 칸은 브라우저가 검사하지 않음)
  regForm.addEventListener("submit", (e) => {
    if (!field("city").value || !field("dong").value || !field("address").value) {
      e.preventDefault();
      const msg = "주소 검색 버튼으로 농지 주소를 먼저 선택해 주세요.";
      if (window.showPopup) showPopup(msg, "warn");
      else alert(msg);
    }
  });

  /* ===================== 3-1. 주소 검색 → 위치 자동 입력 ===================== */
  // 다음 우편번호는 시/도를 짧게 주므로("대전") 정식 이름으로 바꿔서 채우기
  const SIDO_FULL = {
    "서울": "서울특별시", "부산": "부산광역시", "대구": "대구광역시", "인천": "인천광역시",
    "광주": "광주광역시", "대전": "대전광역시", "울산": "울산광역시", "세종": "세종특별자치시",
    "경기": "경기도", "강원": "강원특별자치도", "충북": "충청북도", "충남": "충청남도",
    "전북": "전북특별자치도", "전남": "전라남도", "경북": "경상북도", "경남": "경상남도",
    "제주": "제주특별자치도",
  };

  // 주소 → 좌표 (OpenStreetMap Nominatim, 위험 구역 신고와 같은 방식)
  async function geocode(address) {
    const url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=kr&q="
              + encodeURIComponent(address);
    const res = await fetch(url, { headers: { "Accept-Language": "ko" } });
    if (!res.ok) return null;
    const list = await res.json();
    return list.length ? { lat: parseFloat(list[0].lat), lng: parseFloat(list[0].lon) } : null;
  }

  // 못 찾으면 주소 끝부분(번지 등)을 하나씩 떼면서 다시 시도
  async function geocodeLoose(address) {
    let words = address.trim().split(/\s+/);
    while (words.length >= 2) {
      const pos = await geocode(words.join(" "));
      if (pos) return { pos, exact: words.join(" ") === address.trim() };
      words = words.slice(0, -1);
    }
    return null;
  }

  async function findCoords(road, jibun) {
    latInput.value = "";      // 이전 검색의 좌표가 남지 않게 비우기
    lngInput.value = "";
    resetPick();
    addrMsg.textContent = "위치를 찾는 중이에요...";

    const found = (await geocodeLoose(road)) || (jibun ? await geocodeLoose(jibun) : null);
    if (!found) {
      addrMsg.textContent = "주소 위치를 찾지 못했어요. 지도를 클릭해 위치를 찍어 주세요.";
      if (window.showPopup) showPopup("주소 위치를 찾지 못했어요.\n지도에서 직접 위치를 눌러 주세요.", "warn");
      return;
    }
    setPick(found.pos.lat, found.pos.lng, found.exact ? 17 : 15);
    addrMsg.textContent = found.exact
      ? "주소 위치로 맞췄어요. 필요하면 지도를 클릭해 미세 조정하세요."
      : "정확한 번지는 못 찾아서 근처로 맞췄어요. 지도를 클릭해 정확한 위치를 찍어 주세요.";
  }

  addrBtn.addEventListener("click", () => {
    // 다음(카카오) 우편번호 검색 (script.html 에서 이미 불러옴)
    if (typeof daum === "undefined" || !daum.Postcode) {
      if (window.showPopup) showPopup("주소 검색을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.", "warn");
      return;
    }
    new daum.Postcode({
      oncomplete: (data) => {
        const road = data.roadAddress || data.address;
        const jibun = data.jibunAddress || data.autoJibunAddress || "";
        addrInput.value = road;

        // 서버로 보낼 숨은 칸 채우기
        field("city").value = SIDO_FULL[data.sido] || data.sido;
        field("region").value = data.sigungu || "";
        field("dong").value = data.bname1 || data.bname;

        // 상세 주소: 지번 주소에서 "시/도 시/군/구" 부분을 떼고 저장 (예: "전민동 15-2")
        const prefix = [data.sido, data.sigungu].filter(Boolean).join(" ") + " ";
        const base = jibun || road;
        field("address").value = base.startsWith(prefix) ? base.slice(prefix.length) : base;

        findCoords(road, jibun);
      },
    }).open();
  });

  /* ===================== 4. ESC 로 닫기 ===================== */
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (!modal.hidden) closeModal();
    else if (!drawer.hidden) closeDrawer();
  });
});
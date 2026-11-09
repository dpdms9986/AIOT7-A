/* ===== 위험 구역 (hazards.js) =====
 * 표는 서버(타임리프)가 그림. 여기서는
 *  1) 행의 data- 값을 읽어 지도에 마커 찍기
 *  2) 행/마커 클릭 → 지도 이동 + 상세 보여 주기
 *  3) 신고 팝업 열기/닫기 + 지도 클릭으로 위도·경도 채우기
 */
document.addEventListener("DOMContentLoaded", () => {
  if (typeof L === "undefined") return;           // 지도 라이브러리가 없으면 중단

  const DEFAULT_POS = [36.3504, 127.3845];        // 기본 위치 (대전)
  const COLORS = { "고위험": "#d93025", "중위험": "#f29900", "저위험": "#1e8e3e" };
  const RESOLVED_COLOR = "#9aa0a6";
  const TILE = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const TILE_OPT = { maxZoom: 19, attribution: "&copy; OpenStreetMap" };

  const badgeClass = (type) => (type === "고위험" ? "hz-high" : type === "중위험" ? "hz-mid" : "hz-low");

  // 배경에서 눌렀다가 배경에서 뗐을 때만 닫기 (팝업 안에서 드래그해도 안 꺼짐)
  function closeOnBackdrop(overlay, close) {
    let downOnBackdrop = false;
    overlay?.addEventListener("pointerdown", (e) => { downOnBackdrop = (e.target === overlay); });
    overlay?.addEventListener("click", (e) => {
      if (e.target === overlay && downOnBackdrop) close();
      downOnBackdrop = false;
    });
  }

  /* ---------- 1) 큰 지도 + 마커 ---------- */
  const mapEl = document.getElementById("hz-map");
  const rows = [...document.querySelectorAll(".hz-row")];
  const markers = new Map();                      // 행 → 마커
  let map = null;

  if (mapEl) {
    map = L.map(mapEl).setView(DEFAULT_POS, 12);
    L.tileLayer(TILE, TILE_OPT).addTo(map);

    rows.forEach((row) => {
      const d = row.dataset;
      const lat = parseFloat(d.lat);
      const lng = parseFloat(d.lng);
      if (Number.isNaN(lat) || Number.isNaN(lng)) return;

      const resolved = d.status === "RESOLVED";
      const marker = L.circleMarker([lat, lng], {
        radius: 9,
        color: "#ffffff",
        weight: 2,
        fillColor: resolved ? RESOLVED_COLOR : (COLORS[d.type] || COLORS["중위험"]),
        fillOpacity: 0.9,
      }).addTo(map);

      marker.bindTooltip(d.title || "위험 구역");
      marker.on("click", () => select(row, true));
      markers.set(row, marker);
    });

    // 마커가 있으면 전부 보이게 화면 맞추기
    if (markers.size > 0) {
      const bounds = L.latLngBounds([...markers.values()].map((m) => m.getLatLng()));
      map.fitBounds(bounds, { padding: [30, 30], maxZoom: 15 });
    }
  }

  /* ---------- 2) 선택 → 지도 이동 + 상세 ---------- */
  const detail = document.getElementById("hz-detail");
  const hint = document.getElementById("hz-detail-hint");
  const $ = (id) => document.getElementById(id);

  function select(row, fromMarker) {
    if (fromMarker) showPageOf(row);   // 마커를 눌렀는데 그 행이 다른 페이지면 그 페이지로 이동
    rows.forEach((r) => r.classList.toggle("selected", r === row));
    const d = row.dataset;

    const marker = markers.get(row);
    if (map && marker) {
      map.setView(marker.getLatLng(), 15);
      marker.openTooltip();
    }

    // 상세 채우기 (글자는 textContent 로 안전하게)
    $("hz-d-title").textContent = d.title || "-";
    $("hz-d-type").textContent = d.type || "-";
    $("hz-d-type").className = "hz-badge " + badgeClass(d.type);
    const active = d.status === "ACTIVE";
    $("hz-d-status").textContent = active ? "주의 요망" : "해소됨";
    $("hz-d-status").className = "hz-status " + (active ? "active" : "resolved");
    $("hz-d-author").textContent = d.author || "-";
    $("hz-d-date").textContent = d.date || "-";
    $("hz-d-coord").textContent = `${d.lat}, ${d.lng}`;
    $("hz-d-content").textContent = d.content || "";

    if (detail) detail.hidden = false;
    if (hint) hint.hidden = true;
    if (fromMarker) row.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }

  rows.forEach((row) => {
    row.addEventListener("click", (e) => {
      if (e.target.closest("form, button")) return;   // 버튼 누를 땐 선택 안 함
      select(row, false);
    });
  });

      /* ---------- 2-0) 목록 검색 (제목·내용·작성자 / 위험도 / 상태) ---------- */
  const fKeyword = document.getElementById("hz-f-keyword");
  const fType = document.getElementById("hz-f-type");
  const fStatus = document.getElementById("hz-f-status");
  const fSearch = document.getElementById("hz-f-search");
  const fReset = document.getElementById("hz-f-reset");
  const countEl = document.getElementById("hz-count");
  const noResult = document.getElementById("hz-no-result");

  /* ----- 페이지 나누기 (검색 결과를 10건씩 보여줌) ----- */
  const PAGE_SIZE = 10;
  const pagerEl = document.getElementById("hz-pager");
  let matched = rows.slice();      // 검색 조건에 맞는 행들
  let currentPage = 1;

  function renderPage(page) {
    const totalPages = Math.max(1, Math.ceil(matched.length / PAGE_SIZE));
    currentPage = Math.min(Math.max(page, 1), totalPages);
    const from = (currentPage - 1) * PAGE_SIZE;
    const visible = new Set(matched.slice(from, from + PAGE_SIZE));
    rows.forEach((row) => { row.hidden = !visible.has(row); });
    renderPager(totalPages);
  }

  function renderPager(totalPages) {
    if (!pagerEl) return;
    pagerEl.innerHTML = "";
    if (matched.length === 0) return;              // 1페이지뿐이면 숨김

    const GROUP = 10;                          // 한 번에 보이는 번호 개수
    const start = Math.floor((currentPage - 1) / GROUP) * GROUP + 1;
    const end = Math.min(start + GROUP - 1, totalPages);

    const add = (label, page, on) => {
      const a = document.createElement("a");
      a.href = "#";
      a.textContent = label;
      if (on) a.classList.add("on");
      a.addEventListener("click", (e) => { e.preventDefault(); renderPage(page); });
      pagerEl.appendChild(a);
    };

    add("«", 1, false);
    for (let p = start; p <= end; p++) add(String(p), p, p === currentPage);
    add("»", totalPages, false);
  }

  function showPageOf(row) {
    const idx = matched.indexOf(row);
    if (idx < 0) return;
    const page = Math.floor(idx / PAGE_SIZE) + 1;
    if (page !== currentPage) renderPage(page);
  }

  function applyFilter() {
    const kw = (fKeyword?.value || "").trim();
    const type = fType?.value || "";
    const status = fStatus?.value || "";

    matched = rows.filter((row) => {
      const d = row.dataset;
      const text = `${d.title} ${d.content} ${d.author}`;
      return (!kw || text.includes(kw))
          && (!type || d.type === type)
          && (!status || d.status === status);
    });
    const matchSet = new Set(matched);

    // 지도 마커: 조건에 맞는 건 페이지와 상관없이 전부 표시
    rows.forEach((row) => {
      const marker = markers.get(row);
      if (map && marker) {
        if (matchSet.has(row)) marker.addTo(map);
        else marker.remove();
      }
    });

    if (countEl) countEl.textContent = matched.length + "건";
    if (noResult) noResult.hidden = matched.length > 0 || rows.length === 0;

    renderPage(1);                             // 검색하면 1페이지부터
  }

  // [검색] 버튼 / 검색칸에서 Enter 를 누를 때만 거르기
  fSearch?.addEventListener("click", applyFilter);
  fKeyword?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      applyFilter();
    }
  });
  fReset?.addEventListener("click", () => {
    if (fKeyword) fKeyword.value = "";
    if (fType) fType.value = "";
    if (fStatus) fStatus.value = "";
    applyFilter();
  });

  renderPage(1);   // 처음 열었을 때도 10건씩만 보이게

  /* ---------- 2-1) 상세보기 팝업 (내용이 길어도 전부 보이게) ---------- */
  const viewModal = document.getElementById("hz-view-modal");

  function openView(row) {
    const d = row.dataset;
    $("hz-v-title").textContent = d.title || "-";
    $("hz-v-type").textContent = d.type || "-";
    $("hz-v-type").className = "hz-badge " + badgeClass(d.type);
    const active = d.status === "ACTIVE";
    $("hz-v-status").textContent = active ? "주의 요망" : "해소됨";
    $("hz-v-status").className = "hz-status " + (active ? "active" : "resolved");
    $("hz-v-author").textContent = d.author || "-";
    $("hz-v-date").textContent = d.date || "-";
    $("hz-v-coord").textContent = `${d.lat}, ${d.lng}`;
    $("hz-v-content").textContent = d.content || "";
    // 이미지 목록 연결
    currentImages = JSON.parse(row.dataset.images || "[]");
    currentImageIndex = 0;

    if (currentImages.length > 0) {
    showHazardImage(0);
    } else {
    viewImageGallery.hidden = true;
    }
    viewModal.hidden = false;
    }

  /* ---------- 상세보기 이미지 갤러리 ---------- */
  const viewImageGallery = $("hz-v-image-gallery");
  const viewImage = $("hz-v-image");
  const prevImageBtn = $("hz-image-prev");
  const nextImageBtn = $("hz-image-next");
  const imageCount = $("hz-image-count");

  let currentImages = [];
  let currentImageIndex = 0;

  function showHazardImage(index) {
    if (!currentImages.length) {
        viewImageGallery.hidden = true;
        return;
    }

  currentImageIndex = index;
  viewImage.src = currentImages[currentImageIndex];
  imageCount.textContent =
      `${currentImageIndex + 1} / ${currentImages.length}`;

  const hasMultipleImages = currentImages.length > 1;
  prevImageBtn.hidden = !hasMultipleImages;
  nextImageBtn.hidden = !hasMultipleImages;
  imageCount.hidden = !hasMultipleImages;
  viewImageGallery.hidden = false;
  }

  prevImageBtn?.addEventListener("click", () => {
    const index =
        (currentImageIndex - 1 + currentImages.length) % currentImages.length;
    showHazardImage(index);
  });

  nextImageBtn?.addEventListener("click", () => {
    const index =
        (currentImageIndex + 1) % currentImages.length;
    showHazardImage(index);
   });

  const closeView = () => { if (viewModal) viewModal.hidden = true; };

  document.querySelectorAll(".hz-view-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const row = btn.closest(".hz-row");
      select(row, false);      // 지도도 같이 이동
      openView(row);
    });
  });
  viewModal?.querySelectorAll("[data-hz-view-close]").forEach((b) => b.addEventListener("click", closeView));
    // closeOnBackdrop(viewModal, closeView); - 바깥 클릭 시 팝업 꺼짐
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && viewModal && !viewModal.hidden) closeView(); });

  

  
  
  /* ---------- 3) 신고 팝업 ---------- */
  const modal = document.getElementById("hz-modal");
  const openBtn = document.getElementById("hz-open-btn");
  const imageInput = document.getElementById("hz-images");
  const imagePreview = document.getElementById("hz-image-preview");

  let selectedHazardFiles = [];
  let previewUrls = [];

     function renderImagePreview() {
     if (!imagePreview) return;

  // 기존 미리보기 URL 정리
    previewUrls.forEach(url => URL.revokeObjectURL(url));
    previewUrls = [];

    imagePreview.innerHTML = "";

    if (selectedHazardFiles.length === 0) {
        imagePreview.innerHTML =
            '<span class="hz-image-empty">선택된 이미지가 없습니다.</span>';
        return;
    }

    // 선택된 파일 개수 표시
    const count = document.createElement("p");
    count.className = "hz-image-count";
    count.textContent = `선택된 이미지 ${selectedHazardFiles.length}장`;
    imagePreview.appendChild(count);

    selectedHazardFiles.forEach((file, index) => {
        const item = document.createElement("div");
        item.className = "hz-preview-item";

        const img = document.createElement("img");
        const url = URL.createObjectURL(file);
        previewUrls.push(url);

        img.src = url;
        img.alt = file.name;

        const name = document.createElement("span");
        name.textContent = file.name;

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.textContent = "×";
        removeBtn.className = "hz-preview-remove";
        removeBtn.setAttribute("aria-label", `${file.name} 삭제`);

        removeBtn.addEventListener("click", () => {
            selectedHazardFiles.splice(index, 1);
            updateImageInput();
        });

        item.append(img, name, removeBtn);
        imagePreview.appendChild(item);
       });
       }

       // 누적된 파일 목록을 실제 form input에 반영
     function updateImageInput() {
    const dataTransfer = new DataTransfer();

    selectedHazardFiles.forEach(file => {
        dataTransfer.items.add(file);
    });

    imageInput.files = dataTransfer.files;

    renderImagePreview();
   }

    // 파일을 새로 선택할 때 기존 파일에 추가
    imageInput?.addEventListener("change", () => {
    const newFiles = Array.from(imageInput.files);

    newFiles.forEach(file => {
        const alreadyExists = selectedHazardFiles.some(
            existing =>
                existing.name === file.name &&
                existing.size === file.size &&
                existing.lastModified === file.lastModified
        );

        if (!alreadyExists) {
            selectedHazardFiles.push(file);
        }
    });

    updateImageInput();
 }); // change 이벤트 종료
  const latInput = document.getElementById("hz-lat");
  const lngInput = document.getElementById("hz-lng");
  let pickMap = null;
  let pickMarker = null;

  function setPick(lat, lng) {
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);
    if (pickMarker) pickMarker.setLatLng([lat, lng]);
  }

  function openModal() {
    if (!modal) return;
    modal.hidden = false;

    // 팝업 안 작은 지도 (처음 열 때 한 번만 만들기)
    if (!pickMap) {
      const start = map ? map.getCenter() : L.latLng(DEFAULT_POS);
      pickMap = L.map("hz-pick-map").setView(start, 13);
      L.tileLayer(TILE, TILE_OPT).addTo(pickMap);
      pickMarker = L.marker(start).addTo(pickMap);
      setPick(start.lat, start.lng);
      pickMap.on("click", (e) => setPick(e.latlng.lat, e.latlng.lng));
    }
    // 숨겨져 있던 지도는 크기를 다시 계산해야 제대로 보임
    setTimeout(() => pickMap.invalidateSize(), 0);
    modal.querySelector("input[name='title']")?.focus();
  }

  function closeModal() {
    if (modal) modal.hidden = true;
  }

  // 위도·경도를 직접 고치면 작은 지도 마커도 이동
  [latInput, lngInput].forEach((input) => {
    input?.addEventListener("change", () => {
      const lat = parseFloat(latInput.value);
      const lng = parseFloat(lngInput.value);
      if (Number.isNaN(lat) || Number.isNaN(lng) || !pickMap) return;
      pickMarker.setLatLng([lat, lng]);
      pickMap.setView([lat, lng]);
    });
  });

  openBtn?.addEventListener("click", openModal);
  modal?.querySelectorAll("[data-hz-close]").forEach((b) => b.addEventListener("click", closeModal));
  // closeOnBackdrop(modal, closeModal);   // 바깥 클릭 시 팝업 꺼짐
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && modal && !modal.hidden) closeModal(); });

    /* ---------- 4) 주소 검색 → 위도·경도 자동 입력 ---------- */
  const addrInput = document.getElementById("hz-addr");
  const addrBtn = document.getElementById("hz-addr-btn");
  const addrMsg = document.getElementById("hz-addr-msg");

  // 주소 → 좌표 (OpenStreetMap Nominatim, 무료)
  async function geocode(address) {
    const url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=kr&q="
              + encodeURIComponent(address);
    const res = await fetch(url, { headers: { "Accept-Language": "ko" } });
    if (!res.ok) return null;
    const list = await res.json();
    return list.length ? { lat: parseFloat(list[0].lat), lng: parseFloat(list[0].lon) } : null;
  }

  // 못 찾으면 주소 끝부분(번지 등)을 하나씩 떼면서 다시 시도 → 동네라도 맞추기
  async function geocodeLoose(address) {
    let words = address.trim().split(/\s+/);
    while (words.length >= 2) {
      const pos = await geocode(words.join(" "));
      if (pos) return { pos, exact: words.join(" ") === address.trim() };
      words = words.slice(0, -1);
    }
    return null;
  }

  async function moveToAddress(roadAddr, jibunAddr) {
    addrMsg.textContent = "위치를 찾는 중이에요...";
    const found = (await geocodeLoose(roadAddr)) || (jibunAddr ? await geocodeLoose(jibunAddr) : null);

    if (!found) {
      addrMsg.textContent = "주소 위치를 찾지 못했어요. 지도에서 직접 위치를 눌러 주세요.";
      if (window.showPopup) showPopup("주소 위치를 찾지 못했어요.\n지도에서 직접 위치를 눌러 주세요.", "warn");
      return;
    }

    const { lat, lng } = found.pos;
    setPick(lat, lng);
    pickMap.setView([lat, lng], found.exact ? 17 : 15);
    addrMsg.textContent = found.exact
      ? "주소 위치로 맞췄어요. 필요하면 지도를 클릭해 미세 조정하세요."
      : "정확한 번지는 못 찾아서 근처로 맞췄어요. 지도를 클릭해 정확한 위치를 찍어 주세요.";
  }

  addrBtn?.addEventListener("click", () => {
    // 다음(카카오) 우편번호 검색 (script.html 에서 이미 불러옴)
    if (typeof daum === "undefined" || !daum.Postcode) {
      if (window.showPopup) showPopup("주소 검색을 불러오지 못했어요. 지도에서 직접 위치를 눌러 주세요.", "warn");
      return;
    }
    new daum.Postcode({
      oncomplete: (data) => {
        const road = data.roadAddress || data.address;
        addrInput.value = road;
        moveToAddress(road, data.jibunAddress || data.autoJibunAddress);
      },
    }).open();
  });

});
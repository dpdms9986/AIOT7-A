// 위험 구역 마커 (대시보드 지도 공용)
// - GET /api/hazards 로 주의 요망(ACTIVE) 위험 구역을 받아서 지도에 ⚠ 마커로 표시
// - 사용법: addHazardMarkers(map)
(function () {
  const COLORS = { "고위험": "#d93025", "중위험": "#f29900", "저위험": "#1e8e3e" };

  // 팝업 글자는 textContent 로 (안전)
  const popupOf = (h) => {
    const box = document.createElement("div");
    const title = document.createElement("b");
    title.textContent = `⚠ ${h.title}`;
    box.append(title);

    const content = (h.content || "").length > 80 ? h.content.slice(0, 80) + "…" : (h.content || "");
    [`${h.hazardType} · 신고자 ${h.authorName}`, content].forEach((line) => {
      if (!line) return;
      box.append(document.createElement("br"), document.createTextNode(line));
    });
    return box;
  };

  window.addHazardMarkers = async function (map) {
    if (!map || typeof L === "undefined") return null;

    // 농지 마커와 따로 관리해서, 농지 마커를 다시 그려도 지워지지 않게 함
    const layer = L.layerGroup().addTo(map);

    try {
      const res = await fetch("/api/hazards");
      if (!res.ok) return layer;
      const list = await res.json();

      list.forEach((h) => {
        if (h.latitude == null || h.longitude == null) return;
        const color = COLORS[h.hazardType] || COLORS["중위험"];
        const icon = L.divIcon({
          className: "",
          html: `<div class="hazard-icon" style="background:${color}">⚠</div>`,
          iconSize: [26, 26],
          iconAnchor: [13, 13],
        });
        L.marker([h.latitude, h.longitude], { icon, title: h.title, zIndexOffset: 500 })
          .bindPopup(popupOf(h), { maxWidth: 240 })
          .addTo(layer);
      });
    } catch (e) {
      console.error("위험 구역 불러오기 실패", e);
    }
    return layer;
  };
})();
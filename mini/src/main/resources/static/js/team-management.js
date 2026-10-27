(() => {
    const tbody = document.getElementById("team-member-tbody");
    if (!tbody) return;

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) =>
        Array.from(root.querySelectorAll(selector));

    // 백엔드 API 엔드 포인트 설정
    const TEAM_API = "api/team/members";
    let members = [];

    const statusInfo = {
        online: { label: "접속 중", className: "online" },
        working: { label: "작업 중", className: "working" },
        offline: { label: "오프라인", className: "offline" }
    };

    const skillClass = {
        "초급": "beginner",
        "중급": "intermediate",
        "베테랑": "veteran"
    };

    const searchForm = $("#team-search-form");
    const keywordInput = $("#team-keyword");
    const statusFilter = $("#team-status-filter");
    const skillFilter = $("#team-skill-filter");
    const emptyMessage = $("#team-empty");

    const profileModal = $("#team-profile-modal");
    const detailModal = $("#team-detail-modal");
    const reassignModal = $("#team-reassign-modal");

    let selectedMemberId = null;

    function escapeHTML(value) {
        return String(value ?? "").replace(/[&<>"']/g, char => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[char]);
    }

    function getMember(id) {
        return members.find(member => member.id === id);
    }

    function renderSummary() {
        $("#team-total-count").textContent = `${members.length}명`;

        $("#team-online-count").textContent =
            `${members.filter(member => member.status === "online").length}명`;

        $("#team-working-count").textContent =
            `${members.filter(member => member.status === "working").length}명`;

        $("#team-completed-count").textContent =
            `${members.reduce((sum, member) => sum + member.completed, 0)}건`;
    }

    function renderMembers() {
        const keyword = keywordInput.value.trim().toLocaleLowerCase("ko-KR");
        const status = statusFilter.value;
        const skill = skillFilter.value;

        const visibleMembers = members.filter(member => {
            const matchesKeyword =
                `${member.name} ${member.phone} ${member.drone}`
                    .toLocaleLowerCase("ko-KR")
                    .includes(keyword);

            return matchesKeyword &&
                (!status || member.status === status) &&
                (!skill || member.skill === skill);
        });

        tbody.innerHTML = visibleMembers.map(member => {
            const statusData = statusInfo[member.status];

            return `
                <tr>
                    <td>
                        <div class="team-member-cell">
                            <span class="team-avatar">
                                ${escapeHTML(member.name.slice(0, 1))}
                            </span>
                            <div>
                                <div class="team-member-name">
                                    ${escapeHTML(member.name)}
                                </div>
                                <div class="team-member-meta">
                                    ${escapeHTML(member.id)}
                                </div>
                            </div>
                        </div>
                    </td>
                    <td>${escapeHTML(member.phone)}</td>
                    <td>
                        <span class="team-status ${statusData.className}">
                            ${statusData.label}
                        </span>
                    </td>
                    <td>${escapeHTML(member.drone || "미배정")}</td>
                    <td>${member.tank ? `${member.tank} L` : "미등록"}</td>
                    <td>${escapeHTML(member.currentTask)}</td>
                    <td>${member.completed}건</td>
                    <td>
                        <span class="team-skill ${skillClass[member.skill]}">
                            ${escapeHTML(member.skill)}
                        </span>
                    </td>
                    <td>
                        <div class="team-row-actions">
                            <button type="button" class="team-row-btn"
                                    data-action="detail"
                                    data-id="${escapeHTML(member.id)}">
                                상세보기
                            </button>
                            <button type="button"
                                    class="team-row-btn reassign"
                                    data-action="reassign"
                                    data-id="${escapeHTML(member.id)}">
                                재배정
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");

        $("#team-visible-count").textContent = visibleMembers.length;
        emptyMessage.hidden = visibleMembers.length > 0;

        renderAllocation();
        renderSummary();
    }

    function renderAllocation() {
        const allocationBody = $("#team-allocation-tbody");

        allocationBody.innerHTML = members.map(member => `
            <tr>
                <td>
                    <div class="team-member-name">
                        ${escapeHTML(member.name)}
                    </div>
                    <div class="team-member-meta">
                        ${escapeHTML(member.id)}
                    </div>
                </td>
                <td>
                    <select data-allocation-skill="${escapeHTML(member.id)}"
                            aria-label="${escapeHTML(member.name)} 숙련도">
                        ${["초급", "중급", "베테랑"].map(skill => `
                            <option value="${skill}"
                                ${member.skill === skill ? "selected" : ""}>
                                ${skill}
                            </option>
                        `).join("")}
                    </select>
                </td>
                <td>
                    <input type="number" min="0.1" max="10" step="0.1"
                           value="${member.weight}"
                           data-allocation-weight="${escapeHTML(member.id)}"
                           aria-label="${escapeHTML(member.name)} 할당 가중치">
                </td>
                <td>${member.completed}건</td>
                <td>
                    <button type="button" class="team-row-btn"
                            data-action="save-allocation"
                            data-id="${escapeHTML(member.id)}">
                        설정 저장
                    </button>
                </td>
            </tr>
        `).join("");
    }

    function openModal(modal) {
        modal.hidden = false;
        document.body.style.overflow = "hidden";

        const focusTarget = $("input:not([type='hidden']), select, button", modal);
        focusTarget?.focus();
    }

    function closeModal(modal) {
        modal.hidden = true;

        if ($$(".team-modal-backdrop:not([hidden])").length === 0) {
            document.body.style.overflow = "";
        }
    }

    function showDetail(member) {
        const statusData = statusInfo[member.status];

        $("#team-detail-content").innerHTML = `
            <div class="team-detail-grid">
                <div class="team-detail-item">
                    <span>조종사 이름</span>
                    <strong>${escapeHTML(member.name)}</strong>
                </div>
                <div class="team-detail-item">
                    <span>프로필 ID</span>
                    <strong>${escapeHTML(member.id)}</strong>
                </div>
                <div class="team-detail-item">
                    <span>연락처</span>
                    <strong>${escapeHTML(member.phone)}</strong>
                </div>
                <div class="team-detail-item">
                    <span>접속 상태</span>
                    <strong>${statusData.label}</strong>
                </div>
                <div class="team-detail-item">
                    <span>담당 기체</span>
                    <strong>${escapeHTML(member.drone || "미배정")}</strong>
                </div>
                <div class="team-detail-item">
                    <span>약제 탱크 용량</span>
                    <strong>${member.tank ? `${member.tank} L` : "미등록"}</strong>
                </div>
                <div class="team-detail-item">
                    <span>숙련도</span>
                    <strong>${escapeHTML(member.skill)}</strong>
                </div>
                <div class="team-detail-item">
                    <span>기본 할당 가중치</span>
                    <strong>${member.weight}</strong>
                </div>
                <div class="team-detail-item">
                    <span>현재 작업</span>
                    <strong>${escapeHTML(member.currentTask)}</strong>
                </div>
                <div class="team-detail-item">
                    <span>금일 완료 작업</span>
                    <strong>${member.completed}건</strong>
                </div>
            </div>
        `;

        openModal(detailModal);
    }

    function openReassign(member) {
        if (member.status !== "offline") {
            const confirmed = confirm(
                `${member.name} 조종사의 미완료 농지를 재배정하시겠습니까?`
            );

            if (!confirmed) return;
        }

        const availableTargets = members.filter(candidate =>
            candidate.id !== member.id &&
            candidate.status === "online"
        );

        if (availableTargets.length === 0) {
            alert("재배정 가능한 접속 중 조종사가 없습니다.");
            return;
        }

        const form = $("#team-reassign-form");
        form.elements.sourceId.value = member.id;
        form.elements.sourceName.value = member.name;

        $("#team-reassign-target").innerHTML = availableTargets
            .map(candidate => `
                <option value="${escapeHTML(candidate.id)}">
                    ${escapeHTML(candidate.name)} · ${escapeHTML(candidate.id)}
                </option>
            `).join("");

        openModal(reassignModal);
    }

    // 탭 전환
    $$(".team-tab").forEach(button => {
        button.addEventListener("click", () => {
            const tab = button.dataset.teamTab;

            $$(".team-tab").forEach(item => {
                const active = item === button;
                item.classList.toggle("active", active);
                item.setAttribute("aria-pressed", String(active));
            });

            $("#team-members-panel").hidden = tab !== "members";
            $("#team-allocation-panel").hidden = tab !== "allocation";
        });
    });

    // 검색 및 초기화
    searchForm.addEventListener("submit", event => {
        event.preventDefault();
        renderMembers();
    });

    searchForm.addEventListener("reset", () => {
        setTimeout(renderMembers, 0);
    });

    keywordInput.addEventListener("input", renderMembers);
    statusFilter.addEventListener("change", renderMembers);
    skillFilter.addEventListener("change", renderMembers);

    // 프로필 추가
    $("#add-pilot-btn").addEventListener("click", () => {
        $("#team-profile-form").reset();
        openModal(profileModal);
    });

    $("#team-profile-form").addEventListener("submit", event => {
        event.preventDefault();

        const form = event.currentTarget;
        const values = new FormData(form);

        const name = String(values.get("name") || "").trim();
        const phone = String(values.get("phone") || "").trim();

        if (!name || !phone) {
            alert("이름과 연락처를 입력해 주세요.");
            return;
        }

        if (members.some(member => member.phone === phone)) {
            alert("이미 등록된 연락처입니다.");
            return;
        }

        const weight = Number(values.get("weight"));

        if (!Number.isFinite(weight) || weight < 0.1 || weight > 10) {
            alert("가중치는 0.1 이상 10 이하로 입력해 주세요.");
            return;
        }

        members.push({
            id: `PILOT-${String(Date.now()).slice(-6)}`,
            name,
            phone,
            drone: String(values.get("drone") || "").trim(),
            tank: Number(values.get("tank")) || 0,
            status: String(values.get("status")),
            currentTask: "-",
            completed: 0,
            skill: String(values.get("skill")),
            weight
        });

        closeModal(profileModal);
        renderMembers();

    });

    // 목록 버튼: 상세보기 / 재배정 / 가중치 저장
    document.addEventListener("click", event => {
        const button = event.target.closest("[data-action]");
        if (!button) return;

        const member = getMember(button.dataset.id);
        if (!member) return;

        switch (button.dataset.action) {
            case "detail":
                showDetail(member);
                break;

            case "reassign":
                openReassign(member);
                break;

            case "save-allocation": {
                const skillInput = $(
                    `[data-allocation-skill="${member.id}"]`
                );
                const weightInput = $(
                    `[data-allocation-weight="${member.id}"]`
                );

                const weight = Number(weightInput.value);

                if (!Number.isFinite(weight) || weight < 0.1 || weight > 10) {
                    alert("가중치는 0.1 이상 10 이하로 입력해 주세요.");
                    return;
                }

                member.skill = skillInput.value;
                member.weight = weight;

                renderMembers();
                break;
            }
        }
    });

        // 재배정 (백엔드 API 연결 후 구현)
    $("#team-reassign-form").addEventListener("submit", event => {
        event.preventDefault();

        const form = event.currentTarget;
        const source = getMember(form.elements.sourceId.value);
        const target = getMember(form.elements.targetId.value);

        if (!source || !target) {
            alert("재배정 대상을 확인해 주세요.");
            return;
        }

        if (target.status !== "online") {
            alert("현재 접속 중인 조종사만 재배정 대상으로 선택할 수 있습니다.");
            return;
        }

        // TODO: 재배정 API 호출 (source, target, land)
        alert("재배정 기능은 서버 연결 후 사용할 수 있습니다.");
    });

    // 초대 코드 복사
    $("#invite-pilot-btn").addEventListener("click", async () => {
        const code = window.currentUser?.inviteCode;

        if (!code) {
            alert("초대 코드를 불러올 수 없습니다.");
            return;
        }

        try {
            await navigator.clipboard.writeText(code);
            alert(`초대 코드 ${code}가 복사되었습니다.`);
        } catch (error) {
            alert(`초대 코드: ${code}`);
        }
    });

    // 모달 닫기
    $$("[data-close-modal]").forEach(button => {
        button.addEventListener("click", () => {
            const modal = button.closest(".team-modal-backdrop");
            if (modal) closeModal(modal);
        });
    });

    $$(".team-modal-backdrop").forEach(modal => {
        modal.addEventListener("click", event => {
            if (event.target === modal) closeModal(modal);
        });
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            $$(".team-modal-backdrop:not([hidden])")
                .forEach(closeModal);
        }
    });

    // 서버에서 팀원 목록을 불러오는 비동기 초기화 함수
    async function initTeamData() {
        try {
            const res = await fetch(TEAM_API);
            if (!res.ok) throw new Error(`서버 응답 오류: ${res.status}`);
            members = await res.json();
        } catch (error) {
            console.error("팀원 목록 조회 실패:", error);
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding:20px; color:#666;">팀원 데이터를 불러오지 못했습니다.</td></tr>`;
            return;
        }
        renderMembers();
    }

    initTeamData();
})();
})();
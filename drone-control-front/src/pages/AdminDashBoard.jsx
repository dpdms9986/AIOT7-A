import { Link } from 'react-router-dom'
import {
  Leaf, MapPin, Tractor, FileText, Sprout, ShieldCheck, TrendingUp, ArrowRight,
} from 'lucide-react'

// 아래 기능 카드 4개
const FEATURES = [
  { icon: <MapPin size={22} />, title: '농장 현황 조회', desc: '지도에서 농장의 위치와 상태를 확인할 수 있습니다.' },
  { icon: <Leaf size={22} />, title: '작업 관리', desc: '작업 일정과 진행 상황을 간편하게 관리하세요.' },
  { icon: <Tractor size={22} />, title: '장비 관리', desc: '농기계와 장비의 상태를 실시간으로 확인합니다.' },
  { icon: <FileText size={22} />, title: '데이터 분석', desc: '수집된 데이터를 바탕으로 효율적인 농업을 지원합니다.' },
]

// 제목 아래 작은 아이콘 3개
const POINTS = [
  { icon: <Sprout size={18} />, title: '실시간 모니터링', desc: '농장 상태를 한눈에 확인' },
  { icon: <ShieldCheck size={18} />, title: '안전한 관리', desc: '사고 예방 및 작업 중단 알림' },
  { icon: <TrendingUp size={18} />, title: '데이터 기반 분석', desc: '더 나은 농업을 위한 인사이트' },
]

function AdminDashboard() {
  return (
    <div className="home">
      {/* 맨 위 줄 */}
      <header className="home-header">
        <Link to="/" className="home-logo">
          <Leaf size={26} />
          <span>스마트 농업 관제 시스템</span>
        </Link>

        <nav className="home-nav">
          <a href="#intro">시스템 소개</a>
          <a href="#features">주요 기능</a>
          <a href="#start">이용 안내</a>
          <a href="#start">고객센터</a>
        </nav>

        <div className="home-auth">
          <Link to="/signup" className="home-btn home-btn-outline">회원가입</Link>
          <Link to="/login" className="home-btn home-btn-primary">로그인</Link>
        </div>
      </header>

      {/* 큰 제목 영역: 왼쪽 글, 오른쪽 지도 그림 */}
      <section className="home-hero" id="intro">
        <div className="home-hero-text">
          <h1>
            더 스마트한 농업을 위한
            <br />
            <span>실시간 관제 시스템</span>
          </h1>
          <p>
            농업 현장의 데이터를 실시간으로 확인하고
            <br />
            효율적인 관리를 지원합니다.
          </p>

          <ul className="home-points">
            {POINTS.map((p) => (
              <li key={p.title}>
                <span className="home-point-icon">{p.icon}</span>
                <div>
                  <strong>{p.title}</strong>
                  <small>{p.desc}</small>
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* 실제 지도가 아니라 CSS로 만든 간단한 그림 */}
        <div className="home-hero-map">
          <div className="home-status-card">
            <Leaf size={18} />
            <div>
              <small>농장 전체 상태</small>
              <strong>정상</strong>
            </div>
          </div>
          <div className="home-map-board">
            <MapPin className="home-pin pin-1" size={30} color="#f2b705" fill="#f2b705" />
            <MapPin className="home-pin pin-2" size={30} color="#238451" fill="#238451" />
            <MapPin className="home-pin pin-3" size={30} color="#2f80ed" fill="#2f80ed" />
            <MapPin className="home-pin pin-4" size={30} color="#8a8f8c" fill="#8a8f8c" />
          </div>
        </div>
      </section>

      {/* 아래 영역: 기능 카드 4개 + 시작하기 카드 */}
      <section className="home-bottom">
        <div className="home-features" id="features">
          {FEATURES.map((f) => (
            <article key={f.title} className="home-feature-card">
              <div className="home-feature-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
              <ArrowRight size={16} />
            </article>
          ))}
        </div>

        <aside className="home-start" id="start">
          <h3>
            <Leaf size={16} /> 지금 바로 시작해보세요!
          </h3>
          <p>
            로그인 후 더 다양한 기능을 이용할 수 있습니다.
            <br />
            회원가입을 통해 간편하게 시작하세요.
          </p>
          <Link to="/login" className="home-btn home-btn-primary home-btn-block">
            로그인하기 <ArrowRight size={14} />
          </Link>
          <Link to="/signup" className="home-btn home-btn-outline home-btn-block">
            회원가입하기
          </Link>
        </aside>
      </section>
    </div>
  )
}

export default AdminDashboard
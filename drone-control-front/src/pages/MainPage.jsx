import "../styles/main-page.css";
import mainPageWireframe from "../assets/main-page-wireframe.png";

export default function MainPage() {
    return (
        <main className="main-page">
            <img
                src={mainPageWireframe}
                alt="메인 페이지 와이어프레임"
                className="main-page-wireframe"
            />
        </main>
    );
}
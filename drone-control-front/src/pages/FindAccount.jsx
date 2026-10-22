import { useState } from "react";

import "../styles/findaccount.css";

export default function FindAccount() {
  const [activeTab, setActiveTab] = useState("id");

  const handleSubmit = (event) => {
    event.preventDefault();

    alert(
      activeTab === "id"
        ? "아이디 찾기 기능은 추후 연결할 예정입니다."
        : "비밀번호 찾기 기능은 추후 연결할 예정입니다."
    );
  };


    return (
    <main className="findaccount-page">
      <section className="findaccount-card">
      

       {/* 아이디 찾기 / 비밀번호 찾기 탭 */}
       <div className="findaccount-tabs">
          <button
            type="button"
            className={activeTab === "id" ? "active" : ""}
            onClick={() => setActiveTab("id")}
          >
            아이디 찾기
          </button>

          <button
            type="button"
            className={activeTab === "password" ? "active" : ""}
            onClick={() => setActiveTab("password")}
          >
            비밀번호 찾기
          </button>
        </div>
        
         <form className="findaccount-form" onSubmit={handleSubmit}>
         {activeTab === "id" ? (
            <>
            <div className="findaccount-field">
            <label htmlFor="find-name">이름</label>
            <input
              id="find-name"
              type="text"
              name="name"
              placeholder="이름"
              required/>
               </div>
         
            <div className="findaccount-field">
            <label htmlFor="find-email">이메일</label>
            <input
              id="find-email"
              type="email"
              name="email"
              placeholder="이메일"
              required/>
               </div>
         
            <button className="findaccount-submit" type="submit">
                아이디 찾기
              </button>

            </>
         ) : (
             <>
             
            <div className="findaccount-field">
            <label htmlFor="find-userid">아이디</label>
            <input
              id="find-userid"
              type="text"
              name="userid"
              placeholder="아이디"
              required/>
               </div>
             
             <div className="findaccount-field">
            <label htmlFor="find-email">이메일</label>
            <input
              id="password-email"
              type="email"
              name="email"
              placeholder="이메일"
              required/>
               </div>
             
             <button className="findaccount-submit" type="submit">
                비밀번호 찾기
              </button>
             </>
             )}
           </form>
        
    </section>
    </main>

    )


}
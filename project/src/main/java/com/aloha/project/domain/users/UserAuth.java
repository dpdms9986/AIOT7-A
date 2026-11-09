package com.aloha.project.domain.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity 
@Table(name = "user_auth" )
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@ToString(exclude = "user")
public class UserAuth {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long no;
  
  // Users : UserAuth = 1 : N
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_no", nullable = false)
  private Users user;

  @Column(nullable = false, length = 100)
   // ⭐ 회원 권한 : ROLE_PILOT, ROLE_ADMIN, ROLE_LEADER 등
  private String auth;        
  
//   // UserAuth.java 안에 추가하면 편리한 생성자 (선택 사항)
// public UserAuth(String auth) {
//     this.auth = auth;
// }
// // 회원가입 처리 시
// user.addAuth(new UserAuth("ROLE_" + signupDto.getRole()));
  
}

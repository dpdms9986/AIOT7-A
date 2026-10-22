package com.aloha.mini.domain;

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
  private String auth;            // ⭐ 회원 권한 : ROLE_PILOT, ROLE_ADMIN 등
  
}

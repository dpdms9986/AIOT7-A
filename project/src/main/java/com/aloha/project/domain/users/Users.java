package com.aloha.project.domain.users;

import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.aloha.project.domain.Base;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "users")
public class Users extends Base {

  private String username; // 아이디  
  private String password; // 비밀번호
  @Transient private String newPassword;        // 새 비밀번호
  @Transient private String confirmPassword;    // 새 비밀번호 확인


  private String name;     // 이름
  private String tel;      // 전화번호
  private String email;    // 이메일
  private Boolean enabled; // 계정 활성화 여부

  // 권한 목록 (user_auth.user_no -> users.no, 조회 전용)
  @ToString.Exclude
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "userNo", referencedColumnName = "no", insertable = false, updatable = false)
  @Fetch(FetchMode.SUBSELECT)
  List<UserAuth> authList;
  
}

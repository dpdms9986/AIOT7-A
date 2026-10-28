package com.aloha.project.domain.users;

import org.hibernate.annotations.DynamicInsert;

import com.aloha.project.domain.Base;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "user_auth")
public class UserAuth extends Base {

  private Long userNo;
  private String username;
  private String auth; 
  private String name; 

  
}

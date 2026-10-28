package com.aloha.project.domain.common;

import org.hibernate.annotations.DynamicInsert;

import com.aloha.project.domain.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "board")
public class Board extends Base {

  private String type;
  private String title;
  @Column(columnDefinition = "TEXT")
  private String content;
  private Integer views;
  private Integer seq;
  
}

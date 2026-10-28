package com.aloha.project.domain.common;

import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;


@Data
public class Files {
	
	private int fileNo;
	private String fileCode;
	private String fileName;
	private String fullName;
	private long fileSize;
	private int sortSeq;
	private Date regDate;
	private Date updDate;
	
	private String parentTable;
	private int parentNo;
	
	
	private MultipartFile[] file;	// 파일정보
	
	
	private String subPath;
	
}

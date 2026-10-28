package com.aloha.project.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.common.Files;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileUtils {

	/**
	 * 파일 업로드 (다중)
	 * 
	 * @param files
	 * @param uploadPath
	 * @return
	 * @throws Exception
	 */
	public List<Files> uploadFiles(MultipartFile[] files, String uploadPath) throws Exception {

		if (files == null)
			return null;

		ArrayList<Files> fileList = new ArrayList<Files>();

		// 업로드 경로에 파일 복사
		for (MultipartFile file : files) {
			// 파일 존재여부 확인
			if (file.isEmpty()) {
				continue;
			}

			long fileSize = file.getSize();

			// 파일명 중복 방지를 위한 고유 ID 생성
			UUID uid = UUID.randomUUID();

			// 실제 원본 파일 이름
			String originalFileName = file.getOriginalFilename();

			// UID_XXX.png
			String uploadFileName = uid.toString() + "_" + originalFileName;

			// 업로드 폴더에 업로드할 파일 복사(upload)
			byte[] fileData = file.getBytes();

			// 파일객체 : ~/upload/UID_XXX.png
			File target = new File(uploadPath, uploadFileName);

			// fileData : 요청된 파일
			// target : 업로드할 파일 객체
			// 파일 복사
			FileCopyUtils.copy(fileData, target);

			// 확장자
			// String formatName = uploadFileName.substring(uploadFileName.lastIndexOf(".") + 1).toLowerCase();
			// MediaType mType = MediaUtils.getMediaType(formatName);

			// 업로드된 파일 전체 경로 (경로+이름)
			String uploadedPath = uploadPath + "/" + uploadFileName;

			Files f = new Files();
			f.setFullName(uploadedPath);
			f.setFileName(originalFileName);
			f.setFileSize(fileSize);
			fileList.add(f);

			log.info("업로드 : " + uploadedPath);
		}
		log.info("업로드가 완료되었습니다");
		return fileList;
	}

	/**
	 * 파일 업로드 (단일)
	 * 
	 * @param files
	 * @param uploadPath
	 * @return
	 * @throws Exception
	 */
	public Files uploadFile(MultipartFile file, String uploadPath) throws Exception {

		if (file == null)
			return null;

		if (file.isEmpty()) {
			return null;
		}

		long fileSize = file.getSize();

		// 파일명 중복 방지를 위한 고유 ID 생성
		// UUID uid = UUID.randomUUID();

		// 실제 원본 파일 이름
		String originalFileName = file.getOriginalFilename();

		// UID_XXX.png
		String uploadFileName = /* uid.toString() + "_" + */ originalFileName;

		// 업로드 폴더에 업로드할 파일 복사(upload)
		byte[] fileData = file.getBytes();

		// 파일객체 : ~/upload/UID_XXX.png
		File target = new File(uploadPath, uploadFileName);

		// fileData : 요청된 파일
		// target : 업로드할 파일 객체
		// 파일 복사
		FileCopyUtils.copy(fileData, target);

		// 확장자
		// String formatName = uploadFileName.substring(uploadFileName.lastIndexOf(".") + 1);
		// MediaType mType = MediaUtils.getMediaType(formatName);

		// 업로드된 파일 전체 경로 (경로+이름)
		String uploadedPath = uploadPath + "/" + uploadFileName;

		Files f = new Files();
		f.setFullName(uploadedPath);
		f.setFileName(uploadFileName);
		f.setFileSize(fileSize);

		return f;
	}

	/**
	 * 파일 삭제
	 * 
	 * @param fullName
	 * @throws Exception
	 */
	public void deleteFile(String fullName) throws Exception {

		File deleteFile = new File(fullName);

		// 실제로 파일이 존재하는지 확인
		if (deleteFile.exists()) {
			// 파일 삭제
			if (deleteFile.delete()) {
				log.info("삭제한 파일 : " + fullName);
				log.info("파일 삭제 성공!");
			} else {
				log.info("파일삭제 실패!");
			}
		} else {
			log.info("삭제(실패) : " + fullName);
			log.info("파일이 존재하지 않습니다.");
		}

	}

	/**
	 * 실제 파일 삭제 - 해당 게시글 전체파일 삭제
	 * 
	 * @param fileList
	 * @throws Exception
	 */
	public void deleteFiles(List<Files> fileList) throws Exception {

		// 해당 게시글의 첨부파일 전체 삭제
		for (Files file : fileList) {
			String fullName = file.getFullName();

			File deleteFile = new File(fullName);

			// 실제로 파일이 존재하는지 확인
			if (deleteFile.exists()) {
				// 파일 삭제
				if (deleteFile.delete()) {
					log.info("삭제한 파일 : " + fullName);
					log.info("파일 삭제 성공!");
				} else {
					log.info("파일삭제 실패!");
				}
			} else {
				log.info("삭제(실패) : " + fullName);
				log.info("파일이 존재하지 않습니다.");
			}

		}

	}

	/**
	 * 타겟 폴더와 하위파일 모두 삭제
	 * 
	 * @param targetFolder
	 * @return
	 */
	public boolean deleteDirectoryAndFiles(File targetFolder) {
		if (!targetFolder.exists()) {
			log.info("{} >>> 경로가 존재하지 않습니다.", targetFolder);
			return false;
		}

		File[] files = targetFolder.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				log.info("{} >>> 파일은 디렉토리입니다. 하위 파일을 확인하겠습니다.", file);
				deleteDirectoryAndFiles(file);
			}
			file.delete();
			log.info("{} >>> 파일이 삭제되었습니다.", file);
		}

		return targetFolder.delete();
	}


	// 폴더 만들기
	public void makeFolder(String path) {

		File folder = new File(path);
		// 해당 디렉토리가 없을경우 디렉토리를 생성합니다.
		if (!folder.exists()) {
			try {
				folder.mkdirs(); // 폴더 생성합니다.
				System.out.println("폴더가 생성되었습니다.");
			} catch (Exception e) {
				e.getStackTrace();
			}
		} else {
			System.out.println("이미 폴더가 생성되어 있습니다.");
		}
	}

}

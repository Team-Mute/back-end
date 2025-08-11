package Team_Mute.back_end.domain.space_admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3Uploader {

	@Value("${cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${cloud.aws.credentials.secret-key}")
	private String secretKey;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.region.static}")
	private String region;

	public String upload(MultipartFile file, String dirName) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("업로드할 파일이 없습니다.");
		}

		// 파일명 생성: dir/yyyyMMddHHmmss_originalName
		String originalName = file.getOriginalFilename();
		String timestamp = java.time.LocalDateTime.now()
			.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String key = dirName + "/" + timestamp + "_" + originalName;

		// Content-Type 보정
		String contentType = file.getContentType();
		if (contentType == null || contentType.isBlank()) {
			contentType = "application/octet-stream";
		}

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(contentType)
			.build();

		// 대용량 안전: fromInputStream 사용
		getS3Client().putObject(
			putObjectRequest,
			software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
				file.getInputStream(), file.getSize()
			)
		);

		// URL 생성: 디렉터리(/)는 그대로, "파일명"만 인코딩
		String encodedFileName = java.net.URLEncoder
			.encode(originalName, java.nio.charset.StandardCharsets.UTF_8)
			.replace("+", "%20"); // 공백 보정
		String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/"
			+ dirName + "/" + timestamp + "_" + encodedFileName;

		return url;
	}

	public List<String> uploadAll(List<MultipartFile> files, String dirName) {
		List<String> urls = new ArrayList<>(files.size());
		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) continue;
			try {
				urls.add(upload(file, dirName));
			} catch (IOException e) {
				throw new RuntimeException("파일 업로드 실패: " + file.getOriginalFilename(), e);
			}
		}
		return urls;
	}

	private String createFileName(String originalName, String dirName) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		return dirName + "/" + timestamp + "_" + originalName;
	}

	private S3Client getS3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			))
			.build();
	}

	// 버킷에서 이미지 삭제
}

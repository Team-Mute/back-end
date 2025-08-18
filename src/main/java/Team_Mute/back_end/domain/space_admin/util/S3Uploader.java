package Team_Mute.back_end.domain.space_admin.util;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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

	/* 복제한 데이터 업로드 */
	public String copyByUrl(String sourceUrl, String targetDir) {
		if (sourceUrl == null || sourceUrl.isBlank()) {
			throw new IllegalArgumentException("S3 sourceUrl이 비어 있습니다.");
		}
		String sourceKey = extractKeyFromUrl(sourceUrl);
		String newKey = buildNewKey(targetDir, sourceKey.substring(sourceKey.lastIndexOf('/') + 1));

		try (S3Client s3 = getS3Client()) {
			CopyObjectRequest req = CopyObjectRequest.builder()
				.copySource(bucket + "/" + urlEncode(sourceKey))
				.destinationBucket(bucket)
				.destinationKey(newKey)
				//.acl(ObjectCannedACL.PUBLIC_READ) // 공개 URL 사용 중이면 그대로 유지
				.build();

			s3.copyObject(req);
			return buildPublicUrl(newKey);
		}
	}

	// S3 퍼블릭 URL 생성(CloudFront를 쓰지 않는 기본 케이스)
	private String buildPublicUrl(String key) {
		// ex) https://{bucket}.s3.{region}.amazonaws.com/{key}
		String encKey = urlEncode(key);
		return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encKey;
	}

	private String urlEncode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private String buildNewKey(String dirName, String originalName) {
		String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		return dirName + "/" + ts + "_" + originalName;
	}

	private String extractKeyFromUrl(String url) {
		// S3 또는 CloudFront URL 모두에서 key만 추출
		try {
			URI u = URI.create(url);
			String path = u.getPath();               // "/folder/file.png"
			String key = path.startsWith("/") ? path.substring(1) : path;
			// CloudFront 도메인으로 쓰는 경우에도 path가 곧 key이므로 그대로 사용
			// s3 website endpoint 등 변형이 있어도 path기반으로 동작
			return URLDecoder.decode(key, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalArgumentException("S3 URL에서 Key 추출 실패: " + url, e);
		}
	}
}

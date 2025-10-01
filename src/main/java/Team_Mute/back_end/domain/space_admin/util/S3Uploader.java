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
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3Uploader
 * - AWS S3 연동 유틸리티
 * - 업로드 동작을 단순화하고 예외/로깅 처리를 일관되게 함
 * - 외부에서 재사용하기 쉬운 순수 기능 메서드 제공
 */
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

	@Value("${cloud.aws.cloudfront.domain}")
	private String cloudfrontDomain;

	/**
	 * 파일을 S3에 업로드
	 */
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

		// URL 생성
		return "https://" + cloudfrontDomain + "/" + urlEncode(key);
	}

	/**
	 * 여러 파일을 S3에 업로드
	 */
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

	/**
	 * AWS S3 클라이언트를 생성하여 반환
	 * 매번 호출 시 새로운 S3Client 인스턴스를 생성하므로, 멀티스레드 환경에서도 안전하게 사용 가능
	 * 접근 키, 시크릿 키, 리전(region) 정보를 기반으로 초기화
	 *
	 * @return 새로 생성된 S3Client 인스턴스
	 */
	private S3Client getS3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			))
			.build();
	}

	/**
	 * 기존 S3 객체를 새로운 키로 복사
	 **/
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

	/**
	 * CloudFront 기반 퍼블릭 URL 생성
	 **/
	private String buildPublicUrl(String key) {
		String encKey = urlEncode(key);

		// CloudFront URL 사용
		return "https://" + cloudfrontDomain + "/" + encKey;
	}

	/**
	 * S3 객체 키(Key)를 URL-safe 문자열로 인코딩
	 * 공백(" ")은 %20으로 치환하여 CloudFront URL에서도 정상 동작하도록 처리
	 *
	 * @param s 원본 문자열 (파일 경로나 파일명)
	 * @return URL 인코딩된 문자열
	 */
	private String urlEncode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * 지정된 디렉토리와 원본 파일명을 기반으로 S3 객체 키(Key)를 생성
	 * 파일명 충돌을 방지하기 위해 타임스탬프(yyyyMMddHHmmss)를 접두사로 붙임
	 * <p>
	 * 예: images/photo.png -> images/20251001153045_photo.png
	 *
	 * @param dirName      업로드할 S3 디렉토리명
	 * @param originalName 원본 파일명
	 * @return 고유한 S3 객체 키
	 */
	private String buildNewKey(String dirName, String originalName) {
		String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		return dirName + "/" + ts + "_" + originalName;
	}

	/**
	 * URL에서 S3 키 추출
	 */
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

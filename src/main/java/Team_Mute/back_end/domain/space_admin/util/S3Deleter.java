package Team_Mute.back_end.domain.space_admin.util;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3Deleter {

	@Value("${cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${cloud.aws.credentials.secret-key}")
	private String secretKey;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.region.static}")
	private String region;

	// URL을 Key로 변환 후 삭제
	public void deleteByUrl(String url) {
		String key = extractKeyFromUrl(url);
		deleteByKey(key);
	}

	// Key로 바로 삭제
	public void deleteByKey(String key) {
		try {
			getS3Client().deleteObject(DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build());
		} catch (NoSuchKeyException e) {
			// 이미 없으면 무시
		} catch (S3Exception e) {
			throw e;
		}
	}

	private S3Client getS3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			))
			.build();
	}

	private String extractKeyFromUrl(String url) {
		try {
			java.net.URI u = java.net.URI.create(url);
			String path = u.getPath();
			String key = path.startsWith("/") ? path.substring(1) : path;
			return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalArgumentException("URL에서 Key 추출 실패: " + url, e);
		}
	}

	public void deleteFolder(String folderPath) {
		S3Client s3Client = getS3Client();

		// 폴더 경로 뒤에 슬래시(/)가 없으면 추가
		// S3에서 폴더는 "폴더명/" 접두사로 인식됨
		if (!folderPath.endsWith("/")) {
			folderPath += "/";
		}

		try {
			// S3 폴더 내 모든 객체(파일) 목록을 가져옴
			ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix(folderPath) // 해당 접두사를 가진 모든 객체를 찾음
				.build();
			ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

			// 폴더가 비어 있으면(객체가 없으면) 작업을 종료
			if (listObjectsV2Response.contents().isEmpty()) {
				return;
			}

			// 삭제할 객체들의 식별자 목록을 생성
			Delete delete = Delete.builder()
				.objects(listObjectsV2Response.contents().stream()
					.map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
					.collect(Collectors.toList()))
				.build();

			// 객체들을 한 번에 삭제하는 요청을 생성
			DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
				.bucket(bucket)
				.delete(delete)
				.build();

			// 일괄 삭제 작업을 실행
			s3Client.deleteObjects(deleteObjectsRequest);
		} catch (S3Exception e) {
			// S3 관련 예외 발생 시, 런타임 예외로 변환하여 던짐
			throw new RuntimeException("폴더 삭제 실패: " + folderPath, e);
		}
	}
}

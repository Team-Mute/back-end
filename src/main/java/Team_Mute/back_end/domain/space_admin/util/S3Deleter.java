package Team_Mute.back_end.domain.space_admin.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
			return java.net.URLDecoder.decode(path.substring(1), java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalArgumentException("S3 URL에서 Key 추출 실패: " + url, e);
		}
	}
}

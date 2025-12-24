package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.S3ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.util.List;

import static net.tylerwade.springbootvideostreaming.TestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class S3ContentStreamAdapterTests {

	@MockitoBean
	public S3Client s3Client;

	private ContentStreamAdapter contentStreamAdapter;

	@BeforeEach
	void setup() {
		contentStreamAdapter = new S3ContentStreamAdapter(s3Client, "test-bucket");
	}

	@Test
	void getAllContentMetadata_returnsMetadata() throws IOException {
		// Arrange
		ListObjectsV2Iterable mockIterable = Mockito.mock(ListObjectsV2Iterable.class);

		when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenReturn(mockIterable);

		when(mockIterable.contents())
				.thenReturn(MOCK_S3_OBJECTS::iterator);

		// Act
		List<StreamedContentMetadata> metadataList = contentStreamAdapter.getAllContentMetadata();

		// Assert
		assertThat(metadataList)
				.hasSize(3)
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(EARTH_SPINNING_S3_OBJECT.key());
					assertThat(metadata.getContentType()).isEqualTo(EARTH_SPINNING_CONTENT_TYPE);
					assertThat(metadata.getFileSize()).isEqualTo(EARTH_SPINNING_S3_OBJECT.size());
				})
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(PARK_S3_OBJECT.key());
					assertThat(metadata.getContentType()).isEqualTo(PARK_CONTENT_TYPE);
					assertThat(metadata.getFileSize()).isEqualTo(PARK_S3_OBJECT.size());
				})
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(SCIENCE_S3_OBJECT.key());
					assertThat(metadata.getContentType()).isEqualTo(SCIENCE_CONTENT_TYPE);
					assertThat(metadata.getFileSize()).isEqualTo(SCIENCE_S3_OBJECT.size());
				});
	}

	@Test
	void getAllContentMetadata_throwsSdkException() {
		// Arrange
		when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenThrow(SdkException.builder().message("Service not available.").build());

		// Act & Assert
		assertThrows(SdkException.class, () -> contentStreamAdapter.getAllContentMetadata());
	}

	@Test
	void getContentMetadata_returnsMetadata() throws IOException {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
								.contentType(EARTH_SPINNING_CONTENT_TYPE)
								.contentLength(EARTH_SPINNING_S3_OBJECT.size())
								.build());

		// Act
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata(EARTH_SPINNING_S3_OBJECT.key());

		// Assert
		assertNotNull(metadata);
		assertEquals(EARTH_SPINNING_S3_OBJECT.key(), metadata.getKey());
		assertEquals(EARTH_SPINNING_CONTENT_TYPE, metadata.getContentType());
		assertEquals(EARTH_SPINNING_S3_OBJECT.size(), metadata.getFileSize());
	}

	@Test
	void getContentMetadata_throwsIoException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		assertThrows(SdkException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void getContentSize_returnsFileSize() throws IOException {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
						.contentType(EARTH_SPINNING_CONTENT_TYPE)
						.contentLength(EARTH_SPINNING_S3_OBJECT.size())
						.build());

		// Act
		Long size = contentStreamAdapter.getContentSize(EARTH_SPINNING_S3_OBJECT.key());

		// Assert
		assertNotNull(size);
		assertEquals(EARTH_SPINNING_S3_OBJECT.size(), size);
	}


	@Test
	void getContentSize_throwsIoException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		assertThrows(SdkException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void loadContent_returnsContent() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
						.contentType(EARTH_SPINNING_CONTENT_TYPE)
						.contentLength(EARTH_SPINNING_S3_OBJECT.size())
						.build());

		StreamContentRequest request = StreamContentRequest.builder()
				.key(EARTH_SPINNING_S3_OBJECT.key())
				.range(new Range(0L, EARTH_SPINNING_S3_OBJECT.size() / 2)) // Half the file size
				.build();

		when(s3Client.getObject(any(GetObjectRequest.class)))
				.thenReturn(mock(ResponseInputStream.class));

		// Act & Assert
	 	StepVerifier.create(contentStreamAdapter.loadContent(request))
				.assertNext(content -> {
					assertNotNull(content);
					assertEquals(EARTH_SPINNING_S3_OBJECT.key(), content.getKey());
					assertNotNull(content.getMetadata());
					assertEquals(EARTH_SPINNING_S3_OBJECT.key(), content.getMetadata().getKey());
					assertEquals(EARTH_SPINNING_CONTENT_TYPE, content.getMetadata().getContentType());
					assertEquals(EARTH_SPINNING_S3_OBJECT.size(), content.getMetadata().getFileSize());
					assertNotNull(content.getContent());
					assertEquals(EARTH_SPINNING_S3_OBJECT.size() / 2 + 1, content.getContentLength());
				}).verifyComplete();
	}

	@Test
	void loadContent_throwsException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		StreamContentRequest request = StreamContentRequest.builder().key("missing-video.mp4").build();
		StepVerifier.create(contentStreamAdapter.loadContent(request))
				.expectError(SdkException.class)
				.verify();
	}

}
package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.S3ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class S3ContentStreamAdapterTests {

	@MockitoBean
	public S3Client s3Client;

	private ContentStreamAdapter contentStreamAdapter;

	private static final S3Object earthSpinningObject = S3Object.builder()
			.key("earth-spinning.mp4")
			.size(873682L)
			.build();

	private static final S3Object parkObject = S3Object.builder()
			.key("park.mp4")
			.size(21657943L)
			.build();

	private static final S3Object scienceVideoObject = S3Object.builder()
			.key("science-video")
			.size(13927646L)
			.build();

	private static final List<S3Object> mockS3Objects = List.of(
			earthSpinningObject, parkObject, scienceVideoObject
	);

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
				.thenReturn(mockS3Objects::iterator);

		// Act
		List<StreamedContentMetadata> metadataList = contentStreamAdapter.getAllContentMetadata();

		// Assert
		assertThat(metadataList)
				.hasSize(3)
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(earthSpinningObject.key());
					assertThat(metadata.getContentType()).isEqualTo("video/mp4");
					assertThat(metadata.getFileSize()).isEqualTo(earthSpinningObject.size());
				})
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(parkObject.key());
					assertThat(metadata.getContentType()).isEqualTo("video/mp4");
					assertThat(metadata.getFileSize()).isEqualTo(parkObject.size());
				})
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(scienceVideoObject.key());
					assertThat(metadata.getContentType()).isEqualTo("application/octet-stream");
					assertThat(metadata.getFileSize()).isEqualTo(scienceVideoObject.size());
				});
	}

	@Test
	void getAllContentMetadata_throwsIoException() {
		// Arrange
		when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
				.thenThrow(SdkException.builder().message("Service not available.").build());

		// Act & Assert
		assertThrows(IOException.class, () -> contentStreamAdapter.getAllContentMetadata());
	}

	@Test
	void getContentMetadata_returnsMetadata() throws IOException {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
								.contentType("video/mp4")
								.contentLength(earthSpinningObject.size())
								.build());

		// Act
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata(earthSpinningObject.key());

		// Assert
		assertNotNull(metadata);
		assertEquals(earthSpinningObject.key(), metadata.getKey());
		assertEquals("video/mp4", metadata.getContentType());
		assertEquals(earthSpinningObject.size(), metadata.getFileSize());
	}

	@Test
	void getContentMetadata_throwsIoException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		assertThrows(IOException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void getContentSize_returnsFileSize() throws IOException {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
						.contentType("video/mp4")
						.contentLength(earthSpinningObject.size())
						.build());

		// Act
		Long size = contentStreamAdapter.getContentSize(earthSpinningObject.key());

		// Assert
		assertNotNull(size);
		assertEquals(earthSpinningObject.size(), size);
	}


	@Test
	void getContentSize_throwsIoException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		assertThrows(IOException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void loadContent_returnsContent() throws IOException {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder()
						.contentType("video/mp4")
						.contentLength(earthSpinningObject.size())
						.build());

		StreamContentRequest request = StreamContentRequest.builder()
				.key(earthSpinningObject.key())
				.range(new Range(0L, earthSpinningObject.size() / 2)) // Half the file size
				.build();

		when(s3Client.getObject(any(GetObjectRequest.class)))
				.thenReturn(mock(ResponseInputStream.class));

		// Act
		StreamedContent content = contentStreamAdapter.loadContent(request);

		// Assert
		assertNotNull(content);
		assertEquals(earthSpinningObject.key(), content.getKey());
		assertNotNull(content.getMetadata());
		assertEquals(earthSpinningObject.key(), content.getMetadata().getKey());
		assertEquals("video/mp4", content.getMetadata().getContentType());
		assertEquals(earthSpinningObject.size(), content.getMetadata().getFileSize());
		assertNotNull(content.getContent());
		assertEquals(earthSpinningObject.size() / 2 + 1, content.getContentLength());
	}

	@Test
	void loadContent_throwsIoException() {
		// Arrange
		when(s3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(SdkException.builder().message("Object not found.").build());

		// Act & Assert
		assertThrows(IOException.class, () -> contentStreamAdapter.loadContent(StreamContentRequest.builder().key("missing-video.mp4").build()));
	}

}
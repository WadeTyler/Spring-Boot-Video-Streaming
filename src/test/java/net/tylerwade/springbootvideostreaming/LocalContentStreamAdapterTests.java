package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.LocalContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;

import static net.tylerwade.springbootvideostreaming.TestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LocalContentStreamAdapterTests {

	@Autowired
	private ResourceLoader resourceLoader;

	private static final String VIDEOS_DIRECTORY = "videos";

	private ContentStreamAdapter contentStreamAdapter;

	@BeforeEach
	void setup() {
		contentStreamAdapter = new LocalContentStreamAdapter(resourceLoader, VIDEOS_DIRECTORY);
	}

	@Test
	void getAllContentMetadata_returnsMetadata() throws IOException {
		List<StreamedContentMetadata> metadataList = contentStreamAdapter.getAllContentMetadata();

		assertNotNull(metadataList);
		assertThat(metadataList)
				.hasSize(3)
				.anySatisfy(metadata -> {
					assertThat(metadata.getKey()).isEqualTo(EARTH_SPINNING_VIDEO_KEY);
					assertThat(metadata.getContentType()).isEqualTo(EARTH_SPINNING_CONTENT_TYPE);
					assertThat(metadata.getFileSize()).isEqualTo(EARTH_SPINNING_FILE_SIZE);
				});
	}

	@Test
	void getContentMetadata_returnsMetadata() throws IOException {
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata(EARTH_SPINNING_VIDEO_KEY);

		assertNotNull(metadata);
		assertEquals(EARTH_SPINNING_VIDEO_KEY, metadata.getKey());
		assertEquals(EARTH_SPINNING_CONTENT_TYPE, metadata.getContentType());
		assertEquals(EARTH_SPINNING_FILE_SIZE, metadata.getFileSize());
	}

	@Test
	void getContentMetadata_throwsMissingResourceException() {
		assertThrows(MissingResourceException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void getContentMetadata_defaultsContentType() throws IOException {
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata(SCIENCE_VIDEO_KEY);

		assertNotNull(metadata);
		assertEquals(SCIENCE_VIDEO_KEY, metadata.getKey());
		assertEquals(SCIENCE_CONTENT_TYPE, metadata.getContentType());
	}

	@Test
	void getContentSize_returnsFileSize() throws IOException {
		Long fileSize = contentStreamAdapter.getContentSize(EARTH_SPINNING_VIDEO_KEY);

		assertEquals(EARTH_SPINNING_FILE_SIZE, fileSize);
	}

	@Test
	void getContentSize_throwsMissingResourceException() {
		assertThrows(MissingResourceException.class, () -> contentStreamAdapter.getContentSize("missing-video.mp4"));
	}

	@Test
	void loadContent_returnsContent() {
		Range range = new Range(0L, EARTH_SPINNING_FILE_SIZE / 2);
		StreamContentRequest streamContentRequest = StreamContentRequest.builder()
				.key(EARTH_SPINNING_VIDEO_KEY)
				.range(range)
				.build();

		StepVerifier.create(contentStreamAdapter.loadContent(streamContentRequest))
				.assertNext(content -> {
					assertNotNull(content);
					assertEquals(EARTH_SPINNING_VIDEO_KEY, content.getKey());
					assertNotNull(content.getMetadata());
					assertEquals(EARTH_SPINNING_VIDEO_KEY, content.getMetadata().getKey());
					assertEquals(EARTH_SPINNING_CONTENT_TYPE, content.getMetadata().getContentType());
					assertEquals(EARTH_SPINNING_FILE_SIZE, content.getMetadata().getFileSize());
					assertEquals(range, content.getRange());
					assertNotNull(content.getContent());
					assertEquals(range.getEnd() - range.getStart() + 1, content.getContentLength());
				}).verifyComplete();
	}

	@Test
	void loadContent_throwsMissingResourceException() {
		StreamContentRequest request = StreamContentRequest.builder().key("missing-video.mp4").build();

		StepVerifier.create(contentStreamAdapter.loadContent(request))
				.expectError(MissingResourceException.class)
				.verify();
	}

	@Test
	void loadContent_returnsMaxChunkSize() {
		StreamContentRequest contentRequest = new StreamContentRequest(PARK_VIDEO_KEY, new Range(0L, null));

		StepVerifier.create(contentStreamAdapter.loadContent(contentRequest))
				.assertNext(content -> {
					assertNotNull(content);
					assertEquals(contentStreamAdapter.getMaxChunkSize(), content.getContentLength());
				}).verifyComplete();
	}

}
package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.adapter.LocalContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LocalContentStreamAdapterTests {

	@Autowired
	private ResourceLoader resourceLoader;

	private static final String VIDEOS_DIRECTORY = "videos";

	private ContentStreamAdapter contentStreamAdapter;

	private static final String earthSpinningVideoKey = "earth-spinning.mp4";
	private static final String earthSpinningContentType = "video/mp4";
	private static final Long earthSpinningFileSize = 873682L;

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
					assertThat(metadata.getKey()).isEqualTo(earthSpinningVideoKey);
					assertThat(metadata.getContentType()).isEqualTo(earthSpinningContentType);
					assertThat(metadata.getFileSize()).isEqualTo(earthSpinningFileSize);
					assertThat(metadata.getRange()).isEqualTo(new Range(0L, earthSpinningFileSize - 1));
				});
	}

	@Test
	void getContentMetadata_returnsMetadata() throws IOException {
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata(earthSpinningVideoKey);

		assertNotNull(metadata);
		assertEquals(earthSpinningVideoKey, metadata.getKey());
		assertEquals(earthSpinningContentType, metadata.getContentType());
		assertEquals(earthSpinningFileSize, metadata.getFileSize());
		assertEquals(new Range(0L, metadata.getFileSize() - 1), metadata.getRange());
	}

	@Test
	void getContentMetadata_throwsMissingResourceException() {
		assertThrows(MissingResourceException.class, () -> contentStreamAdapter.getContentMetadata("missing-video.mp4"));
	}

	@Test
	void getContentMetadata_defaultsContentType() throws IOException {
		StreamedContentMetadata metadata = contentStreamAdapter.getContentMetadata("science-video");

		assertNotNull(metadata);
		assertEquals("science-video", metadata.getKey());
		assertEquals("application/octet-stream", metadata.getContentType());
	}

	@Test
	void getContentSize_returnsFileSize() throws IOException {
		Long fileSize = contentStreamAdapter.getContentSize(earthSpinningVideoKey);

		assertEquals(earthSpinningFileSize, fileSize);
	}

	@Test
	void getContentSize_throwsMissingResourceException() {
		assertThrows(MissingResourceException.class, () -> contentStreamAdapter.getContentSize("missing-video.mp4"));
	}

	@Test
	void loadContent_returnsContent() throws IOException {
		Range range = new Range(0L, earthSpinningFileSize / 2);
		StreamContentRequest streamContentRequest = StreamContentRequest.builder()
				.key(earthSpinningVideoKey)
				.range(range)
				.build();

		StreamedContent content = contentStreamAdapter.loadContent(streamContentRequest);

		assertNotNull(content);
		assertEquals(earthSpinningVideoKey, content.getKey());
		assertNotNull(content.getMetadata());
		assertEquals(earthSpinningVideoKey, content.getMetadata().getKey());
		assertEquals(earthSpinningContentType, content.getMetadata().getContentType());
		assertEquals(earthSpinningFileSize, content.getMetadata().getFileSize());
		assertEquals(range, content.getMetadata().getRange());
		assertNotNull(content.getContent());
		assertEquals(range.getEnd() - range.getStart() + 1, content.getContentLength());
	}

	@Test
	void loadContent_throwsMissingResourceException() {
		assertThrows(MissingResourceException.class, () -> contentStreamAdapter.loadContent(StreamContentRequest.builder().key("missing-video.mp4").build()));
	}

	@Test
	void loadContent_returnsMaxChunkSize() throws IOException {
		StreamContentRequest contentRequest = new StreamContentRequest("park.mp4", new Range(0L, null));
		StreamedContent content = contentStreamAdapter.loadContent(contentRequest);

		long MAX_CHUNK_SIZE = 1024 * 1024L; // 1 MB

		assertNotNull(content);
		assertEquals(MAX_CHUNK_SIZE, content.getContentLength());
	}

}
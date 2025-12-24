package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static net.tylerwade.springbootvideostreaming.TestResources.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StreamedContentTests {

	@Autowired
	private ContentStreamAdapter contentStreamAdapter;

	@Test
	void toResponseEntity_returns206PartialContent() {
		// Act & Assert
		StreamContentRequest request = StreamContentRequest.builder()
				.key(EARTH_SPINNING_VIDEO_KEY)
				.range(new Range(0L, EARTH_SPINNING_FILE_SIZE / 2))
				.build();

		StepVerifier.create(contentStreamAdapter.loadContent(request))
				.assertNext(content -> {
					ResponseEntity<Flux<DataBuffer>> responseEntity = content.toResponseEntity();
					// Assert
					assertNotNull(responseEntity);
					assertEquals(206, responseEntity.getStatusCode().value());
					assertEquals(MediaType.valueOf(EARTH_SPINNING_CONTENT_TYPE), responseEntity.getHeaders().getContentType());
					assertEquals("bytes", responseEntity.getHeaders().getFirst("Accept-Ranges"));
					assertEquals(EARTH_SPINNING_FILE_SIZE / 2 + 1, responseEntity.getHeaders().getContentLength());
					assertEquals(String.format("bytes %s-%s/%s", 0, EARTH_SPINNING_FILE_SIZE / 2, EARTH_SPINNING_FILE_SIZE),
							responseEntity.getHeaders().getFirst("Content-Range"));
					assertNotNull(responseEntity.getBody());
					assertTrue(responseEntity.hasBody());
				}).verifyComplete();
	}

}
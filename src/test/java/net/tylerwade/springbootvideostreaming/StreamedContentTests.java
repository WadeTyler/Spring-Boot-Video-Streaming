package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

import static net.tylerwade.springbootvideostreaming.TestResources.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StreamedContentTests {

	@Autowired
	private ContentStreamAdapter contentStreamAdapter;

	@Test
	void toResponseEntity_returns206PartialContent() throws IOException {
		// Act
		ResponseEntity<StreamingResponseBody> responseEntity = contentStreamAdapter.loadContent(StreamContentRequest.builder()
						.key(EARTH_SPINNING_VIDEO_KEY)
						.range(new Range(0L, EARTH_SPINNING_FILE_SIZE / 2))
						.build())
				.toResponseEntity();

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


	}

}
package net.tylerwade.s3example;

import lombok.RequiredArgsConstructor;
import net.tylerwade.springbootvideostreaming.adapter.ContentStreamAdapter;
import net.tylerwade.springbootvideostreaming.model.Range;
import net.tylerwade.springbootvideostreaming.model.StreamContentRequest;
import net.tylerwade.springbootvideostreaming.model.StreamedContent;
import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

	// Will be an S3ContentStreamAdapter
	private final ContentStreamAdapter contentStreamAdapter;

	@GetMapping
	public List<StreamedContentMetadata> getAllVideosMetadata() throws IOException {
		return contentStreamAdapter.getAllContentMetadata();
	}

	@GetMapping("/{key}/metadata")
	public StreamedContentMetadata getVideoMetadata(@PathVariable String key) throws IOException {
		return contentStreamAdapter.getContentMetadata(key);
	}

	@GetMapping("/{key}/size")
	public Long getVideoSize(@PathVariable String key) throws IOException {
		return contentStreamAdapter.getContentSize(key);
	}


	// You can visit http://localhost:8080/api/v1/videos/park.mp4 in your browser and notice that the video automatically
	// will play and stream.
	//
	// Range header should be parsed to a Range object.
	// Modern browsers will automatically send the Range header when used with HTML's <video> tag.
	// For more information check here:
	// https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Range
	@GetMapping("/{key}")
	public Mono<ResponseEntity<Flux<DataBuffer>>> getVideoContent(@PathVariable String key,
																  @RequestHeader(value = "Range", required = false) String rangeHeader) {
		Range range = parseRangeHeader(rangeHeader);

		StreamContentRequest contentRequest = StreamContentRequest
				.builder()
				.key(key)
				.range(range)
				.build();

		// Load the content.
		// You can use .toResponseEntity() to directly convert to response entity, or you may use however needed.
		return contentStreamAdapter.loadContent(contentRequest)
				.map(StreamedContent::toResponseEntity);
	}

	private static Range parseRangeHeader(String rangeHeader) {
		if (rangeHeader == null || !rangeHeader.contains("=")) {
			return new Range(0L, null);
		}

		String[] parts = rangeHeader.split("=", 2);
		String[] rangeValues = parts[1].split("-", 2);

		Long start = parseLongOrNull(rangeValues[0]);
		Long end = (rangeValues.length > 1) ? parseLongOrNull(rangeValues[1]) : null;

		return new Range(start != null ? start : 0L, end);
	}

	private static Long parseLongOrNull(String value) {
		try {
			return (value == null || value.isBlank()) ? null : Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}


}
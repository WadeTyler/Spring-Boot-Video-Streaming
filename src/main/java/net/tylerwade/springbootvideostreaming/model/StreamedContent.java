package net.tylerwade.springbootvideostreaming.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
@EqualsAndHashCode
public class StreamedContent {

	private String key;

	private StreamedContentMetadata metadata;

	private Flux<DataBuffer> content;

	private Long contentLength;

	private Range range;

	/**
	 * Converts the {@code StreamedContent} object into a {@code ResponseEntity} object.
	 */
	@JsonIgnore
	public ResponseEntity<Flux<DataBuffer>> toResponseEntity() {
		boolean isCompleteContent = range.getStart() == 0 && range.getEnd() == metadata.getFileSize() - 1;

		return ResponseEntity.status(isCompleteContent ? 200 : 206)
				.header("Content-Type", metadata.getContentType())
				.header("Accept-Ranges", "bytes")
				.header("Content-Length", String.valueOf(contentLength))
				.header("Content-Range", String.format("bytes %s-%s/%s",
						range.getStart(), range.getEnd(), metadata.getFileSize()))
				.body(content);
	}

}
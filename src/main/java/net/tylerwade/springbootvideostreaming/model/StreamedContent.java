package net.tylerwade.springbootvideostreaming.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
@EqualsAndHashCode
public class StreamedContent {

	private String key;

	private StreamedContentMetadata metadata;

	private StreamingResponseBody content;

	private Long contentLength;

}
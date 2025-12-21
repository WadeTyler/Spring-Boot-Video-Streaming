package net.tylerwade.springbootvideostreaming.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class StreamedContentMetadata {

	private String key;
	private String contentType;
	private Long fileSize;

}
package net.tylerwade.springbootvideostreaming;

import net.tylerwade.springbootvideostreaming.model.StreamedContentMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

public class TestResources {

	public static final String EARTH_SPINNING_VIDEO_KEY = "earth-spinning.mp4";
	public static final String EARTH_SPINNING_CONTENT_TYPE = "video/mp4";
	public static final Long EARTH_SPINNING_FILE_SIZE = 873682L;

	public static final String PARK_VIDEO_KEY = "park.mp4";
	public static final String PARK_CONTENT_TYPE = "video/mp4";
	public static final Long PARK_FILE_SIZE = 21657943L;

	public static final String SCIENCE_VIDEO_KEY = "science-video";
	public static final String SCIENCE_CONTENT_TYPE = "application/octet-stream";
	public static final Long SCIENCE_FILE_SIZE = 13927646L;

	public static final StreamedContentMetadata EARTH_SPINNING_METADATA = StreamedContentMetadata.builder()
			.key(EARTH_SPINNING_VIDEO_KEY)
			.contentType(EARTH_SPINNING_CONTENT_TYPE)
			.fileSize(EARTH_SPINNING_FILE_SIZE)
			.build();

	public static final StreamedContentMetadata PARK_METADATA = StreamedContentMetadata.builder()
			.key(PARK_VIDEO_KEY)
			.contentType(PARK_CONTENT_TYPE)
			.fileSize(PARK_FILE_SIZE)
			.build();

	public static final StreamedContentMetadata SCIENCE_METADATA = StreamedContentMetadata.builder()
			.key(SCIENCE_VIDEO_KEY)
			.contentType(SCIENCE_CONTENT_TYPE)
			.fileSize(SCIENCE_FILE_SIZE)
			.build();

	public static final S3Object EARTH_SPINNING_S3_OBJECT = S3Object.builder()
			.key(EARTH_SPINNING_VIDEO_KEY)
			.size(EARTH_SPINNING_FILE_SIZE)
			.build();

	public static final S3Object PARK_S3_OBJECT = S3Object.builder()
			.key(PARK_VIDEO_KEY)
			.size(PARK_FILE_SIZE)
			.build();

	public static final S3Object SCIENCE_S3_OBJECT = S3Object.builder()
			.key(SCIENCE_VIDEO_KEY)
			.size(SCIENCE_FILE_SIZE)
			.build();

	public static final List<S3Object> MOCK_S3_OBJECTS = List.of(
			EARTH_SPINNING_S3_OBJECT, PARK_S3_OBJECT, SCIENCE_S3_OBJECT
	);
}
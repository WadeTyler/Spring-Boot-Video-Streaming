# Spring Boot Video Streaming Library

Streaming video efficiently in a Spring Boot application can be surprisingly complex. Handling byte-range requests (HTTP 206), calculating offsets, and managing different storage backends often leads to a lot of boilerplate code.

This library simplifies that process. It provides a clean abstraction for streaming video content, allowing you to focus on your application logic while we handle the heavy lifting of chunked delivery and range management.

## Key Features

- **Byte-Range Support:** Seamless handling of `Range` headers for seekable video playback.
- **Pluggable Storage:** Comes with built-in support for both local file systems and Amazon S3.
- **Smart Chunking:** Prevents memory issues by streaming content in 1MB chunks (configurable).
- **Spring Boot Auto-configuration:** Works out of the box with zero configuration for local files.

## Getting Started

### Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>net.tylerwade</groupId>
    <artifactId>spring-boot-video-streaming</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage (Local Files)

By default, the library looks for videos in `src/main/resources/videos`. You can start streaming immediately by injecting the `ContentStreamAdapter` into your controller.

```java
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final ContentStreamAdapter contentStreamAdapter;

    @GetMapping("/{key}")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @PathVariable String key,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        // 1. Create a request with the video key and the requested range
        StreamContentRequest request = StreamContentRequest.builder()
                .key(key)
                .range(parseRangeHeader(rangeHeader)) // See examples for helper method
                .build();

        // 2. Load the content through the adapter
        StreamedContent content = contentStreamAdapter.loadContent(request);

        // 3. Return the response entity (headers like Content-Range are handled automatically)
        return content.toResponseEntity();
    }
}
```

## Storage Adapters

### Local Storage (Default)
The `LocalContentStreamAdapter` is auto-configured if no other adapter is defined. It serves files from the classpath.

- **Default path:** `classpath:videos`
- **Customization:** To change the directory, you can define your own bean:
  ```java
  @Bean
  public ContentStreamAdapter contentStreamAdapter(ResourceLoader resourceLoader) {
      return new LocalContentStreamAdapter(resourceLoader, "my-custom-video-folder");
  }
  ```

### Amazon S3
To stream from S3, define an `S3ContentStreamAdapter` bean in your configuration.

```java
@Bean
public ContentStreamAdapter contentStreamAdapter(S3Client s3Client) {
    return new S3ContentStreamAdapter(s3Client, "your-s3-bucket-name");
}
```

## How It Works

1. **Range Parsing:** When a browser requests a video, it usually sends a `Range` header (e.g., `bytes=0-`).
2. **Chunking:** The library calculates the appropriate byte range to return, ensuring it doesn't exceed the `MAX_CHUNK_SIZE` (1MB). This keeps your application's memory footprint low even with high concurrency.
3. **Non-blocking IO:** We use Spring's `StreamingResponseBody` to stream the data directly to the HTTP response output stream.
4. **Automatic Headers:** The `.toResponseEntity()` helper automatically sets the correct `Content-Type`, `Content-Length`, `Content-Range`, and `Accept-Ranges` headers.

## Pro-Tips

- **Browser Compatibility:** Modern browsers automatically handle range requests when you use the standard HTML5 `<video>` tag.
- **Metadata:** You can use `contentStreamAdapter.getContentMetadata(key)` to get the file size and MIME type without loading the actual content.

---
Built with ❤️ for Spring Boot developers.
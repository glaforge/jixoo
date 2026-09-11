/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.glaforge.jixoo.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.cloud.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * Client for interacting with the Divoom Cloud REST API ({@code https://appin.divoom-gz.com}).
 * <p>
 * Supports authentication, asset upload, persistent Custom Channel updating (via cloud MQTT push to device),
 * and publishing creations to the Divoom Cloud gallery.
 */
public class DivoomCloudClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DivoomCloudClient.class);
    private static final String CLOUD_BASE_URL = "https://appin.divoom-gz.com/";
    private static final String CDN_BASE_URL = "https://fin.divoom-gz.com/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final Duration timeout;

    public DivoomCloudClient() {
        this(Duration.ofSeconds(60));
    }

    public DivoomCloudClient(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Authenticates with Divoom cloud services using user email and plain password.
     * The password is MD5 hashed locally before transmission.
     *
     * @param email    the Divoom user email
     * @param password the plain text password
     * @return an authenticated {@link DivoomCloudSession}
     */
    public DivoomCloudSession login(String email, String password) {
        String md5Password = md5(password);
        Map<String, Object> body = Map.of(
                "Email", email,
                "Password", md5Password
        );

        DivoomLoginResponse response = postJson("UserLogin", body, DivoomLoginResponse.class);
        if (!response.isSuccess()) {
            throw new PixooException("Divoom Cloud Login failed: Code " + response.returnCode() + " - " + response.returnMessage());
        }

        DivoomCloudSession tempSession = new DivoomCloudSession(response.userId(), response.token(), 0, email);

        // Fetch bound device list to automatically resolve default deviceId
        long deviceId = 0;
        try {
            DivoomDeviceListResponse deviceListResponse = getDeviceList(tempSession);
            if (!deviceListResponse.deviceList().isEmpty()) {
                deviceId = deviceListResponse.deviceList().get(0).deviceId();
            }
        } catch (Exception e) {
            log.warn("Could not automatically retrieve bound device ID: {}", e.getMessage());
        }

        return new DivoomCloudSession(response.userId(), response.token(), deviceId, email);
    }

    /**
     * Retrieves the list of Divoom hardware devices bound to the user's account.
     *
     * @param session the active cloud session
     * @return device list response
     */
    public DivoomDeviceListResponse getDeviceList(DivoomCloudSession session) {
        Map<String, Object> body = Map.of(
                "UserId", session.userId(),
                "Token", session.token()
        );
        return postJson("Device/GetListV2", body, DivoomDeviceListResponse.class);
    }

    /**
     * Uploads a Divoom binary animation (or image) to the Divoom CDN.
     *
     * @param session    the active cloud session
     * @param binaryData the Divoom binary format bytes (e.g. from {@link DivoomAnimationEncoder})
     * @return the unique FileId string assigned by Divoom (e.g. {@code group1/M00/...})
     */
    public String uploadPicture(DivoomCloudSession session, byte[] binaryData) {
        String boundary = "----DivoomFormBoundary" + System.currentTimeMillis();

        Map<String, Object> jsonMeta = new HashMap<>();
        jsonMeta.put("UserId", session.userId());
        jsonMeta.put("Token", session.token());
        if (session.deviceId() != 0) {
            jsonMeta.put("DeviceId", session.deviceId());
        }

        try {
            String jsonString = MAPPER.writeValueAsString(jsonMeta);
            byte[] multipartBody = buildMultipartBody(boundary, "upFile", "upFile.bin", binaryData, "json", jsonString);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_BASE_URL + "Cloud/UploadPicture"))
                    .timeout(timeout)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("User-Agent", "Aurabox/3.1.10 (iPad; iOS 14.8)")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new PixooException("Failed to upload image to Divoom Cloud: HTTP " + response.statusCode() + " - " + response.body());
            }

            DivoomFileUploadResponse uploadResponse = MAPPER.readValue(response.body(), DivoomFileUploadResponse.class);
            if (!uploadResponse.isSuccess() || uploadResponse.fileId() == null || uploadResponse.fileId().isBlank()) {
                throw new PixooException("Upload rejected by Divoom Cloud: " + response.body());
            }

            return uploadResponse.fileId();
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Exception during Divoom Cloud upload: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the list of animations/images currently saved in a custom channel slot.
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @param deviceId        the device ID (or 0 to use the session default)
     * @return the list response
     */
    public DivoomCustomListResponse getCustomList(DivoomCloudSession session, int customPageIndex, long deviceId) {
        long targetDeviceId = deviceId != 0 ? deviceId : session.deviceId();
        if (targetDeviceId == 0) {
            throw new IllegalArgumentException("Target deviceId must be specified or resolved during login");
        }

        Map<String, Object> body = Map.of(
                "Command", "Channel/GetCustomList",
                "CustomPageIndex", customPageIndex,
                "DeviceId", targetDeviceId,
                "UserId", session.userId(),
                "Token", session.token()
        );

        return postJson("Channel/GetCustomList", body, DivoomCustomListResponse.class);
    }

    /**
     * Retrieves the list of animations assigned to a specific custom channel slot for the default device.
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @return the list response
     */
    public DivoomCustomListResponse getCustomList(DivoomCloudSession session, int customPageIndex) {
        return getCustomList(session, customPageIndex, 0L);
    }

    /**
     * Assigns an uploaded binary asset to a specific custom channel slot and position index.
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @param customId        the item index / position in the playlist (1-based)
     * @param fileId          the FileId returned from {@link #uploadPicture}
     * @param deviceId        the device ID (or 0 to use the session default)
     * @return the API response
     */
    public DivoomApiResponse setCustomChannel(DivoomCloudSession session, int customPageIndex, int customId, String fileId, long deviceId) {
        long targetDeviceId = deviceId != 0 ? deviceId : session.deviceId();
        if (targetDeviceId == 0) {
            throw new IllegalArgumentException("Target deviceId must be specified or resolved during login");
        }

        Map<String, Object> body = Map.of(
                "Command", "Channel/SetCustom",
                "CustomPageIndex", customPageIndex,
                "CustomId", customId,
                "FileId", fileId,
                "DeviceId", targetDeviceId,
                "UserId", session.userId(),
                "Token", session.token()
        );

        return postJson("Channel/SetCustom", body, DivoomApiResponse.class);
    }

    /**
     * Assigns an uploaded binary asset to a specific custom channel slot (at default index 1).
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @param fileId          the FileId returned from {@link #uploadPicture}
     * @param deviceId        the device ID (or 0 to use the session default)
     * @return the API response
     */
    public DivoomApiResponse setCustomChannel(DivoomCloudSession session, int customPageIndex, String fileId, long deviceId) {
        return setCustomChannel(session, customPageIndex, 1, fileId, deviceId);
    }

    /**
     * Deletes an animation/image item from a custom channel slot playlist.
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @param customId        the item index / position in the playlist to delete
     * @param deviceId        the device ID (or 0 to use the session default)
     * @return the API response
     */
    public DivoomApiResponse deleteCustom(DivoomCloudSession session, int customPageIndex, int customId, long deviceId) {
        long targetDeviceId = deviceId != 0 ? deviceId : session.deviceId();
        if (targetDeviceId == 0) {
            throw new IllegalArgumentException("Target deviceId must be specified or resolved during login");
        }

        Map<String, Object> body = Map.of(
                "Command", "Channel/DeleteCustom",
                "CustomPageIndex", customPageIndex,
                "CustomId", customId,
                "DeviceId", targetDeviceId,
                "UserId", session.userId(),
                "Token", session.token()
        );

        return postJson("Channel/DeleteCustom", body, DivoomApiResponse.class);
    }

    /**
     * Clears all animations/images from a custom channel slot playlist.
     *
     * @param session         the active cloud session
     * @param customPageIndex the custom page index (0, 1, or 2)
     * @param deviceId        the device ID (or 0 to use the session default)
     * @return the API response
     */
    public DivoomApiResponse cleanCustom(DivoomCloudSession session, int customPageIndex, long deviceId) {
        long targetDeviceId = deviceId != 0 ? deviceId : session.deviceId();
        if (targetDeviceId == 0) {
            throw new IllegalArgumentException("Target deviceId must be specified or resolved during login");
        }

        Map<String, Object> body = Map.of(
                "Command", "Channel/CleanCustom",
                "CustomPageIndex", customPageIndex,
                "DeviceId", targetDeviceId,
                "UserId", session.userId(),
                "Token", session.token()
        );

        return postJson("Channel/CleanCustom", body, DivoomApiResponse.class);
    }

    /**
     * Publishes a binary animation or image directly to the user's Divoom Cloud gallery.
     *
     * @param session     the active cloud session
     * @param binaryData  the Divoom 64x64 binary format bytes
     * @param fileName    the title of the artwork
     * @param description the description content
     * @param isPrivate   true to mark private (visible only to user), false for public
     * @param deviceId    the device ID (or 0 to use the session default)
     * @return the API response containing GalleryId and PixelFileId
     */
    public DivoomApiResponse uploadToGallery(DivoomCloudSession session, byte[] binaryData, String fileName, String description, boolean isPrivate, long deviceId) {
        long targetDeviceId = deviceId != 0 ? deviceId : session.deviceId();
        String fileMd5 = md5(binaryData);
        String boundary = "----DivoomFormBoundary" + System.currentTimeMillis();

        Map<String, Object> jsonMeta = new LinkedHashMap<>();
        jsonMeta.put("Classify", 1);
        jsonMeta.put("Content", description != null ? description : fileName);
        jsonMeta.put("CopyrightFlag", 1);
        jsonMeta.put("DeviceId", targetDeviceId);
        jsonMeta.put("FileMD5", fileMd5);
        jsonMeta.put("FileName", fileName);
        jsonMeta.put("FileSize", 4); // 4 = 64x64 resolution
        jsonMeta.put("FileType", 2); // 2 = Animation
        jsonMeta.put("HideFlag", isPrivate ? 1 : 0);
        jsonMeta.put("PrivateFlag", isPrivate ? 1 : 0);
        jsonMeta.put("IsAndroid", 1);
        jsonMeta.put("Version", 12);
        jsonMeta.put("UserId", session.userId());
        jsonMeta.put("Token", session.token());

        try {
            byte[] multipartBody = buildMultipartBody(boundary, "pixelFile", "pixelFile", binaryData, "json", MAPPER.writeValueAsString(jsonMeta));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_BASE_URL + "Cloud/GalleryUploadV3"))
                    .timeout(timeout)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("User-Agent", "Aurabox/3.1.10 (iPad; iOS 14.8)")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new PixooException("Divoom Cloud Gallery upload failed: HTTP " + response.statusCode() + " - " + response.body());
            }

            return MAPPER.readValue(response.body(), DivoomApiResponse.class);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Error uploading to Divoom Cloud Gallery: " + e.getMessage(), e);
        }
    }

    private <T> T postJson(String endpoint, Object body, Class<T> responseType) {
        try {
            String jsonPayload = MAPPER.writeValueAsString(body);
            log.debug("Sending Cloud JSON to {}{}: {}", CLOUD_BASE_URL, endpoint, jsonPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_BASE_URL + endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("User-Agent", "Aurabox/3.1.10 (iPad; iOS 14.8)")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new PixooException("Divoom Cloud request " + endpoint + " failed: HTTP " + response.statusCode() + " - " + response.body());
            }

            return MAPPER.readValue(response.body(), responseType);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Error communicating with Divoom Cloud (" + endpoint + "): " + e.getMessage(), e);
        }
    }

    private static byte[] buildMultipartBody(String boundary, String fileField, String fileName, byte[] fileBytes, String jsonField, String jsonValue) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String lineBreak = "\r\n";

        // 1. File part
        baos.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: application/octet-stream" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        baos.write(fileBytes);
        baos.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        // 2. JSON meta part
        if (jsonField != null && jsonValue != null) {
            baos.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"" + jsonField + "\"" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
            baos.write(jsonValue.getBytes(StandardCharsets.UTF_8));
            baos.write(lineBreak.getBytes(StandardCharsets.UTF_8));
        }

        // Final boundary
        baos.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    /**
     * Computes the hexadecimal MD5 hash of a given string.
     *
     * @param input the input string
     * @return 32-character lowercase hex MD5 string
     */
    public static String md5(String input) {
        return md5(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the hexadecimal MD5 hash of a given byte array.
     *
     * @param input the input byte array
     * @return 32-character lowercase hex MD5 string
     */
    public static String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm unavailable", e);
        }
    }

    /**
     * Loads a cached Divoom cloud session from {@code ~/.jixoo/cloud_session.json} if present.
     *
     * @return the loaded session, or null if not found
     */
    public static DivoomCloudSession loadCachedSession() {
        Path path = getSessionFilePath();
        if (Files.exists(path)) {
            try {
                return MAPPER.readValue(path.toFile(), DivoomCloudSession.class);
            } catch (Exception e) {
                log.warn("Failed to load cached session from {}: {}", path, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Downloads a binary pixel art asset directly from the Divoom CDN.
     *
     * @param fileId the FileId (e.g. {@code group1/M00/...})
     * @return raw binary container bytes
     */
    public byte[] downloadAsset(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId cannot be null or blank");
        }
        String cleanId = fileId.startsWith("/") ? fileId.substring(1) : fileId;
        String url = CDN_BASE_URL + cleanId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "Aurabox/3.1.10 (iPad; iOS 14.8)")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new PixooException("Failed to download asset from CDN: HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Error downloading asset " + fileId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Browses curated community 64x64 pixel art in the Divoom Public Gallery.
     * This endpoint does not require user authentication.
     *
     * @param classify category identifier (0=all, 1=pixel art, 2=celebrity, 3=anime/games, etc.)
     * @param sort     sorting order (POPULAR or NEWEST)
     * @param startNum pagination start offset (1-based)
     * @param endNum   pagination end offset (inclusive)
     * @return gallery listing response
     */
    public CloudGalleryResponse browseGallery(int classify, GallerySort sort, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "Classify", classify,
                "FileSize", 4, // 64x64 resolution
                "FileSort", sort != null ? sort.getValue() : 1,
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "FileType", 5,
                "RefreshIndex", 0,
                "Version", 19,
                "UserId", 0,
                "Token", 0
        );
        return postJson("GetCategoryFileListV2", body, CloudGalleryResponse.class);
    }

    /**
     * Searches community 64x64 pixel art by keyword.
     * This endpoint does not require user authentication.
     *
     * @param keyword  search query string
     * @param startNum pagination start offset (1-based)
     * @param endNum   pagination end offset (inclusive)
     * @return gallery search results
     */
    public CloudGalleryResponse searchGallery(String keyword, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "Keywords", keyword,
                "KeywordsEn", keyword,
                "FileSize", 4,
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "FileType", 5,
                "SortType", 0,
                "Version", 19,
                "UserId", 0,
                "Token", 0
        );
        return postJson("SearchGalleryV3", body, CloudGalleryResponse.class);
    }

    /**
     * Retrieves all public artworks uploaded by an artist / creator.
     * This endpoint does not require user authentication.
     *
     * @param artistUserId user ID of the creator
     * @param startNum     pagination start offset (1-based)
     * @param endNum       pagination end offset (inclusive)
     * @return creator's public artwork gallery
     */
    public CloudGalleryResponse getArtistArtworks(long artistUserId, int startNum, int endNum) {
        return getArtistArtworks(artistUserId, 4, startNum, endNum);
    }

    /**
     * Retrieves public artworks uploaded by an artist / creator filtered by target resolution.
     *
     * @param artistUserId user ID of the creator
     * @param fileSize     resolution filter (4 for 64x64, 127 for all resolutions)
     * @param startNum     pagination start offset (1-based)
     * @param endNum       pagination end offset (inclusive)
     * @return creator's public artwork gallery
     */
    public CloudGalleryResponse getArtistArtworks(long artistUserId, int fileSize, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("SomeOneUserId", artistUserId);
        body.put("ShowAllFlag", 1);
        body.put("Classify", 0);
        body.put("FileSize", fileSize);
        body.put("FileType", 5);
        body.put("FileSort", 0);
        body.put("RefreshIndex", 0);
        body.put("Version", 19);
        body.put("StartNum", actualStart);
        body.put("EndNum", actualEnd);
        body.put("UserId", 0);
        body.put("Token", 0);
        return postJson("GetSomeoneListV3", body, CloudGalleryResponse.class);
    }

    /**
     * Retrieves public profile metadata and follower statistics for a creator.
     *
     * @param artistUserId user ID of the creator
     * @return creator profile
     */
    public ArtistProfile getArtistProfile(long artistUserId) {
        Map<String, Object> body = Map.of(
                "SomeOneUserId", artistUserId,
                "UserId", 0,
                "Token", 0
        );
        return postJson("GetSomeoneInfoV2", body, ArtistProfile.class);
    }

    /**
     * Retrieves the artworks uploaded by the authenticated user.
     *
     * @param session  the active cloud session
     * @param startNum pagination start offset (1-based)
     * @param endNum   pagination end offset (inclusive)
     * @return user's uploaded artworks
     */
    public CloudGalleryResponse getMyUploads(DivoomCloudSession session, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "UserId", session.userId(),
                "Token", session.token()
        );
        return postJson("GetMyUploadListV3", body, CloudGalleryResponse.class);
    }

    /**
     * Retrieves the artworks favorited/liked by the authenticated user.
     *
     * @param session  the active cloud session
     * @param startNum pagination start offset (1-based)
     * @param endNum   pagination end offset (inclusive)
     * @return user's liked artworks
     */
    public CloudGalleryResponse getMyLikes(DivoomCloudSession session, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "UserId", session.userId(),
                "Token", session.token()
        );
        return postJson("GetMyLikeListV3", body, CloudGalleryResponse.class);
    }

    /**
     * Retrieves the top 20 official community clock dial faces for a device.
     *
     * @param deviceId the registered device ID
     * @param startNum pagination start offset (1-based)
     * @param endNum   pagination end offset (inclusive)
     * @return top community clocks
     */
    public ClockStoreResponse getTopClocks(long deviceId, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "Flag", 1,
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "DeviceId", deviceId,
                "Language", "en",
                "CountryISOCode", "US"
        );
        return postJson("Channel/StoreTop20", body, ClockStoreResponse.class);
    }

    /**
     * Browses community clock dial faces by classification.
     *
     * @param deviceId   the registered device ID
     * @param classifyId category identifier
     * @param startNum   pagination start offset (1-based)
     * @param endNum     pagination end offset (inclusive)
     * @return community clocks in category
     */
    public ClockStoreResponse browseClocks(long deviceId, int classifyId, int startNum, int endNum) {
        int actualStart = Math.max(1, startNum);
        int actualEnd = Math.max(actualStart, endNum);
        Map<String, Object> body = Map.of(
                "ClassifyId", classifyId,
                "Flag", 0,
                "StartNum", actualStart,
                "EndNum", actualEnd,
                "DeviceId", deviceId,
                "Language", "en",
                "CountryISOCode", "US"
        );
        return postJson("Channel/StoreClockGetList", body, ClockStoreResponse.class);
    }

    /**
     * Saves a Divoom cloud session to {@code ~/.jixoo/cloud_session.json}.
     *
     * @param session the session to save
     */
    public static void saveSession(DivoomCloudSession session) {
        Path path = getSessionFilePath();
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), session);
        } catch (Exception e) {
            log.warn("Failed to save session to {}: {}", path, e.getMessage());
        }
    }

    /**
     * Logs out the active user session from Divoom Cloud and invalidates the session token.
     *
     * @param session the session to invalidate
     * @return the API response
     */
    public DivoomApiResponse logout(DivoomCloudSession session) {
        if (session == null) {
            clearSession();
            return new DivoomApiResponse();
        }
        Map<String, Object> body = Map.of(
                "UserId", session.userId(),
                "Token", session.token()
        );
        try {
            return postJson("UserLogout", body, DivoomApiResponse.class);
        } catch (Exception e) {
            log.warn("UserLogout call failed: {}", e.getMessage());
            return new DivoomApiResponse();
        } finally {
            clearSession();
        }
    }

    /**
     * Clears and deletes the cached cloud session file from {@code ~/.jixoo/cloud_session.json}.
     */
    public static void clearSession() {
        Path path = getSessionFilePath();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete session file {}: {}", path, e.getMessage());
        }
    }

    private static Path getSessionFilePath() {
        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, ".jixoo", "cloud_session.json");
    }
}

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
package io.github.glaforge.jixoo.cli.command;

import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.cli.PixooCli;
import io.github.glaforge.jixoo.cloud.DivoomAnimationEncoder;
import io.github.glaforge.jixoo.cloud.DivoomCloudClient;
import io.github.glaforge.jixoo.cloud.model.*;
import io.github.glaforge.jixoo.image.DivoomAssetDecoder;
import io.github.glaforge.jixoo.image.GifDecoder;
import io.github.glaforge.jixoo.image.GifEncoder;
import io.github.glaforge.jixoo.image.ImageProcessor;
import io.github.glaforge.jixoo.model.PixooAnimation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI Command group for interacting with Divoom Cloud services.
 */
@Command(
        name = "cloud",
        description = "Manage Divoom Cloud integration, persistent Custom Channels, and public Gallery discovery.",
        subcommands = {
                CloudCommand.LoginCommand.class,
                CloudCommand.LogoutCommand.class,
                CloudCommand.DevicesCommand.class,
                CloudCommand.CustomChannelCommand.class,
                CloudCommand.GalleryCommand.class,
                CloudCommand.BrowseCommand.class,
                CloudCommand.SearchCommand.class,
                CloudCommand.ArtistCommand.class,
                CloudCommand.UploadsCommand.class,
                CloudCommand.LikesCommand.class,
                CloudCommand.DownloadCommand.class,
                CloudCommand.PlayCommand.class
        }
)
public class CloudCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    PixooCli cliParent;

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    private static DivoomCloudSession resolveSession(String email, String password) {
        if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = client.login(email, password);
                DivoomCloudClient.saveSession(session);
                return session;
            }
        }

        // Try environment variables
        String envEmail = System.getenv("DIVOOM_EMAIL");
        String envPass = System.getenv("DIVOOM_PASSWORD");
        if (envEmail != null && !envEmail.isBlank() && envPass != null && !envPass.isBlank()) {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = client.login(envEmail, envPass);
                DivoomCloudClient.saveSession(session);
                return session;
            }
        }

        // Try cached session
        DivoomCloudSession cached = DivoomCloudClient.loadCachedSession();
        if (cached != null) {
            return cached;
        }

        throw new IllegalArgumentException("No Divoom credentials found. Please run 'jixoo64 cloud login' or set DIVOOM_EMAIL and DIVOOM_PASSWORD.");
    }

    private static PixooAnimation loadAnimation(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".gif")) {
            return GifDecoder.decode(path);
        } else {
            return ImageProcessor.processImage(ImageProcessor.load(path));
        }
    }

    /**
     * Subcommand: cloud login
     */
    @Command(name = "login", description = "Log into your Divoom Cloud account and save session.")
    public static class LoginCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-e", "--email"}, required = true, description = "Divoom account email")
        private String email;

        @Option(names = {"-p", "--password"}, arity = "0..1", interactive = true, description = "Divoom account password")
        private String password;

        @Override
        public Integer call() {
            if (password == null || password.isBlank()) {
                System.err.println("Password cannot be empty.");
                return 1;
            }
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                System.out.printf("Authenticating with Divoom Cloud as %s...%n", email);
                DivoomCloudSession session = client.login(email, password);
                DivoomCloudClient.saveSession(session);
                System.out.println("Login successful!");
                System.out.printf("User ID: %d%n", session.userId());
                if (session.deviceId() != 0) {
                    System.out.printf("Default Device ID: %d%n", session.deviceId());
                }
                return 0;
            } catch (Exception e) {
                System.err.printf("Login error: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Subcommand: cloud logout
     */
    @Command(name = "logout", description = "Log out from Divoom Cloud and clear saved session.")
    public static class LogoutCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Override
        public Integer call() {
            DivoomCloudSession session = DivoomCloudClient.loadCachedSession();
            if (session == null) {
                System.out.println("No active Divoom Cloud session found.");
                return 0;
            }

            try (DivoomCloudClient client = new DivoomCloudClient()) {
                System.out.printf("Logging out user %d (%s)...%n", session.userId(), session.email());
                client.logout(session);
                System.out.println("Logged out successfully. Session cache cleared.");
                return 0;
            } catch (Exception e) {
                DivoomCloudClient.clearSession();
                System.out.println("Session cache cleared (server returned: " + e.getMessage() + ").");
                return 0;
            }
        }
    }

    /**
     * Subcommand: cloud devices
     */
    @Command(name = "devices", description = "List Divoom hardware devices bound to your account.")
    public static class DevicesCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-e", "--email"}, description = "Divoom account email (optional if logged in)")
        private String email;

        @Option(names = {"-p", "--password"}, description = "Divoom account password (optional if logged in)")
        private String password;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = resolveSession(email, password);
                DivoomDeviceListResponse response = client.getDeviceList(session);
                if (response.deviceList().isEmpty()) {
                    System.out.println("No bound Divoom devices found for this account.");
                } else {
                    System.out.println("Bound Divoom devices:");
                    for (DivoomDeviceListResponse.DeviceItem item : response.deviceList()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format(" - [%d] %s (IP: %s", item.deviceId(), item.deviceName(), item.devicePrivateIP()));
                        if (item.deviceSSID() != null && !item.deviceSSID().isBlank()) {
                            sb.append(", SSID: ").append(item.deviceSSID());
                        }
                        if (item.deviceVersion() != null && !item.deviceVersion().isBlank()) {
                            sb.append(", FW: ").append(item.deviceVersion());
                        }
                        sb.append(")");
                        System.out.println(sb.toString());
                    }
                }
                return 0;
            } catch (Exception e) {
                System.err.printf("Error fetching device list: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Subcommand: cloud channel (Persistent Custom Channel update)
     */
    @Command(
            name = "channel",
            description = "Manage your Pixoo 64's persistent Custom Channel playlists (slots 0, 1, or 2): upload, append, list, delete, or clean."
    )
    public static class CustomChannelCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-f", "--file"}, arity = "1..*", description = "Path(s) to local GIF or image file(s)")
        private List<String> filePaths;

        @Parameters(arity = "0..*", description = "Optional positional GIF or image file(s)")
        private List<String> positionalFiles;

        @Option(names = {"-s", "--slot"}, defaultValue = "0", description = "Custom Channel page slot (0, 1, or 2, default: 0)")
        private int slot;

        @Option(names = {"-i", "--index"}, description = "Custom ID / playlist position to start from (starts at 1; default: 1)")
        private Integer index;

        @Option(names = {"-a", "--append"}, description = "Append after the last existing item in the slot playlist")
        private boolean append;

        @Option(names = {"-l", "--list"}, description = "List items currently saved in the slot playlist")
        private boolean listOnly;

        @Option(names = {"--delete"}, description = "Delete an item at the specified index from the slot playlist")
        private Integer deleteIndex;

        @Option(names = {"--clean"}, description = "Clear all items from the slot playlist")
        private boolean cleanSlot;

        @Option(names = {"-d", "--device-id"}, defaultValue = "0", description = "Target Pixoo Device ID (default: auto-detected from session)")
        private long deviceId;

        @Option(names = {"-e", "--email"}, description = "Divoom account email (optional if logged in)")
        private String email;

        @Option(names = {"-p", "--password"}, description = "Divoom account password (optional if logged in)")
        private String password;

        @Option(names = {"--logout"}, description = "Log out after operation to avoid session conflict with mobile app")
        private boolean autoLogout;

        @Spec
        private CommandSpec spec;

        private PrintWriter out() {
            return spec != null ? spec.commandLine().getOut() : new PrintWriter(System.out, true);
        }

        private PrintWriter err() {
            return spec != null ? spec.commandLine().getErr() : new PrintWriter(System.err, true);
        }

        @Override
        public Integer call() {
            if (slot < 0 || slot > 2) {
                err().printf("Invalid slot index: %d. Custom channel slots must be 0, 1, or 2.%n", slot);
                return 1;
            }

            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = resolveSession(email, password);
                long targetDevice = deviceId != 0 ? deviceId : session.deviceId();
                if (targetDevice == 0) {
                    err().println("No Device ID specified and none found in account session. Use --device-id.");
                    return 1;
                }

                // Handle --list
                if (listOnly) {
                    out().printf("Fetching playlist for Device [%d] Custom Channel Slot %d...%n", targetDevice, slot);
                    DivoomCustomListResponse listResp = client.getCustomList(session, slot, targetDevice);
                    if (!listResp.isSuccess()) {
                        err().printf("Failed to get custom list: Code %d - %s%n", listResp.returnCode(), listResp.returnMessage());
                        return 1;
                    }
                    List<DivoomCustomListItem> items = listResp.customList();
                    if (items.isEmpty()) {
                        out().printf("Slot %d is currently empty (0 items).%n", slot);
                    } else {
                        out().printf("Slot %d contains %d item(s):%n", slot, items.size());
                        out().printf("  %-4s %-12s %s%n", "Pos", "CustomId", "FileId");
                        out().printf("  %-4s %-12s %s%n", "---", "--------", "------");
                        for (int i = 0; i < items.size(); i++) {
                            DivoomCustomListItem item = items.get(i);
                            out().printf("  #%-3d %-12d %s%n", i + 1, item.customId(), item.fileId());
                        }
                    }
                    if (autoLogout) {
                        out().println("Logging out from Divoom Cloud...");
                        client.logout(session);
                    }
                    return 0;
                }

                // Handle --clean
                if (cleanSlot) {
                    out().printf("Clearing all items from Device [%d] Custom Channel Slot %d...%n", targetDevice, slot);
                    DivoomApiResponse resp = client.cleanCustom(session, slot, targetDevice);
                    if (resp.isSuccess()) {
                        out().printf("SUCCESS! Slot %d cleared.%n", slot);
                        if (autoLogout) {
                            out().println("Logging out from Divoom Cloud...");
                            client.logout(session);
                        }
                        return 0;
                    } else {
                        err().printf("Failed to clean slot: Code %d - %s%n", resp.returnCode(), resp.returnMessage());
                        return 1;
                    }
                }

                // Handle --delete
                if (deleteIndex != null) {
                    if (deleteIndex < 1) {
                        err().printf("Invalid delete index: %d. Must be >= 1.%n", deleteIndex);
                        return 1;
                    }
                    int targetCustomId = deleteIndex;
                    // If deleteIndex looks like a 1-based position (<= 32), resolve it from the playlist
                    if (deleteIndex <= 32) {
                        DivoomCustomListResponse listResp = client.getCustomList(session, slot, targetDevice);
                        if (listResp.isSuccess() && !listResp.customList().isEmpty()) {
                            List<DivoomCustomListItem> items = listResp.customList();
                            if (deleteIndex <= items.size()) {
                                targetCustomId = items.get(deleteIndex - 1).customId();
                                out().printf("Position #%d resolves to CustomId %d.%n", deleteIndex, targetCustomId);
                            }
                        }
                    }

                    out().printf("Deleting item (CustomId %d) from Device [%d] Custom Channel Slot %d...%n", targetCustomId, targetDevice, slot);
                    DivoomApiResponse resp = client.deleteCustom(session, slot, targetCustomId, targetDevice);
                    if (resp.isSuccess()) {
                        out().printf("SUCCESS! Item deleted from Slot %d.%n", slot);
                        if (autoLogout) {
                            out().println("Logging out from Divoom Cloud...");
                            client.logout(session);
                        }
                        return 0;
                    } else {
                        err().printf("Failed to delete item: Code %d - %s%n", resp.returnCode(), resp.returnMessage());
                        return 1;
                    }
                }

                // Handle uploading file(s)
                List<String> allFiles = new ArrayList<>();
                if (filePaths != null) {
                    allFiles.addAll(filePaths);
                }
                if (positionalFiles != null) {
                    allFiles.addAll(positionalFiles);
                }

                if (allFiles.isEmpty()) {
                    err().println("No file(s) specified. Provide file path(s), or use --list, --delete, or --clean.");
                    return 1;
                }

                // Validate file paths
                for (String file : allFiles) {
                    Path p = Paths.get(file);
                    if (!Files.exists(p) || !Files.isRegularFile(p)) {
                        err().printf("File not found: %s%n", file);
                        return 1;
                    }
                }

                // Query existing playlist for position resolution if index is specified
                List<DivoomCustomListItem> existingItems = new ArrayList<>();
                if (index != null) {
                    DivoomCustomListResponse listResp = client.getCustomList(session, slot, targetDevice);
                    if (listResp.isSuccess()) {
                        existingItems.addAll(listResp.customList());
                    }
                }

                int successCount = 0;
                for (int i = 0; i < allFiles.size(); i++) {
                    Path p = Paths.get(allFiles.get(i));

                    // Determine target CustomId (0 to insert new, or existing item's CustomId to overwrite in place)
                    int targetCustomId = 0;
                    if (index != null && !append) {
                        int pos = index + i;
                        if (pos >= 1 && pos <= existingItems.size()) {
                            targetCustomId = existingItems.get(pos - 1).customId();
                            out().printf("%n[%d/%d] Processing %s (overwriting position #%d, CustomId %d in Slot %d)...%n",
                                    i + 1, allFiles.size(), p.getFileName(), pos, targetCustomId, slot);
                        } else {
                            out().printf("%n[%d/%d] Processing %s (inserting at position #%d in Slot %d)...%n",
                                    i + 1, allFiles.size(), p.getFileName(), pos, slot);
                        }
                    } else {
                        out().printf("%n[%d/%d] Processing %s (appending to Slot %d)...%n",
                                i + 1, allFiles.size(), p.getFileName(), slot);
                    }

                    PixooAnimation animation = loadAnimation(p);
                    out().printf("   Encoding %d frame(s) into Divoom 64x64 binary format...%n", animation.frameCount());
                    byte[] binaryData = DivoomAnimationEncoder.encode(animation);
                    out().printf("   Encoded payload size: %d bytes%n", binaryData.length);

                    out().println("   Uploading binary asset to Divoom Cloud CDN...");
                    String fileId = client.uploadPicture(session, binaryData);
                    out().printf("   Assigned File ID: %s%n", fileId);

                    out().printf("   Assigning to Device [%d] Slot %d...%n", targetDevice, slot);
                    DivoomApiResponse resp = client.setCustomChannel(session, slot, targetCustomId, fileId, targetDevice);

                    if (resp.isSuccess()) {
                        out().printf("   SUCCESS! Assigned to Slot %d.%n", slot);
                        successCount++;
                    } else {
                        err().printf("   Failed to assign: Code %d - %s%n", resp.returnCode(), resp.returnMessage());
                    }
                }

                out().printf("%nDone! Successfully updated %d/%d animation(s) in Slot %d.%n", successCount, allFiles.size(), slot);

                if (autoLogout) {
                    out().println("Logging out from Divoom Cloud...");
                    client.logout(session);
                }

                return successCount == allFiles.size() ? 0 : 1;
            } catch (Exception e) {
                err().printf("Error updating Custom Channel: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Subcommand: cloud gallery
     */
    @Command(name = "gallery", description = "Upload a GIF/image to your Divoom Cloud user gallery.")
    public static class GalleryCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-f", "--file"}, required = true, description = "Path to local GIF or image file")
        private String filePath;

        @Option(names = {"-n", "--name"}, required = true, description = "Title of the artwork")
        private String name;

        @Option(names = {"--desc"}, description = "Description of the artwork")
        private String description;

        @Option(names = {"--private"}, description = "Mark as private (visible only to you)")
        private boolean isPrivate;

        @Option(names = {"-d", "--device-id"}, defaultValue = "0", description = "Target Pixoo Device ID (default: auto-detected from session)")
        private long deviceId;

        @Option(names = {"-e", "--email"}, description = "Divoom account email (optional if logged in)")
        private String email;

        @Option(names = {"-p", "--password"}, description = "Divoom account password (optional if logged in)")
        private String password;

        @Option(names = {"--logout"}, description = "Log out after operation to avoid session conflict with mobile app")
        private boolean autoLogout;

        @Override
        public Integer call() {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                System.err.printf("File not found: %s%n", filePath);
                return 1;
            }

            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = resolveSession(email, password);
                long targetDevice = deviceId != 0 ? deviceId : session.deviceId();

                System.out.printf("1. Loading and converting %s...%n", path.getFileName());
                PixooAnimation animation = loadAnimation(path);

                System.out.printf("2. Encoding %d frame(s) into Divoom 64x64 binary format...%n", animation.frameCount());
                byte[] binaryData = DivoomAnimationEncoder.encode(animation);
                System.out.printf("   Encoded payload size: %d bytes%n", binaryData.length);

                System.out.printf("3. Publishing \"%s\" to your Divoom Cloud gallery (%s)...%n", name, isPrivate ? "private" : "public");
                DivoomApiResponse response = client.uploadToGallery(session, binaryData, name, description, isPrivate, targetDevice);

                if (response.isSuccess()) {
                    System.out.printf("SUCCESS! \"%s\" has been published to your Divoom Cloud gallery.%n", name);
                    if (response.galleryId() != 0) {
                        System.out.printf("Gallery ID: %d%n", response.galleryId());
                    }
                    if (response.pixelFileId() != null) {
                        System.out.printf("File ID: %s%n", response.pixelFileId());
                    }
                    if (autoLogout) {
                        System.out.println("Logging out from Divoom Cloud...");
                        client.logout(session);
                    }
                    return 0;
                } else {
                    System.err.printf("Failed to publish to gallery: Code %d - %s%n", response.returnCode(), response.returnMessage());
                    return 1;
                }
            } catch (Exception e) {
                System.err.printf("Error publishing to gallery: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    private static void printGalleryItems(String title, List<CloudGalleryItem> items) {
        System.out.println(title + ":");
        if (items == null || items.isEmpty()) {
            System.out.println("  (No artworks found)");
            return;
        }
        System.out.println("GalleryId | FileId                          | Artist (ID)        | Likes | Name");
        System.out.println("----------+---------------------------------+--------------------+-------+-------------------");
        for (CloudGalleryItem item : items) {
            String artistStr = String.format("%s (%d)",
                    item.userName() != null ? item.userName() : "Anon",
                    item.userId());
            System.out.printf("%-9d | %-31s | %-18s | %5d | %s%n",
                    item.galleryId(),
                    item.fileId() != null ? item.fileId() : "",
                    artistStr.length() > 18 ? artistStr.substring(0, 18) : artistStr,
                    item.likeCount(),
                    item.fileName() != null ? item.fileName() : "");
        }
    }

    @Command(name = "browse", description = "Browse curated 64x64 animations in the Divoom public gallery.")
    public static class BrowseCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-c", "--category", "--classify"}, defaultValue = "0", description = "Category ID (0=All, 1=Pixel, etc.)")
        private int category;

        @Option(names = {"-s", "--sort"}, defaultValue = "POPULAR", description = "Sort order: POPULAR or NEWEST")
        private GallerySort sort;

        @Option(names = {"--start"}, defaultValue = "0", description = "Starting index (default: 0)")
        private int start;

        @Option(names = {"--end"}, defaultValue = "19", description = "Ending index (default: 19)")
        private int end;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                CloudGalleryResponse resp = client.browseGallery(category, sort, start, end);
                printGalleryItems("Divoom Public Gallery (" + sort + ")", resp.getItems());
                return 0;
            } catch (Exception e) {
                System.err.printf("Error browsing gallery: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "search", description = "Search animations in the Divoom public gallery by keyword.")
    public static class SearchCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Parameters(index = "0", description = "Search keyword (e.g. 'mario', 'space', 'cyberpunk')")
        private String keyword;

        @Option(names = {"--start"}, defaultValue = "0", description = "Starting index (default: 0)")
        private int start;

        @Option(names = {"--end"}, defaultValue = "19", description = "Ending index (default: 19)")
        private int end;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                CloudGalleryResponse resp = client.searchGallery(keyword, start, end);
                printGalleryItems("Search results for '" + keyword + "'", resp.getItems());
                return 0;
            } catch (Exception e) {
                System.err.printf("Error searching gallery: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "artist", description = "View artist/creator profile and public artworks.")
    public static class ArtistCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Parameters(index = "0", description = "Artist User ID")
        private long artistId;

        @Option(names = {"--start"}, defaultValue = "0", description = "Starting artwork index (default: 0)")
        private int start;

        @Option(names = {"--end"}, defaultValue = "19", description = "Ending artwork index (default: 19)")
        private int end;

        @Option(names = {"--all-sizes"}, description = "Include all matrix sizes instead of 64x64 only (default: false)")
        private boolean allSizes;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                ArtistProfile profile = client.getArtistProfile(artistId);
                long displayId = profile.userId() != 0 ? profile.userId() : artistId;
                System.out.printf("Artist: %s (ID: %d)%n", profile.userName() != null ? profile.userName() : "Unknown", displayId);
                if (profile.bio() != null && !profile.bio().isBlank()) {
                    System.out.printf("  Bio:       %s%n", profile.bio());
                }
                System.out.printf("  Followers: %d | Following: %d%n", profile.followerCount(), profile.followingCount());
                System.out.println();

                int fileSize = allSizes ? 127 : 4;
                CloudGalleryResponse resp = client.getArtistArtworks(artistId, fileSize, start, end);
                printGalleryItems("Artworks by " + (profile.userName() != null ? profile.userName() : "Artist " + artistId), resp.getItems());
                return 0;
            } catch (Exception e) {
                System.err.printf("Error fetching artist profile: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "uploads", description = "List your uploaded pixel artworks in Divoom Cloud.")
    public static class UploadsCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-e", "--email"}, description = "Divoom account email")
        private String email;

        @Option(names = {"-p", "--password"}, description = "Divoom account password")
        private String password;

        @Option(names = {"--start"}, defaultValue = "0", description = "Starting index (default: 0)")
        private int start;

        @Option(names = {"--end"}, defaultValue = "19", description = "Ending index (default: 19)")
        private int end;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = resolveSession(email, password);
                CloudGalleryResponse resp = client.getMyUploads(session, start, end);
                printGalleryItems("Your Uploaded Artworks", resp.getItems());
                return 0;
            } catch (Exception e) {
                System.err.printf("Error fetching your uploads: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "likes", description = "List your favorited pixel artworks in Divoom Cloud.")
    public static class LikesCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"-e", "--email"}, description = "Divoom account email")
        private String email;

        @Option(names = {"-p", "--password"}, description = "Divoom account password")
        private String password;

        @Option(names = {"--start"}, defaultValue = "0", description = "Starting index (default: 0)")
        private int start;

        @Option(names = {"--end"}, defaultValue = "19", description = "Ending index (default: 19)")
        private int end;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                DivoomCloudSession session = resolveSession(email, password);
                CloudGalleryResponse resp = client.getMyLikes(session, start, end);
                printGalleryItems("Your Favorited Artworks", resp.getItems());
                return 0;
            } catch (Exception e) {
                System.err.printf("Error fetching your likes: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "download", description = "Download an artwork asset from Divoom Cloud CDN by FileId and convert to animated GIF.")
    public static class DownloadCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Parameters(index = "0", description = "File ID (e.g. group1/M00/... or full URL)")
        private String fileId;

        @Option(names = {"-o", "--output"}, description = "Output file path (default: <filename>.gif)")
        private String outputPath;

        @Option(names = {"--raw"}, description = "Save raw Divoom binary asset without converting to GIF")
        private boolean raw;

        @Override
        public Integer call() {
            try (DivoomCloudClient client = new DivoomCloudClient()) {
                System.out.printf("Downloading asset %s...%n", fileId);
                byte[] data = client.downloadAsset(fileId);

                String cleanName = fileId.contains("/") ? fileId.substring(fileId.lastIndexOf('/') + 1) : fileId;
                boolean isRawRequested = raw || (outputPath != null && outputPath.toLowerCase().endsWith(".bin"));

                Path outPath;
                if (isRawRequested) {
                    if (outputPath != null && !outputPath.isBlank()) {
                        outPath = Paths.get(outputPath);
                    } else {
                        if (!cleanName.toLowerCase().endsWith(".bin")) cleanName += ".bin";
                        outPath = Paths.get(cleanName);
                    }
                    Files.write(outPath, data);
                    System.out.printf("Raw Divoom asset saved (%d bytes) to %s.%n", data.length, outPath.toAbsolutePath());
                    return 0;
                }

                // Check if already a standard GIF
                if (data.length > 6 && (data[0] == 'G' && data[1] == 'I' && data[2] == 'F')) {
                    if (outputPath != null && !outputPath.isBlank()) {
                        outPath = Paths.get(outputPath);
                    } else {
                        if (!cleanName.toLowerCase().endsWith(".gif")) cleanName += ".gif";
                        outPath = Paths.get(cleanName);
                    }
                    Files.write(outPath, data);
                    System.out.printf("GIF asset saved (%d bytes) to %s.%n", data.length, outPath.toAbsolutePath());
                    return 0;
                }

                // Decode Divoom binary asset and convert to GIF
                try {
                    PixooAnimation animation = DivoomAssetDecoder.decode(data);
                    byte[] gifBytes = GifEncoder.encode(animation);

                    if (outputPath != null && !outputPath.isBlank()) {
                        outPath = Paths.get(outputPath);
                    } else {
                        if (!cleanName.toLowerCase().endsWith(".gif")) cleanName += ".gif";
                        outPath = Paths.get(cleanName);
                    }
                    Files.write(outPath, gifBytes);
                    System.out.printf("Successfully converted and saved animated GIF (%d frames, %d bytes) to %s.%n",
                            animation.frameCount(), gifBytes.length, outPath.toAbsolutePath());
                    return 0;
                } catch (Exception decodeEx) {
                    System.err.printf("Warning: Could not decode as Divoom binary (%s). Saving raw asset instead.%n", decodeEx.getMessage());
                    if (outputPath != null && !outputPath.isBlank()) {
                        outPath = Paths.get(outputPath);
                    } else {
                        if (!cleanName.toLowerCase().endsWith(".bin")) cleanName += ".bin";
                        outPath = Paths.get(cleanName);
                    }
                    Files.write(outPath, data);
                    System.out.printf("Raw asset saved (%d bytes) to %s.%n", data.length, outPath.toAbsolutePath());
                    return 0;
                }
            } catch (Exception e) {
                System.err.printf("Failed to download asset: %s%n", e.getMessage());
                return 1;
            }
        }
    }


    @Command(name = "play", description = "Download a cloud artwork by FileId and stream it directly to your Pixoo display.")
    public static class PlayCommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private CloudCommand parent;

        @Parameters(index = "0", description = "Cloud File ID (e.g. group1/M00/... or full URL)")
        private String fileId;

        @Override
        public Integer call() {
            try (DivoomCloudClient cloud = new DivoomCloudClient()) {
                System.out.printf("1. Downloading cloud asset '%s'...%n", fileId);
                byte[] binaryData = cloud.downloadAsset(fileId);
                System.out.printf("   Downloaded %d bytes.%n", binaryData.length);

                System.out.println("2. Decoding Divoom 64x64 binary format in pure Java...");
                PixooAnimation animation = DivoomAssetDecoder.decode(binaryData);
                System.out.printf("   Successfully decoded %d frame(s) (delay %d ms).%n",
                        animation.frameCount(), animation.frames().isEmpty() ? 0 : animation.frames().get(0).delayMs());

                System.out.println("3. Streaming animation to Pixoo 64 display...");
                PixooClient pixoo = parent.cliParent != null ? parent.cliParent.createClient() : PixooClient.builder().ipAddress("127.0.0.1").build();
                PixooResponse resp = pixoo.sendAnimation(animation);
                if (resp.isSuccess()) {
                    System.out.println("SUCCESS! Playing animation on Pixoo display.");
                    return 0;
                } else {
                    System.err.printf("Device error while sending animation: code %d%n", resp.errorCode());
                    return 1;
                }
            } catch (Exception e) {
                System.err.printf("Error playing cloud asset: %s%n", e.getMessage());
                return 1;
            }
        }
    }
}

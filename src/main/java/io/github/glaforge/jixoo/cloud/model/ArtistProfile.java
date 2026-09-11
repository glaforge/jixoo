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
package io.github.glaforge.jixoo.cloud.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public profile details of a Divoom pixel artist / creator.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtistProfile(
        @JsonProperty("ReturnCode") int returnCode,
        @JsonProperty("ReturnMessage") String returnMessage,
        @JsonAlias({"UserId", "SomeOneUserId"}) @JsonProperty("UserId") long userId,
        @JsonAlias({"NickName", "UserName"}) @JsonProperty("UserName") String userName,
        @JsonAlias({"HeadId", "UserHeaderId"}) @JsonProperty("UserHeaderId") String avatarFileId,
        @JsonAlias({"UserNewSign", "Introduce"}) @JsonProperty("Introduce") String bio,
        @JsonAlias({"FansCnt", "FanCnt"}) @JsonProperty("FanCnt") int followerCount,
        @JsonProperty("FollowCnt") int followingCount,
        @JsonProperty("Level") int level,
        @JsonProperty("CountryISOCode") String countryIsoCode,
        @JsonProperty("WebUrl") String webUrl
) {
    public boolean isSuccess() {
        return returnCode == 0;
    }
}

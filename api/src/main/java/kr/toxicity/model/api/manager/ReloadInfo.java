package kr.toxicity.model.api.manager;

import lombok.Builder;

@Builder
public record ReloadInfo(boolean firstReload) {
    public static ReloadInfo DEFAULT = ReloadInfo.builder()
            .firstReload(false)
            .build();
}

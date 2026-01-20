package com.metacraft.api.modules.ai.prompt;

public final class ImagePrompts {
    public static String buildLogoPrompt(String appName, String appDescription) {
        return String.format(
                "Create a modern, professional app logo for '%s'. " +
                "The app is described as: %s. " +
                "The logo should be simple, memorable, and suitable for mobile app icons. " +
                "Use a clean design with appropriate colors that reflect the app's purpose. " +
                "The logo should work well at small sizes and be visually distinctive.",
                appName,
                appDescription
        );
    }

    private ImagePrompts() {}
}

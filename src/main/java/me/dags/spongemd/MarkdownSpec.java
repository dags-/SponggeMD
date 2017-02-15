package me.dags.spongemd;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextParseException;
import org.spongepowered.api.text.serializer.TextSerializer;

import java.util.HashSet;
import java.util.Set;

/**
 * @author dags <dags@dags.me>
 */
public final class MarkdownSpec implements TextSerializer {

    private static final MarkdownPerms DEFAULTS = MarkdownPerms.builder().build();

    private final boolean url;
    private final boolean reset;
    private final boolean showText;
    private final boolean insertText;
    private final boolean runCommand;
    private final boolean suggestCommand;
    private final Set<TextColor> colors;
    private final Set<TextStyle> styles;

    private MarkdownSpec(Builder builder) {
        this.url = builder.openUrl;
        this.reset = builder.reset;
        this.showText = builder.showText;
        this.insertText = builder.insertText;
        this.runCommand = builder.runCommand;
        this.suggestCommand = builder.suggestCommand;
        this.colors = ImmutableSet.copyOf(builder.colors);
        this.styles = ImmutableSet.copyOf(builder.styles);
    }

    public MarkdownTemplate template(String input) {
        return new MarkdownTemplate(this, input);
    }

    /**
     * Render a SpongeMD string to a formatted Text
     *
     * @param input The input String written in the Markdown-like notation
     * @return A formatted Text object including Colors, Styles, Actions enabled in the Spec
     */
    public Text render(String input) {
        return new MDParser(this, input).parse();
    }

    /**
     * Write a given Text object to the Markdown-like notation
     *
     * @param text The formatted Text object
     * @return A String representation of the given Text object written in the Markdown-like notation
     */
    public String write(TextRepresentable text) {
        return new MDWriter(this, true).write(text.toText());
    }

    /**
     * Write a given Text object to the Markdown-like notation without escaping Markdown-like notation
     *
     * @param text The formatted Text object
     * @return A String representation of the given Text object written in the Markdown-like notation
     */
    public String writeUnescaped(TextRepresentable text) {
        return new MDWriter(this, false).write(text.toText());
    }

    /**
     * Write a given Text object to the Markdown-like notation
     *
     * @param text The formatted Text object
     * @return A String representation of the given Text object written in the Markdown-like notation
     */
    @Override
    public String serialize(Text text) {
        return write(text);
    }

    /**
     * Render a given String into a formatted Text object
     *
     * @param input The input String written in the Markdown-like notation
     * @return A formatted Text object including Colors, Styles, Actions enabled in the Spec
     */
    @Override
    public Text deserialize(String input) throws TextParseException {
        return render(input);
    }

    boolean allow(TextColor color) {
        return colors.contains(color);
    }

    boolean allow(TextStyle style) {
        return styles.contains(style);
    }

    boolean allowUrl() {
        return url;
    }

    boolean allowReset() {
        return reset;
    }

    boolean allowShowText() {
        return showText;
    }

    boolean allowInsertText() {
        return insertText;
    }

    boolean allowRunCommand() {
        return runCommand;
    }

    boolean allowSuggestCommand() {
        return suggestCommand;
    }

    /**
     * Create a new MarkdownSpec with all formatting options enabled
     *
     * @return The newly created MarkdownSpec
     */
    public static MarkdownSpec create() {
        return new Builder().allColors().allStyles().allActions().build();
    }

    /**
     * Create a new MarkdownSpec based on the permissions of a given Subject using the default markdown permissions
     * Formatting options will be skipped where the Subject does not have permission to use them
     *
     * @param subject The Subject to test for formatting permissions
     * @return The newly created MarkdownSpec
     */
    public static MarkdownSpec create(Subject subject) {
        return create(subject, DEFAULTS);
    }

    /**
     * Create a new MarkdownSpec based on the permissions of a given Subject using the provided custom permissions
     * Formatting options will be skipped where the Subject does not have permission to use them
     *
     * @param subject The Subject to test for formatting permissions
     * @param perms The MarkdownPerms to test the Subject for
     * @return The newly created MarkdownSpec
     */
    public static MarkdownSpec create(Subject subject, MarkdownPerms perms) {
        Builder builder = new Builder();

        MDParam.colors().values().stream()
                .filter(color -> subject.hasPermission(perms.getColorNode(color)))
                .forEach(builder::allow);

        MDParam.styles().values().stream()
                .filter(style -> subject.hasPermission(perms.getStyleNode(style)))
                .forEach(builder::allow);

        builder.suggestCommand = subject.hasPermission(perms.getSuggestCommand());
        builder.runCommand = subject.hasPermission(perms.getRunCommand());
        builder.insertText = subject.hasPermission(perms.getInsertText());
        builder.showText = subject.hasPermission(perms.getShowText());
        builder.openUrl = subject.hasPermission(perms.getOpenUrl());

        // Accept any form of 'reset'
        builder.reset = subject.hasPermission(perms.getReset())
                || subject.hasPermission(perms.getColorNode(TextColors.RESET))
                || subject.hasPermission(perms.getStyleNode(TextStyles.RESET));

        return new MarkdownSpec(builder);
    }

    public static MarkdownSpec.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<TextColor> colors = new HashSet<>();
        private final Set<TextStyle> styles = new HashSet<>();
        private boolean reset = false;
        private boolean openUrl = false;
        private boolean showText = false;
        private boolean insertText = false;
        private boolean runCommand = false;
        private boolean suggestCommand = false;

        private Builder(){}

        /**
         * Enable all TextColors
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder allColors() {
            MDParam.colors().values().forEach(colors::add);
            return this;
        }

        /**
         * Enable all TextStyles
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder allStyles() {
            MDParam.styles().values().forEach(styles::add);
            return this;
        }

        /**
         * Enable all Click/Shift/Hover actions
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder allActions() {
            this.reset = true;
            this.openUrl = true;
            this.showText = true;
            this.insertText = true;
            this.runCommand = true;
            this.suggestCommand = true;
            return this;
        }

        /**
         * Enable the use of the given TextColor
         *
         * @param color The TextColor to enable
         * @return The current MarkdownSpec.Builder
         */
        public Builder allow(TextColor color) {
            colors.add(color);
            return this;
        }

        /**
         * Enable the use of the given TextStyle
         *
         * @param style The TextStyle to enable
         * @return The current MarkdownSpec.Builder
         */
        public Builder allow(TextStyle.Base style) {
            styles.add(style);
            return this;
        }

        /**
         * Enable the use of the 'Open Url' TextAction
         * (Prompts the user to open a URL in their web browser)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder url() {
            this.openUrl = true;
            return this;
        }

        /**
         * Enable the use of the 'Reset' TextFormat
         * (Clears color/style formatting)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder reset() {
            this.reset = true;
            return this;
        }

        /**
         * Enabled the use of the 'Show Text' TextAction
         * (Shows a 'hovering' text box when the text s moused over)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder showText() {
            this.showText = true;
            return this;
        }

        /**
         * Enable the use of the 'Insert Text' TextAction
         * (Inserts text to into the user's chat input box at the current cursor position when shift-clicked)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder insertText() {
            this.showText = true;
            return this;
        }

        /**
         * Enable the use of the 'Run Command' TextAction
         * (Executes the given command as the User when clicked)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder runCommand() {
            this.runCommand = true;
            return this;
        }

        /**
         * Enabled the use of the 'Suggest Command' TextAction
         * (Inserts the given command into the user's chat input box when clicked)
         *
         * @return The current MarkdownSpec.Builder
         */
        public Builder suggestCommand() {
            this.suggestCommand = true;
            return this;
        }

        public MarkdownSpec build() {
            return new MarkdownSpec(this);
        }
    }
}

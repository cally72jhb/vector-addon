package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.events.SendRawMessageEvent;
import cally72jhb.addon.utils.cipher.EncryptUtils;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.*;

public class ChatEncryption extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How this module is used.")
        .defaultValue(Mode.Both)
        .build()
    );

    private final Setting<Algorithm> algorithm = sgGeneral.add(new EnumSetting.Builder<Algorithm>()
        .name("algorithm-mode")
        .description("What algorithm to use.")
        .defaultValue(Algorithm.AES)
        .build()
    );

    private final Setting<DisplayMode> displayMode = sgGeneral.add(new EnumSetting.Builder<DisplayMode>()
        .name("display-mode")
        .description("How to display the decrypted messages.")
        .defaultValue(DisplayMode.Normal)
        .build()
    );

    private final Setting<String> key = sgGeneral.add(new StringSetting.Builder()
        .name("key")
        .description("What key is used to encrypt and decrypt messages.")
        .defaultValue("vector")
        .build()
    );

    private final Setting<String> secretKey = sgGeneral.add(new StringSetting.Builder()
        .name("secret-key")
        .description("A secondary key to ensure safety.")
        .defaultValue(":>")
        .build()
    );

    private final Setting<String> encryptionPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("encryption-prefix")
        .description("What is used as prefix to encrypt messages.")
        .defaultValue("!enc:")
        .build()
    );

    private final Setting<String> decryptionPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("decryption-prefix")
        .description("What is used as prefix to decrypt messages.")
        .defaultValue("!dnc:")
        .build()
    );

    private final Setting<String> cipherSuffix = sgGeneral.add(new StringSetting.Builder()
        .name("cipher-suffix")
        .description("What suffix to use to end your messages.")
        .defaultValue(";")
        .build()
    );

    private final Setting<Boolean> encryptionDebug = sgGeneral.add(new BoolSetting.Builder()
        .name("encryption-debug")
        .description("Debugs the encryption process.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> decryptionDebug = sgGeneral.add(new BoolSetting.Builder()
        .name("decryption-debug")
        .description("Debugs the decryption process.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> warnings = sgGeneral.add(new BoolSetting.Builder()
        .name("warnings")
        .description("Prints out warnings if failing cipher.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderOriginal = sgGeneral.add(new BoolSetting.Builder()
        .name("render-original")
        .description("Renders the original message when hovering over the decrypted suffix.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderCopy = sgGeneral.add(new BoolSetting.Builder()
        .name("render-copy-button")
        .description("Renders a button to copy the original message.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> decryptedColor = sgGeneral.add(new ColorSetting.Builder()
        .name("decrypted-color")
        .description("The color of the decrypted suffix.")
        .defaultValue(new SettingColor(255, 170, 0, 255, false))
        .build()
    );

    private final Setting<SettingColor> copyColor = sgGeneral.add(new ColorSetting.Builder()
        .name("copy-button-color")
        .description("The color of the copy button.")
        .defaultValue(new SettingColor(255, 170, 0, 255, false))
        .visible(renderCopy::get)
        .build()
    );

    public ChatEncryption() {
        super(Categories.Misc, "chat-encryption", "Encrypts your chat messages to make them unreadable to other people.");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        if (mode.get() == Mode.Encrypt || mode.get() == Mode.Both) event.message = encrypt(event.message);
    }

    @EventHandler
    private void onSendRawMessage(SendRawMessageEvent event) {
        if (mode.get() == Mode.Encrypt || mode.get() == Mode.Both) event.message = encrypt(event.message);
    }

    private String encrypt(String string) {
        if (string.contains(encryptionPrefix.get()) && string.contains(cipherSuffix.get()) && string.indexOf(encryptionPrefix.get()) < string.indexOf(cipherSuffix.get(), string.indexOf(encryptionPrefix.get()))) {
            try {
                int index;

                while ((index = string.indexOf(encryptionPrefix.get())) != -1) {
                    String toEncrypt = string.substring(index + encryptionPrefix.get().length(), string.indexOf(cipherSuffix.get(), index));
                    String encrypted;

                    if (encryptionDebug.get()) {
                        info("key: [ " + getKey() + " ] | validated: [ " + validate(getKey()) + " ]");
                        info("key: [ " + secretKey.get() + " ] | validated: [ " + validate(secretKey.get()) + " ]");
                        info("algorithm: [ " + getAlgorithm() + " ] | size: [ " + getMaxKeySize(algorithm.get()) + " ]");
                        info("message: [ " + toEncrypt + " ]");
                    }

                    try {
                        encrypted = EncryptUtils.encrypt(toEncrypt.concat(validate(secretKey.get())), getKey(), getAlgorithm());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        if (warnings.get()) warning("Encryption failed: " + exception + " Exception.");

                        return string;
                    }

                    string = string.substring(0, index).concat(string.substring(index).replaceFirst(encryptionPrefix.get() + toEncrypt + cipherSuffix.get(), getDecryptionPrefix() + encrypted + cipherSuffix.get()));
                }

                if (string.length() > 256) {
                    if (warnings.get()) warning("The encrypted string is above 256 characters. Unable to encrypt.");

                    return string;
                }

                return string;
            } catch (Exception exception) {
                exception.printStackTrace();
                if (warnings.get()) warning("Encryption failed: Invalid format caused a " + exception + " Exception.");

                return string;
            }
        }

        return string;
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (mode.get() == Mode.Decrypt || mode.get() == Mode.Both) {
            StringBuilder builder = new StringBuilder();

            event.getMessage().asOrderedText().accept((i, style, codePoint) -> {
                builder.append(new String(Character.toChars(codePoint)));
                return true;
            });

            String string = builder.toString();
            String originalString = builder.toString();
            Text original = event.getMessage();

            if (string.contains(getDecryptionPrefix()) && string.contains(cipherSuffix.get()) && string.indexOf(getDecryptionPrefix()) < string.indexOf(cipherSuffix.get(), string.indexOf(getDecryptionPrefix()))) {
                try {
                    int index;

                    while ((index = string.indexOf(getDecryptionPrefix())) != -1) {
                        String toDecrypt = string.substring(string.indexOf(getDecryptionPrefix(), index) + getDecryptionPrefix().length(), string.indexOf(cipherSuffix.get(), index));
                        String decrypted;

                        if (decryptionDebug.get()) {
                            info("key: [ " + getKey() + " ] | validated: [ " + validate(getKey()) + " ]");
                            info("key: [ " + secretKey.get() + " ] | validated: [ " + validate(secretKey.get()) + " ]");
                            info("algorithm: [ " + getAlgorithm() + " ] | size: [ " + getMaxKeySize(algorithm.get()) + " ]");
                            info("message: [ " + toDecrypt + " ]");
                        }

                        try {
                            decrypted = EncryptUtils.decrypt(toDecrypt, getKey(), getAlgorithm());

                            if (!decrypted.endsWith(validate(secretKey.get()))) return;

                            decrypted = decrypted.substring(0, decrypted.length() - validate(secretKey.get()).length());
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            if (warnings.get()) warning("Decryption failed: " + exception + " Exception.");

                            return;
                        }

                        string = string.substring(0, index).concat(string.substring(index).replace(getDecryptionPrefix() + toDecrypt + cipherSuffix.get(), decrypted));
                    }

                    BaseText copy = new LiteralText("[Copy]");
                    copy.setStyle(copy.getStyle()
                        .withColor(copyColor.get().copy().a(255).getPacked())
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, originalString))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new LiteralText("Copy the original message.")
                        ))
                    );

                    if (displayMode.get() == DisplayMode.Normal) {
                        MutableText text = new LiteralText(string + " ").append(new LiteralText("[Decrypted]")
                            .setStyle(Style.EMPTY.withColor(decryptedColor.get().copy().a(255).getPacked())
                                .withHoverEvent(renderOriginal.get() ? new HoverEvent(HoverEvent.Action.SHOW_TEXT, original) : null)
                            )
                            .append(renderCopy.get() ? new LiteralText(" ").append(copy) : new LiteralText(""))
                        );

                        event.setMessage(text);
                    } else {
                        MutableText buttons = new LiteralText("[Decrypted]")
                            .setStyle(Style.EMPTY.withColor(decryptedColor.get().copy().a(255).getPacked())
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(string)))
                            )
                            .append(renderCopy.get() ? new LiteralText(" ").append(copy) : new LiteralText("")
                        );

                        event.setMessage(original.shallowCopy().append(" ").append(buttons));
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    if (warnings.get())
                        warning("Decryption failed: Invalid format caused a " + exception + " Exception");
                }
            }
        }
    }

    // Utils

    private String getAlgorithm() {
        return switch (algorithm.get()) {
            case AES -> "AES";
            case RC4 -> "RC4";
            case Blowfish -> "Blowfish";
        };
    }

    private int getMaxKeySize(Algorithm algorithm) {
        return switch (algorithm) {
            case AES -> 256;
            case RC4 -> 1024;
            case Blowfish -> 448;
        };
    }

    private String getDecryptionPrefix() {
        return encryptionPrefix.get().equals(decryptionPrefix.get()) ? decryptionPrefix.get() + ":" : decryptionPrefix.get();
    }

    private String getKey() {
        String finalKey = validate(key.get());
        if (finalKey.length() > getMaxKeySize(algorithm.get())) finalKey = finalKey.substring(0, getMaxKeySize(algorithm.get()));
        return finalKey;
    }

    private String validate(String string) {
        if (string.isEmpty()) return "vector";

        String valid = "";

        for (char character : string.toCharArray()) if (isCharacterValid(character)) valid += character;

        return valid.isEmpty() ? "vector" : valid;
    }

    private boolean isCharacterValid(char character) {
        return character == '_' || character == '-' || character == '<' || character == '>' || character == ':' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9';
    }

    // Constants

    public enum Mode {
        Encrypt,
        Decrypt,
        Both
    }

    public enum DisplayMode {
        Normal,
        Hide
    }

    public enum Algorithm {
        AES,
        RC4,
        Blowfish
    }
}

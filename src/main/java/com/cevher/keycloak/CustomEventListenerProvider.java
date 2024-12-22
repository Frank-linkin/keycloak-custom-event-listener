package com.cevher.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CustomEventListenerProvider
        implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {

        log.debugf("New %s Event", event.getType());
        log.debugf("onEvent-> %s", toString(event));

        if (EventType.REGISTER.equals(event.getType())) {
            log.debugf("Registration");

            event.getDetails().forEach((key, value) -> log.debugf("%s : %s", key, value));

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel user = this.session.users().getUserById(realm, event.getUserId());
            sendUserData(user, "user-registration");
        }

        if (EventType.UPDATE_PROFILE.equals(event.getType())) {
            log.debugf("UPDATE_PROFILE");

            event.getDetails().forEach((key, value) -> log.debugf("%s : %s", key, value));

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel user = this.session.users().getUserById(realm, event.getUserId());
            sendUserData(user, "user-update");
        }

        if (EventType.DELETE_ACCOUNT.equals(event.getType())) {

            log.debugf("DELETE_ACCOUNT");

            event.getDetails().forEach((key, value) -> log.debugf("%s : %s", key, value));

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel user = this.session.users().getUserById(realm, event.getUserId());
            sendUserData(user, "user-delete");
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        log.debug("onEvent(AdminEvent)");
        log.debugf("Resource path: %s", adminEvent.getResourcePath());
        log.debugf("Resource type: %s", adminEvent.getResourceType());
        log.debugf("Operation type: %s", adminEvent.getOperationType());
        log.debugf("AdminEvent.toString(): %s", toString(adminEvent));

        log.infof(" : %s", adminEvent.getRepresentation());

        if (adminEvent.getResourceType().name().equals("USER_PROFILE") &&
                adminEvent.getOperationType().name().equals("UPDATE")) {

            RealmModel realm = this.model.getRealm(adminEvent.getRealmId());

            // 获取所有语言的覆盖内容
            Map<String, Map<String, String>> allOverrides = getAllRealmLocalizationOverrides(realm);

            // 打印结果
            allOverrides.forEach((locale, overrides) -> {
                log.infof("Locale: %s", locale);
                overrides.forEach((key, value) -> log.infof("  Key: %s, Value: %s", key, value));
            });

            JsonArray replacedDisplayName = new JsonArray();
            try {
                // 获取representation中的配置信息
                String representation = adminEvent.getRepresentation();

                // 解析JSON配置
                JsonObject config = new Gson().fromJson(representation, JsonObject.class);

                if (!config.has("attributes")) {
                    log.infof("No attributes found in the configuration");
                    return;
                }

                JsonArray attributes = config.getAsJsonArray("attributes");

                
                // 遍历所有属性
                for (JsonElement attributeElement : attributes) {
                    JsonObject attribute = attributeElement.getAsJsonObject();

                    // 获取displayName属性
                    if (!attribute.has("displayName")) {
                        continue;
                    }
                    String displayName = attribute.get("displayName").getAsString();
                    // 检查displayName是否是变量格式 (以${开头})
                    if (displayName.startsWith("${") && displayName.endsWith("}")) {
                        // 提取变量名
                        String key = displayName.substring(2, displayName.length() - 1);

                        // 获取中文翻译 (使用zh或zh-CN locale)
                        String chineseTranslation = null;
                        Map<String, String> zhOverrides = allOverrides.get("zh");
                        if (zhOverrides != null) {
                            chineseTranslation = zhOverrides.get(key);
                        }
                        if (chineseTranslation == null) {
                            Map<String, String> zhCNOverrides = allOverrides.get("zh-CN");
                            if (zhCNOverrides != null) {
                                chineseTranslation = zhCNOverrides.get(key);
                            }
                        }

                        // 如果找到中文翻译，则更新displayName
                        if (chineseTranslation != null) {
                            attribute.addProperty("displayName", chineseTranslation);
                            log.infof("Updated displayName for attribute %s: %s",
                                    attribute.get("name").getAsString(),
                                    chineseTranslation);
                        }
                        replacedDisplayName.add(attribute);
                    }

                    if (attribute.get("name").getAsString().equals("email")) {
                        attribute.addProperty("displayName", "邮箱");
                    }

                    if (attribute.get("name").getAsString().equals("firstName")) {
                        attribute.addProperty("displayName", "名字");
                    }   

                    if (attribute.get("name").getAsString().equals("lastName")) {
                        attribute.addProperty("displayName", "姓氏");
                    }   

                    if (attribute.get("name").getAsString().equals("username")) {
                        attribute.addProperty("displayName", "用户名");
                    }

                    log.infof("Updated User Profile attributes: %s", replacedDisplayName);
                }

            } catch (Exception e) {
                log.errorf("Error getting user profile configuration: %s", e.getMessage());
            }
        }

    }

    private void sendUserData(UserModel user, String eventName) {
        String data = """
                {
                    "id": "%s",
                    "email": "%s",
                    "userName": "%s",
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """.formatted(user.getId(), user.getEmail(), user.getUsername(), user.getFirstName(),
                user.getLastName());
        log.debug("data =" + data);

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String userJson = gson.toJson(user.getAttributes());
        log.debug("user =" + userJson);
        log.debugf("userAttributes = %s", user.getAttributes());
        try {
            Client.sendUserData(userJson, eventName);
            log.debug("A new user has been created and post API");
        } catch (Exception e) {
            log.errorf("Failed to call API: %s", e);
        }
    }

    @Override
    public void close() {
    }

    private String toString(Event event) {
        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("type=" + event.getType())
                .add("realmId=" + event.getRealmId())
                .add("clientId=" + event.getClientId())
                .add("userId=" + event.getUserId())
                .add("ipAddress=" + event.getIpAddress());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        if (event.getDetails() != null) {
            event.getDetails().forEach((key, value) -> {
                if (value == null || !value.contains(" ")) {
                    joiner.add(key + "=" + value);
                } else {
                    joiner.add(key + "='" + value + "'");
                }
            });
        }

        return joiner.toString();
    }

    private String toString(AdminEvent event) {
        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getAuthDetails().getUserId());

        StringJoiner joiner = new StringJoiner(", ");

        joiner.add("operationType=" + event.getOperationType())
                .add("realmId=" + event.getAuthDetails().getRealmId())
                .add("clientId=" + event.getAuthDetails().getClientId())
                .add("userId=" + event.getAuthDetails().getUserId());

        if (newRegisteredUser != null) {
            joiner.add("email=" + newRegisteredUser.getEmail())
                    .add("username=" + newRegisteredUser.getUsername())
                    .add("firstName=" + newRegisteredUser.getFirstName())
                    .add("lastName=" + newRegisteredUser.getLastName());
        }

        joiner.add("ipAddress=" + event.getAuthDetails().getIpAddress())
                .add("resourcePath=" + event.getResourcePath());

        if (event.getError() != null) {
            joiner.add("error=" + event.getError());
        }

        return joiner.toString();
    }

    private Map<String, String> getRealmLocalizationOverrides(RealmModel realm, String locale) {
        Map<String, String> overrides = new HashMap<>();
        try {
            // 直接使用locale字符串
            Map<String, String> translations = realm.getRealmLocalizationTextsByLocale(locale);

            if (translations != null && !translations.isEmpty()) {
                log.infof("Found %d overrides for locale %s", translations.size(), locale);
                translations.forEach((key, value) -> {
                    log.infof("Override - Key: %s, Value: %s", key, value);
                    overrides.put(key, value);
                });
            } else {
                log.infof("No overrides found for locale %s", locale);
            }

        } catch (Exception e) {
            log.errorf("Error getting realm localization overrides for locale %s: %s",
                    locale, e.getMessage());
        }
        return overrides;
    }

    // 获取所有语言的覆盖内容
    private Map<String, Map<String, String>> getAllRealmLocalizationOverrides(RealmModel realm) {
        Map<String, Map<String, String>> allOverrides = new HashMap<>();

        try {
            // 获取所有支持的语言
            Set<String> supportedLocales = realm.getSupportedLocalesStream().collect(Collectors.toSet());
            log.infof("Checking overrides for locales: %s", supportedLocales);

            // 对每种语言获取其覆盖内容
            for (String locale : supportedLocales) {
                Map<String, String> localeOverrides = getRealmLocalizationOverrides(realm, locale);
                if (!localeOverrides.isEmpty()) {
                    allOverrides.put(locale, localeOverrides);
                }
            }

        } catch (Exception e) {
            log.errorf("Error getting all realm localization overrides: %s", e.getMessage());
        }

        return allOverrides;
    }

}

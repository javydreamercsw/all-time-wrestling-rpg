package com.github.javydreamercsw.management.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for retrieving and parsing Notion page content using the Blocks API. This handles
 * the extraction of rich text content from Notion pages.
 */
@Slf4j
public class NotionBlocksRetriever {

  private static final String NOTION_API_BASE = "https://api.notion.com/v1";
  private static final String NOTION_VERSION = "2022-06-28";
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String notionToken;

  public NotionBlocksRetriever(@NonNull String notionToken) {
    this.notionToken = notionToken;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  /** Retrieve the page content for a given page ID. */
  public String retrievePageContent(String pageId) {
    try {
      log.debug("Retrieving page content for ID: {}", pageId);

      // Get all blocks for the page
      List<JsonNode> blocks = getAllBlocks(pageId);

      if (blocks.isEmpty()) {
        log.warn("No blocks found for page: {}", pageId);
        return null;
      }

      // Parse blocks into readable content
      StringBuilder content = new StringBuilder();
      for (JsonNode block : blocks) {
        String blockContent = parseBlock(block);
        if (blockContent != null && !blockContent.trim().isEmpty()) {
          content.append(blockContent).append("\n");
        }
      }

      String result = content.toString().trim();
      log.debug(
          "Successfully retrieved {} characters of content for page: {}", result.length(), pageId);
      return result;

    } catch (Exception e) {
      log.error("Failed to retrieve page content for ID: {}", pageId, e);
      return null;
    }
  }

  /** Get all blocks for a page, handling pagination. */
  private List<JsonNode> getAllBlocks(@NonNull String pageId)
      throws IOException, InterruptedException {
    List<JsonNode> allBlocks = new ArrayList<>();
    String nextCursor = null;

    do {
      String url = NOTION_API_BASE + "/blocks/" + pageId + "/children";
      if (nextCursor != null) {
        url += "?start_cursor=" + nextCursor;
      }

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Bearer " + notionToken)
              .header("Notion-Version", NOTION_VERSION)
              .header("Content-Type", "application/json")
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.error(
            "Failed to retrieve blocks. Status: {}, Body: {}",
            response.statusCode(),
            response.body());
        break;
      }

      JsonNode responseJson = objectMapper.readTree(response.body());
      JsonNode results = responseJson.get("results");

      if (results != null && results.isArray()) {
        for (JsonNode block : results) {
          allBlocks.add(block);
        }
      }

      // Check for pagination
      JsonNode hasMore = responseJson.get("has_more");
      if (hasMore != null && hasMore.asBoolean()) {
        JsonNode cursor = responseJson.get("next_cursor");
        nextCursor = cursor != null ? cursor.asText() : null;
      } else {
        nextCursor = null;
      }

    } while (nextCursor != null);

    log.debug("Retrieved {} blocks for page: {}", allBlocks.size(), pageId);
    return allBlocks;
  }

  /** Parse a single block into readable text content. */
  private String parseBlock(JsonNode block) {
    if (block == null) return null;

    JsonNode typeNode = block.get("type");
    if (typeNode == null) return null;

    String blockType = typeNode.asText();

    try {
      return switch (blockType) {
        case "paragraph" -> parseParagraph(block);
        case "heading_1" -> parseHeading(block, 1);
        case "heading_2" -> parseHeading(block, 2);
        case "heading_3" -> parseHeading(block, 3);
        case "bulleted_list_item" -> parseBulletedListItem(block);
        case "numbered_list_item" -> parseNumberedListItem(block);
        case "to_do" -> parseToDoItem(block);
        case "quote" -> parseQuote(block);
        case "callout" -> parseCallout(block);
        case "divider" -> "---";
        default -> {
          log.debug("Unhandled block type: {}", blockType);
          yield null;
        }
      };
    } catch (Exception e) {
      log.warn("Failed to parse block of type {}: {}", blockType, e.getMessage());
      return null;
    }
  }

  /** Parse a paragraph block. */
  private String parseParagraph(JsonNode block) {
    JsonNode paragraph = block.get("paragraph");
    if (paragraph == null) return null;

    JsonNode richText = paragraph.get("rich_text");
    return parseRichText(richText);
  }

  /** Parse a heading block. */
  private String parseHeading(JsonNode block, int level) {
    String headingKey = "heading_" + level;
    JsonNode heading = block.get(headingKey);
    if (heading == null) return null;

    JsonNode richText = heading.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;

    // Add markdown-style heading markers
    String prefix = "#".repeat(level) + " ";
    return prefix + text;
  }

  /** Parse a bulleted list item. */
  private String parseBulletedListItem(JsonNode block) {
    JsonNode listItem = block.get("bulleted_list_item");
    if (listItem == null) return null;

    JsonNode richText = listItem.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;
    return "• " + text;
  }

  /** Parse a numbered list item. */
  private String parseNumberedListItem(JsonNode block) {
    JsonNode listItem = block.get("numbered_list_item");
    if (listItem == null) return null;

    JsonNode richText = listItem.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;
    return "1. " + text;
  }

  /** Parse a to-do item. */
  private String parseToDoItem(JsonNode block) {
    JsonNode toDo = block.get("to_do");
    if (toDo == null) return null;

    JsonNode richText = toDo.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;

    JsonNode checked = toDo.get("checked");
    boolean isChecked = checked != null && checked.asBoolean();

    return (isChecked ? "☑ " : "☐ ") + text;
  }

  /** Parse a quote block. */
  private String parseQuote(JsonNode block) {
    JsonNode quote = block.get("quote");
    if (quote == null) return null;

    JsonNode richText = quote.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;
    return "> " + text;
  }

  /** Parse a callout block. */
  private String parseCallout(JsonNode block) {
    JsonNode callout = block.get("callout");
    if (callout == null) return null;

    JsonNode richText = callout.get("rich_text");
    String text = parseRichText(richText);

    if (text == null || text.trim().isEmpty()) return null;

    // Get the icon if available
    JsonNode icon = callout.get("icon");
    String iconText = "";
    if (icon != null) {
      JsonNode emoji = icon.get("emoji");
      if (emoji != null) {
        iconText = emoji.asText() + " ";
      }
    }

    return iconText + text;
  }

  /** Parse rich text array into plain text with basic formatting. */
  private String parseRichText(JsonNode richTextArray) {
    if (richTextArray == null || !richTextArray.isArray()) return null;

    StringBuilder text = new StringBuilder();

    for (JsonNode richText : richTextArray) {
      JsonNode plainText = richText.get("plain_text");
      if (plainText != null) {
        String textContent = plainText.asText();

        // Apply basic formatting based on annotations
        JsonNode annotations = richText.get("annotations");
        if (annotations != null) {
          if (annotations.get("bold") != null && annotations.get("bold").asBoolean()) {
            textContent = "**" + textContent + "**";
          }
          if (annotations.get("italic") != null && annotations.get("italic").asBoolean()) {
            textContent = "*" + textContent + "*";
          }
          if (annotations.get("code") != null && annotations.get("code").asBoolean()) {
            textContent = "`" + textContent + "`";
          }
        }

        text.append(textContent);
      }
    }

    return text.toString();
  }
}
